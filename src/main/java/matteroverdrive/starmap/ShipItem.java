package matteroverdrive.starmap;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 IShip: a ship of a planet's fleet (the last two construction slots take these). */
public interface ShipItem extends Buildable {
    ShipType getType(ItemStack stack);

    /** On arrival at a planet. */
    void onTravel(ItemStack stack, Planet to);
}
