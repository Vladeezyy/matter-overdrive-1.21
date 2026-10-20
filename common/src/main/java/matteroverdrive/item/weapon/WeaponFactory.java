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
 * gold "✪ Legendary" name and rolled stat multipliers.
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
            weapon.set(DataComponents.CUSTOM_NAME, legendaryName(weapon));
            weapon.set(matteroverdrive.init.MODataComponents.LEGENDARY_WEAPON.get(), Legendary.roll(random, level));
        }
        return weapon;
    }

    /** 1.7.10 Reference.UNICODE_LEGENDARY + gold "Legendary" + the item's name. */
    public static Component legendaryName(ItemStack stack) {
        return Component.literal("\u272a ").append(Component.translatable("rarity.matteroverdrive.legendary")).append(" ").append(stack.getItemName())
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GOLD));
    }

    /**
     * 1.7.10 modifyToLegendary, each level 0..level: damage x(1 + 0.1n), accuracy (spread) x(1 - 0.1n), cooldown x(1 - 0.05n),
     * range x(1 + 0.15n) - 1.7.10 read the range multiplier as an int, so it never changed the range.
     */
    public record Legendary(float damage, float accuracy, float speed, float range) {
        public static final com.mojang.serialization.Codec<Legendary> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.FLOAT.fieldOf("damage").forGetter(Legendary::damage),
                com.mojang.serialization.Codec.FLOAT.fieldOf("accuracy").forGetter(Legendary::accuracy),
                com.mojang.serialization.Codec.FLOAT.fieldOf("speed").forGetter(Legendary::speed),
                com.mojang.serialization.Codec.FLOAT.fieldOf("range").forGetter(Legendary::range)).apply(i, Legendary::new));
        public static final Legendary NONE = new Legendary(1, 1, 1, 1);

        public static Legendary roll(RandomSource random, int level) {
            return new Legendary(1 + 0.1f * random.nextInt(level + 1), 1 - 0.1f * random.nextInt(level + 1),
                    1 - 0.05f * random.nextInt(level + 1), 1 + 0.15f * random.nextInt(level + 1));
        }
    }

    public static Legendary legendary(ItemStack weapon) {
        return weapon.getOrDefault(matteroverdrive.init.MODataComponents.LEGENDARY_WEAPON.get(), Legendary.NONE);
    }

    private WeaponFactory() {}
}
