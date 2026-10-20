package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.recipe.EnergyPackRecipe;
import matteroverdrive.recipe.InscriberRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

public final class MORecipes {
    public static final Registrar<RecipeType<?>> TYPES = Registrar.create(Registries.RECIPE_TYPE, MatterOverdrive.MODID);
    public static final Registrar<RecipeSerializer<?>> SERIALIZERS = Registrar.create(Registries.RECIPE_SERIALIZER, MatterOverdrive.MODID);

    public static final RegistryEntry<RecipeType<?>, RecipeType<InscriberRecipe>> INSCRIBER_TYPE = TYPES.register("inscriber",
            () -> new RecipeType<InscriberRecipe>() {
                @Override
                public String toString() {
                    return MatterOverdrive.MODID + ":inscriber";
                }
            });
    public static final RegistryEntry<RecipeSerializer<?>, InscriberRecipe.Serializer> INSCRIBER_SERIALIZER =
            SERIALIZERS.register("inscriber", InscriberRecipe.Serializer::new);

    public static final RegistryEntry<RecipeSerializer<?>, net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<EnergyPackRecipe>> ENERGY_PACK_SERIALIZER =
            SERIALIZERS.register("energy_pack", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(EnergyPackRecipe::new));

    private MORecipes() {}
}
