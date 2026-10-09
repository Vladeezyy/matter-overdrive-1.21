package matteroverdrive.matter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * How much matter an item holds (1.7.10 MatterRegistry). Base values come from the {@code matteroverdrive:matter}
 * item data map; every other item is derived like 1.7.10 did: up to 8 passes over the crafting recipes (sum of the
 * ingredients, cheapest alternative per slot, divided by the output count, minus returned containers), then smelting
 * results take their input's matter.
 * <p>
 * Beyond 1.7.10 (items it never had): every other recipe type, mods' machines included (ingredients from the recipe's
 * placement info, the result from its display), copper weathering / waxing and log stripping (same matter as the
 * source block; also concrete from its powder, infested blocks from their host). Whatever is still left gets an estimate from its block hardness, rarity and stack size (shown as "~"),
 * except the {@code matteroverdrive:matter_blacklist} tag, unbreakable blocks and spawn eggs.
 */
public final class MatterRegistry {
    public static final DataMapType<Item, Integer> MATTER = DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "matter"), Registries.ITEM, ExtraCodecs.POSITIVE_INT)
            .synced(Codec.INT, false)
            .build();

    private static final int PASSES = 8;
    /** Items that never get an estimated value (creative / technical blocks; modpacks can add to it). */
    public static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "matter_blacklist"));

    /** Values on the logical server (recomputed after every data load) and the copy the client received. */
    private static @Nullable Map<Item, Integer> serverValues;
    private static Map<Item, Integer> clientValues = Map.of();
    /** Items whose value is an estimate, not derived from base values or recipes. */
    private static Set<Item> serverEstimated = Set.of();
    private static Set<Item> clientEstimated = Set.of();

    private MatterRegistry() {}

    /** Matter of one item of this type on the server, 0 if it can't be decomposed or replicated. */
    public static int get(MinecraftServer server, Item item) {
        return values(server).getOrDefault(item, 0);
    }

    public static Map<Item, Integer> values(MinecraftServer server) {
        if (serverValues == null) {
            serverValues = calculate(server);
        }
        return serverValues;
    }

    /** Called when datapacks (recipes, data maps) are (re)loaded. */
    public static void invalidate() {
        serverValues = null;
    }

    /**
     * For code that runs on both sides (slot filters): the server table when this JVM has one, else the client copy.
     * Both hold the same values once the client has synced.
     */
    public static int getAnySide(Item item) {
        Map<Item, Integer> values = serverValues != null ? serverValues : clientValues;
        return values.getOrDefault(item, 0);
    }

    public static int getClient(Item item) {
        return clientValues.getOrDefault(item, 0);
    }

    public static void setClientValues(Map<Item, Integer> values, Set<Item> estimated) {
        clientValues = Map.copyOf(values);
        clientEstimated = Set.copyOf(estimated);
    }

    public static boolean isEstimatedClient(Item item) {
        return clientEstimated.contains(item);
    }

    /** The estimated items of the current server table (call after {@link #values}). */
    public static Set<Item> estimated(MinecraftServer server) {
        values(server);
        return serverEstimated;
    }

    // --- calculation -----------------------------------------------------------------------------

    private static Map<Item, Integer> calculate(MinecraftServer server) {
        long start = System.nanoTime();
        Map<Item, Integer> values = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Integer base = BuiltInRegistries.ITEM.getData(MATTER, BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
            if (base != null && base > 0) values.put(item, base);
        }
        int baseCount = values.size();

        var recipes = server.getRecipeManager().getRecipes();
        for (int pass = 0; pass < PASSES; pass++) {
            int before = values.size();
            for (RecipeHolder<?> holder : recipes) {
                if (holder.value() instanceof CraftingRecipe recipe && !recipe.isSpecial()) {
                    deriveFromCrafting(recipe, server, values);
                }
            }
            if (values.size() == before) break;
        }
        for (RecipeHolder<?> holder : recipes) {
            if (holder.value() instanceof AbstractCookingRecipe recipe) {
                deriveFromCooking(recipe, server, values);
            }
        }
        int classic = values.size();
        // beyond 1.7.10: every recipe type and the block conversions, until nothing changes
        var context = SlotDisplayContext.fromLevel(server.overworld());
        for (int pass = 0; pass < 16; pass++) {
            int before = values.size();
            for (RecipeHolder<?> holder : recipes) {
                Recipe<?> recipe = holder.value();
                if (recipe instanceof CraftingRecipe crafting && crafting.isSpecial()) continue;
                deriveFromAnyRecipe(recipe, context, values);
            }
            deriveFromBlockConversions(values);
            if (values.size() == before) break;
        }
        int derived = values.size();
        Set<Item> estimated = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (values.containsKey(item) || !canEstimate(item)) continue;
            values.put(item, estimate(item));
            estimated.add(item);
        }
        serverEstimated = Set.copyOf(estimated);
        MatterOverdrive.LOGGER.info("Matter registry: {} base, {} from crafting / smelting, {} from other recipes and blocks, {} estimated in {} ms",
                baseCount, classic - baseCount, derived - classic, estimated.size(), (System.nanoTime() - start) / 1_000_000);
        return values;
    }

    /** Any recipe: the sum of its ingredients' cheapest matter, split over the result count. */
    private static void deriveFromAnyRecipe(Recipe<?> recipe, net.minecraft.util.context.ContextMap context, Map<Item, Integer> values) {
        List<Ingredient> ingredients;
        List<RecipeDisplay> displays;
        try {
            var placement = recipe.placementInfo();
            if (placement.isImpossibleToPlace()) return;
            ingredients = placement.ingredients();
            displays = recipe.display();
        } catch (RuntimeException e) {
            return;     // a mod recipe that can't describe itself
        }
        if (ingredients.isEmpty() || displays.isEmpty()) return;
        int total = 0;
        for (Ingredient ingredient : ingredients) {
            int cheapest = cheapest(ingredient, values);
            if (cheapest <= 0) return;
            total += cheapest;
        }
        for (RecipeDisplay display : displays) {
            ItemStack output;
            try {
                output = display.result().resolveForFirstStack(context);
            } catch (RuntimeException e) {
                continue;
            }
            if (output.isEmpty() || values.containsKey(output.getItem())) continue;
            values.put(output.getItem(), Math.max(1, (int) Math.round((double) total / output.getCount())));
        }
    }

    /** Weathered / waxed copper and stripped logs hold the matter of the block they came from. */
    private static void deriveFromBlockConversions(Map<Item, Integer> values) {
        for (Block block : BuiltInRegistries.BLOCK) {
            Integer value = values.get(block.asItem());
            if (value == null || value <= 0) continue;
            var holder = block.builtInRegistryHolder();
            var next = holder.getData(NeoForgeDataMaps.OXIDIZABLES);
            if (next != null) values.putIfAbsent(next.nextOxidationStage().asItem(), value);
            var waxed = holder.getData(NeoForgeDataMaps.WAXABLES);
            if (waxed != null) values.putIfAbsent(waxed.waxed().asItem(), value);
            var stripped = holder.getData(NeoForgeDataMaps.STRIPPABLES);
            if (stripped != null) values.putIfAbsent(stripped.strippedBlock().asItem(), value);
            // concrete powder hardens in water (its target is private: the "_concrete" sibling by name)
            if (block instanceof net.minecraft.world.level.block.ConcretePowderBlock) {
                var id = BuiltInRegistries.BLOCK.getKey(block);
                BuiltInRegistries.BLOCK.getOptional(id.withPath(id.getPath().replace("_concrete_powder", "_concrete")))
                        .ifPresent(concrete -> values.putIfAbsent(concrete.asItem(), value));
            }
        }
        // infested blocks hold their host's matter
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof net.minecraft.world.level.block.InfestedBlock infested) {
                Integer host = values.get(infested.getHostBlock().asItem());
                if (host != null) values.putIfAbsent(block.asItem(), host);
            }
        }
        values.remove(Items.AIR);
    }

    private static boolean canEstimate(Item item) {
        if (item == Items.AIR || item.builtInRegistryHolder().is(BLACKLIST) || item instanceof SpawnEggItem) return false;
        return !(item instanceof BlockItem block) || block.getBlock().defaultDestroyTime() >= 0;
    }

    /** A rough value: block hardness (or 8 for items), x4 per rarity step, x4 for unstackables, x2 for stacks of 16. */
    static int estimate(Item item) {
        ItemStack stack = item.getDefaultInstance();
        int base = item instanceof BlockItem block ? Math.max(1, Math.round(1 + block.getBlock().defaultDestroyTime() * 2)) : 8;
        Rarity rarity = stack.getRarity();
        int multiplier = switch (rarity) {
            case UNCOMMON -> 4;
            case RARE -> 16;
            case EPIC -> 64;
            default -> 1;
        };
        int stackSize = stack.getMaxStackSize();
        if (stackSize == 1) multiplier *= 4;
        else if (stackSize <= 16) multiplier *= 2;
        return (int) Math.min(4096, (long) base * multiplier);
    }

    private static void deriveFromCrafting(CraftingRecipe recipe, MinecraftServer server, Map<Item, Integer> values) {
        ItemStack output = result(recipe, server, CraftingInput.EMPTY);
        if (output.isEmpty() || values.containsKey(output.getItem())) return;
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        if (ingredients.isEmpty()) return;
        int total = 0;
        for (Ingredient ingredient : ingredients) {
            int cheapest = cheapest(ingredient, values);
            if (cheapest <= 0) return; // an ingredient without matter: the result can't be replicated
            total += cheapest;
        }
        int matter = (int) Math.round((double) total / output.getCount());
        if (matter > 0) values.put(output.getItem(), matter);
    }

    /** The cheapest valued alternative of an ingredient, minus what crafting gives back (water bucket -> bucket). */
    private static int cheapest(Ingredient ingredient, Map<Item, Integer> values) {
        int best = 0;
        for (Holder<Item> holder : ingredient.items().toList()) {
            Integer value = values.get(holder.value());
            if (value == null || value <= 0) continue;
            ItemStack remainder = holder.value().getCraftingRemainder(holder.value().getDefaultInstance());
            // wait for the returned container's value (recipe order varies between versions: the cake before the bucket)
            if (!remainder.isEmpty() && !values.containsKey(remainder.getItem())) continue;
            int net = value - (remainder.isEmpty() ? 0 : values.getOrDefault(remainder.getItem(), 0) * remainder.getCount());
            if (net > 0 && (best == 0 || net < best)) best = net;
        }
        return best;
    }

    private static void deriveFromCooking(AbstractCookingRecipe recipe, MinecraftServer server, Map<Item, Integer> values) {
        ItemStack output = result(recipe, server, new SingleRecipeInput(ItemStack.EMPTY));
        if (output.isEmpty() || values.containsKey(output.getItem())) return;
        int input = cheapest(recipe.input(), values);
        if (input > 0) values.put(output.getItem(), Math.max(1, input / output.getCount()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack result(Recipe recipe, MinecraftServer server, Object input) {
        try {
            return recipe.assemble((net.minecraft.world.item.crafting.RecipeInput) input, server.registryAccess());
        } catch (RuntimeException e) {
            return ItemStack.EMPTY; // special recipes that need a real input
        }
    }
}
