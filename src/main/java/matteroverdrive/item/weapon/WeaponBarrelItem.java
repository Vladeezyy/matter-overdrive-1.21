package matteroverdrive.item.weapon;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7.10 WeaponModuleBarrel: one item per barrel. Each multiplies or adds to a few weapon stats; explosion and
 * heal barrels only fit the phaser.
 */
public class WeaponBarrelItem extends Item implements WeaponModule {
    public enum Type {
        // stat -> {multiplier, addend}; 1.7.10 modifyWeaponStat cases 0..3
        DAMAGE(Map.of(WeaponStat.DAMAGE, new float[] {1.5f, 0}, WeaponStat.AMMO, new float[] {0.5f, 0}, WeaponStat.EFFECT, new float[] {0.5f, 0})),
        FIRE(Map.of(WeaponStat.AMMO, new float[] {0.5f, 0}, WeaponStat.DAMAGE, new float[] {0.75f, 0}, WeaponStat.FIRE_DAMAGE, new float[] {1, 1})),
        EXPLOSION(Map.of(WeaponStat.EXPLOSION_DAMAGE, new float[] {1, 1}, WeaponStat.AMMO, new float[] {0.2f, 0},
                WeaponStat.EFFECT, new float[] {0.5f, 0}, WeaponStat.FIRE_RATE, new float[] {0.15f, 0})),
        HEAL(Map.of(WeaponStat.DAMAGE, new float[] {0, 0}, WeaponStat.AMMO, new float[] {0.5f, 0}, WeaponStat.HEAL, new float[] {1, 0.1f}));

        private final Map<WeaponStat, float[]> stats;

        Type(Map<WeaponStat, float[]> stats) {
            this.stats = stats;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Tooltip order of 1.7.10 addDetails. */
    private static final Map<Type, WeaponStat[]> TOOLTIP = Map.of(
            Type.DAMAGE, new WeaponStat[] {WeaponStat.DAMAGE, WeaponStat.AMMO, WeaponStat.EFFECT},
            Type.FIRE, new WeaponStat[] {WeaponStat.AMMO, WeaponStat.DAMAGE, WeaponStat.FIRE_DAMAGE},
            Type.EXPLOSION, new WeaponStat[] {WeaponStat.EXPLOSION_DAMAGE, WeaponStat.AMMO, WeaponStat.EFFECT, WeaponStat.FIRE_RATE},
            Type.HEAL, new WeaponStat[] {WeaponStat.DAMAGE, WeaponStat.AMMO, WeaponStat.HEAL});

    private final Type type;

    public WeaponBarrelItem(Type type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    /** Explosion and heal barrels are phaser-only (1.7.10 supportsModule of the rifle, shotgun and sniper). */
    public boolean phaserOnly() {
        return type == Type.EXPLOSION || type == Type.HEAL;
    }

    @Override
    public int getSlot(ItemStack module) {
        return SLOT_BARREL;
    }

    @Override
    public float modifyStat(WeaponStat stat, ItemStack module, ItemStack weapon, float original) {
        float[] change = type.stats.get(stat);
        return change == null ? original : original * change[0] + change[1];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        for (WeaponStat stat : TOOLTIP.get(type)) {
            float[] change = type.stats.get(stat);
            // 1.7.10 weaponStatToInfo: multipliers as %, additive stats as the amount added; heal is good above 0
            float value = change[1] != 0 ? change[1] : change[0];
            boolean good = stat == WeaponStat.HEAL ? value > 0 : value >= 1;
            tooltip.accept(Component.translatable("weapon_stat." + MatterOverdrive.MODID + "." + stat.name().toLowerCase(Locale.ROOT))
                    .append(": " + Math.round(value * 100) + "%").withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }
}
