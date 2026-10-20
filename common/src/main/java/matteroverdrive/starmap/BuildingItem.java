package matteroverdrive.starmap;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 IBuilding: a building of a planet (the first two construction slots take these). */
public interface BuildingItem extends Buildable {
    BuildingType getType(ItemStack stack);
}
