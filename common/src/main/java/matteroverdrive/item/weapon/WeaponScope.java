package matteroverdrive.item.weapon;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 IWeaponScope: a sights module that sets the zoom and changes accuracy. */
public interface WeaponScope extends WeaponModule {
    float getZoomAmount(ItemStack scope, ItemStack weapon);

    float getAccuracyModify(ItemStack scope, ItemStack weapon, boolean zoomed, float accuracy);
}
