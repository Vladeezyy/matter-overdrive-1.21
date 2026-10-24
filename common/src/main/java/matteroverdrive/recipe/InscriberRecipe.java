package matteroverdrive.recipe;

import com.google.gson.JsonObject;

import matteroverdrive.init.MORecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 InscriberRecipe: one main and one secondary item become the result, costing {@code energy} FE over
 * {@code time} ticks. Defined in {@code data/<ns>/recipes/*.json} with type {@code matteroverdrive:inscriber}.
 */
public record InscriberRecipe(ResourceLocation id, Ingredient main, Ingredient secondary, ItemStack result, int energy, int time)
        implements Recipe<InscriberRecipe.Input> {

    public static final class Input extends SimpleContainer {
        public Input(ItemStack main, ItemStack secondary) {
            super(main, secondary);
        }

        public ItemStack main() {
            return getItem(0);
        }

        public ItemStack secondary() {
            return getItem(1);
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return main.test(input.main()) && secondary.test(input.secondary());
    }

    @Override
    public ItemStack assemble(Input input, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<InscriberRecipe> getSerializer() {
        return MORecipes.INSCRIBER_SERIALIZER.get();
    }

    @Override
    public RecipeType<InscriberRecipe> getType() {
        return MORecipes.INSCRIBER_TYPE.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result;
    }

    @Override
    public net.minecraft.core.NonNullList<Ingredient> getIngredients() {
        return net.minecraft.core.NonNullList.of(Ingredient.EMPTY, main, secondary);
    }

    public static final class Serializer implements RecipeSerializer<InscriberRecipe> {
        @Override
        public InscriberRecipe fromJson(ResourceLocation id, JsonObject json) {
            int energy = GsonHelper.getAsInt(json, "energy"), time = GsonHelper.getAsInt(json, "time");
            if (energy <= 0 || time <= 0) throw new com.google.gson.JsonSyntaxException("energy and time must be positive");
            return new InscriberRecipe(id, Ingredient.fromJson(json.get("main"), false), Ingredient.fromJson(json.get("secondary"), false),
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")), energy, time);
        }

        @Override
        public InscriberRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new InscriberRecipe(id, Ingredient.fromNetwork(buf), Ingredient.fromNetwork(buf), buf.readItem(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InscriberRecipe recipe) {
            recipe.main.toNetwork(buf);
            recipe.secondary.toNetwork(buf);
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.energy);
            buf.writeVarInt(recipe.time);
        }
    }
}
