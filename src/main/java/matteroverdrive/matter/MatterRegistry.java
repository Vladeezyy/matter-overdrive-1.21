package matteroverdrive.matter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * How much matter an item holds (1.7.10 MatterRegistry). Base values come from the {@code matteroverdrive:matter}
 * item data map; every other item is derived like 1.7.10 did: up to 8 passes over the crafting recipes (sum of the
 * ingredients, cheapest alternative per slot, divided by the output count, minus returned containers), then smelting
 * results take their input's matter.
 */
public final class MatterRegistry {
    public static final DataMapType<Item, Integer> MATTER = DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "matter"), Registries.ITEM, ExtraCodecs.POSITIVE_INT)
            .synced(Codec.INT, false)
            .build();

    private static final int PASSES = 8;

    /** Values on the logical server (recomputed after every data load) and the copy the client received. */
    private static @Nullable Map<Item, Integer> serverValues;
    private static Map<Item, Integer> clientValues = Map.of();

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

    public static void setClientValues(Map<Item, Integer> values) {
        clientValues = Map.copyOf(values);
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
        MatterOverdrive.LOGGER.info("Matter registry: {} base and {} calculated entries in {} ms", baseCount,
                values.size() - baseCount, (System.nanoTime() - start) / 1_000_000);
        return values;
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
