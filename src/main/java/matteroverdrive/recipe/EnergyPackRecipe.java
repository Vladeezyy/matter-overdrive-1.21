package matteroverdrive.recipe;

import matteroverdrive.init.MOItems;
import matteroverdrive.init.MORecipes;
import matteroverdrive.item.BatteryItem;
import matteroverdrive.item.weapon.EnergyPackItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 EnergyPackRecipe: a tritanium plate, a charged battery and gunpowder make one energy pack per 32000 FE
 * stored in the battery (a full battery gives 16).
 */
public class EnergyPackRecipe extends CustomRecipe {
    public EnergyPackRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static ItemStack battery(CraftingInput input) {
        ItemStack battery = ItemStack.EMPTY;
        int plates = 0, powder = 0, others = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(MOItems.TRITANIUM_PLATE.get())) plates++;
            else if (s.is(Items.GUNPOWDER)) powder++;
            else if (s.getItem() instanceof BatteryItem b && !b.isCreative() && battery.isEmpty()) battery = s;
            else others++;
        }
        return plates == 1 && powder == 1 && others == 0 ? battery : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack battery = battery(input);
        return !battery.isEmpty() && ((BatteryItem) battery.getItem()).getEnergy(battery) >= EnergyPackItem.ENERGY;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack battery = battery(input);
        if (battery.isEmpty()) return ItemStack.EMPTY;
        int packs = ((BatteryItem) battery.getItem()).getEnergy(battery) / EnergyPackItem.ENERGY;
        return packs > 0 ? new ItemStack(MOItems.ENERGY_PACK.get(), Math.min(64, packs)) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<EnergyPackRecipe> getSerializer() {
        return MORecipes.ENERGY_PACK_SERIALIZER.get();
    }
}
