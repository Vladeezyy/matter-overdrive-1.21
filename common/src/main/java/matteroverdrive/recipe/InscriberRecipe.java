package matteroverdrive.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import matteroverdrive.init.MORecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 InscriberRecipe: one main and one secondary item become the result, costing {@code energy} FE over
 * {@code time} ticks. Defined in {@code data/<ns>/recipe/*.json} with type {@code matteroverdrive:inscriber}.
 */
public record InscriberRecipe(Ingredient main, Ingredient secondary, ItemStackTemplate output, int energy, int time)
        implements Recipe<InscriberRecipe.Input> {

    public record Input(ItemStack main, ItemStack secondary) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? main : secondary;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return main.test(input.main()) && secondary.test(input.secondary());
    }

    /** A new stack of the result (26.x: recipes hold templates, stacks need bound components). */
    public ItemStack result() {
        return output.create();
    }

    @Override
    public ItemStack assemble(Input input) {
        return output.create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean isSpecial() {
        return true;
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
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    private static final MapCodec<InscriberRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("main").forGetter(InscriberRecipe::main),
            Ingredient.CODEC.fieldOf("secondary").forGetter(InscriberRecipe::secondary),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(InscriberRecipe::output),
            ExtraCodecs.POSITIVE_INT.fieldOf("energy").forGetter(InscriberRecipe::energy),
            ExtraCodecs.POSITIVE_INT.fieldOf("time").forGetter(InscriberRecipe::time)
    ).apply(i, InscriberRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, InscriberRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, InscriberRecipe::main,
            Ingredient.CONTENTS_STREAM_CODEC, InscriberRecipe::secondary,
            ItemStackTemplate.STREAM_CODEC, InscriberRecipe::output,
            ByteBufCodecs.VAR_INT, InscriberRecipe::energy,
            ByteBufCodecs.VAR_INT, InscriberRecipe::time,
            InscriberRecipe::new);

    public static final RecipeSerializer<InscriberRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
