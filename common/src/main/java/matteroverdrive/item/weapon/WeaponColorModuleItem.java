package matteroverdrive.item.weapon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 WeaponModuleColor: tints a weapon's bolts. Ten colours, found in chests (no recipe in 1.7.10). */
public class WeaponColorModuleItem extends Item implements WeaponModule {
    /** 1.7.10 colors[]: red, green, blue, brown, pink, sky blue, gold, lime green, black, grey. */
    public static final String[] NAMES = {"red", "green", "blue", "brown", "pink", "sky_blue", "gold", "lime_green", "black", "grey"};
    public static final int[] COLORS = {0xCC0000, 0x009933, 0x0066FF, 0x663333, 0xFF99FF, 0x99CCFF, 0xD4AF37, 0x66FF66, 0x1E1E1E, 0x808080};

    private final int color;

    public WeaponColorModuleItem(int color, Properties properties) {
        super(properties.stacksTo(1));
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public int getSlot(ItemStack module) {
        return SLOT_COLOR;
    }

    @Override
    public float modifyStat(WeaponStat stat, ItemStack module, ItemStack weapon, float original) {
        return original;
    }
}
