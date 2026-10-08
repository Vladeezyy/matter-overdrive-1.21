package matteroverdrive.item.weapon;

import matteroverdrive.init.MOItems;
import matteroverdrive.item.BatteryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 WeaponFactory.getRandomDecoratedEnergyWeapon: phaser rifle 70, omni tool 30, plasma shotgun 10, ion sniper 5; barrel none 200 / damage 100 (level 1+) / fire 10 (level 1+) / explosion 5 (level 2+);
 * battery 100 / hc battery 20 (level 1+); sniper scope 10 of 310 (level 1+). Fully charged. Legendary weapons get the
 * gold "Legendary" name (their 1.7.10 stat bonuses aren't ported).
 */
public final class WeaponFactory {
    public static ItemStack randomDecorated(RandomSource random, int level, boolean legendary) {
        int roll = random.nextInt(115);
        EnergyWeaponItem item = roll < 70 ? MOItems.PHASER_RIFLE.get() : roll < 100 ? MOItems.OMNI_TOOL.get()
                : roll < 110 ? MOItems.PLASMA_SHOTGUN.get() : MOItems.ION_SNIPER.get();
        ItemStack weapon = new ItemStack(item);
        int barrel = random.nextInt(200 + (level >= 1 ? 110 : 0) + (level >= 2 ? 5 : 0));
        ItemStack module = barrel < 200 ? ItemStack.EMPTY : barrel < 300 ? new ItemStack(MOItems.BARREL_DAMAGE.get())
                : barrel < 310 ? new ItemStack(MOItems.BARREL_FIRE.get()) : new ItemStack(MOItems.BARREL_EXPLOSION.get());
        if (!module.isEmpty() && item.canInstall(WeaponModule.SLOT_BARREL, module)) EnergyWeaponItem.setModule(weapon, WeaponModule.SLOT_BARREL, module);
        if (level >= 1) {
            BatteryItem battery = random.nextInt(120) < 100 ? MOItems.BATTERY.get() : MOItems.HC_BATTERY.get();
            EnergyWeaponItem.setModule(weapon, WeaponModule.SLOT_BATTERY, battery.charged());
            if (random.nextInt(310) < 10) {
                ItemStack scope = new ItemStack(MOItems.SNIPER_SCOPE.get());
                if (item.canInstall(WeaponModule.SLOT_SIGHTS, scope)) EnergyWeaponItem.setModule(weapon, WeaponModule.SLOT_SIGHTS, scope);
            }
        }
        EnergyWeaponItem.setEnergy(weapon, EnergyWeaponItem.getCapacity(weapon));
        if (legendary) {
            weapon.set(DataComponents.CUSTOM_NAME, Component.translatable("rarity.matteroverdrive.legendary").withStyle(ChatFormatting.GOLD)
                    .append(" ").append(weapon.getItemName()).withStyle(s -> s.withItalic(false)));
        }
        return weapon;
    }

    private WeaponFactory() {}
}
