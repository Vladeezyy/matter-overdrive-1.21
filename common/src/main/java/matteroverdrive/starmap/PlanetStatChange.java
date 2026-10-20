package matteroverdrive.starmap;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 IPlanetStatChange: a building item that changes its planet's stats. */
public interface PlanetStatChange {
    float changeStat(ItemStack stack, Planet planet, PlanetStatType statType, float original);
}
