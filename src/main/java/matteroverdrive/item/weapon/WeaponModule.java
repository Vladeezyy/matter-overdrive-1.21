package matteroverdrive.item.weapon;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 IWeaponModule: an item that fits a weapon slot and changes its stats. */
public interface WeaponModule {
    /** 1.7.10 Reference.MODULE_*: battery 0, colour 1, barrel 2, sights 3, other 4. */
    int SLOT_BATTERY = 0, SLOT_COLOR = 1, SLOT_BARREL = 2, SLOT_SIGHTS = 3, SLOT_OTHER = 4, SLOTS = 5;

    int getSlot(ItemStack module);

    float modifyStat(WeaponStat stat, ItemStack module, ItemStack weapon, float original);
}
