package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.recipe.EnergyPackRecipe;
import matteroverdrive.recipe.InscriberRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MORecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MatterOverdrive.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MatterOverdrive.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<InscriberRecipe>> INSCRIBER_TYPE = TYPES.register("inscriber",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "inscriber")));
    public static final DeferredHolder<RecipeSerializer<?>, InscriberRecipe.Serializer> INSCRIBER_SERIALIZER =
            SERIALIZERS.register("inscriber", InscriberRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<EnergyPackRecipe>> ENERGY_PACK_SERIALIZER =
            SERIALIZERS.register("energy_pack", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(EnergyPackRecipe::new));

    private MORecipes() {}
}
