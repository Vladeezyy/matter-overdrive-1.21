package matteroverdrive.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Data Pad guide asks for an item's crafting recipe (1.7.10 GuideElementRecipe searched the recipe list). */
public final class GuideRecipePayload {
    public static Consumer<Grid> onGrid = g -> {};

    public record Request(Item item) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "guide_recipe_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Request::item, Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** Nine slots (row by row) of the item's options. */
    public record Grid(Item item, List<List<ItemStack>> slots) implements CustomPacketPayload {
        public static final Type<Grid> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "guide_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Grid> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Grid::item,
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.apply(ByteBufCodecs.list()), Grid::slots, Grid::new);

        @Override
        public Type<Grid> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.STREAM_CODEC, GuideRecipePayload::handleRequest)
                .playToClient(Grid.TYPE, Grid.STREAM_CODEC, (grid, context) -> onGrid.accept(grid));
    }

    private static void handleRequest(Request request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        List<List<ItemStack>> grid = find(player, request.item());
        if (grid != null) PacketDistributor.sendToPlayer(player, new Grid(request.item(), grid));
    }

    /** The first crafting recipe making the item, as a 3x3 grid of options per slot. */
    public static List<List<ItemStack>> find(ServerPlayer player, Item item) {
        var registries = player.level().registryAccess();
        for (var holder : player.level().getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe) || recipe.isSpecial()) continue;
            ItemStack result;
            try {
                result = recipe.getResultItem(registries);
            } catch (RuntimeException e) {
                continue;
            }
            if (result == null || !result.is(item)) continue;
            List<List<ItemStack>> grid = new ArrayList<>();
            for (int i = 0; i < 9; i++) grid.add(List.of());
            var ingredients = recipe.getIngredients();
            if (recipe instanceof ShapedRecipe shaped) {
                for (int i = 0; i < ingredients.size(); i++) {
                    int x = i % shaped.getWidth(), y = i / shaped.getWidth();
                    grid.set(x + y * 3, stacks(ingredients.get(i)));
                }
                return grid;
            }
            for (int i = 0; i < Math.min(9, ingredients.size()); i++) grid.set(i, stacks(ingredients.get(i)));
            return grid;
        }
        return null;
    }

    private static List<ItemStack> stacks(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return java.util.Arrays.stream(ingredient.getItems()).limit(16).toList();
    }

    private GuideRecipePayload() {}
}
