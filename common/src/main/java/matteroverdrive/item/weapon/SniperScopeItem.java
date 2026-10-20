package matteroverdrive.item.weapon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModuleSniperScope: zoom 0.85, accuracy x0.4 zoomed (+3 unzoomed), accuracy x0.8 and range x1.5. */
public class SniperScopeItem extends Item implements WeaponScope {
    public SniperScopeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public float getZoomAmount(ItemStack scope, ItemStack weapon) {
        return 0.85f;
    }

    @Override
    public float getAccuracyModify(ItemStack scope, ItemStack weapon, boolean zoomed, float accuracy) {
        return zoomed ? accuracy * 0.4f : accuracy + 3f;
    }

    @Override
    public int getSlot(ItemStack module) {
        return SLOT_SIGHTS;
    }

    @Override
    public float modifyStat(WeaponStat stat, ItemStack module, ItemStack weapon, float original) {
        return switch (stat) {
            case ACCURACY -> original * 0.8f;
            case RANGE -> original * 1.5f;
            default -> original;
        };
    }
}
