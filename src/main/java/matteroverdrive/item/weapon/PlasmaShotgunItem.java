package matteroverdrive.item.weapon;

import matteroverdrive.init.MOSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 PlasmaShotgun: 10 bolts sharing 16 damage every 22 ticks for 2560 FE, range 16, speed 3, 80 max heat
 * (heat after a shot: (heat + 6) x 4.2), accuracy 5 + heat x 0.3. No zoom.
 * 1.7.10 also let you charge the shot to fire fewer, tighter bolts; the port always fires the full spread.
 */
public class PlasmaShotgunItem extends EnergyWeaponItem {
    public static final int SHOTS = 10;

    public PlasmaShotgunItem(Properties properties) {
        super(properties, 16, 22, 16, 2560, 80, 3, 0);
    }

    @Override
    protected float baseAccuracy(ItemStack weapon, boolean zoomed) {
        return 5f + getHeat(weapon) * 0.3f;
    }

    /** 1.7.10 PlasmaShotgun.supportsModule: colour modules and the damage or fire barrel only. */
    @Override
    public boolean supportsModule(ItemStack module) {
        return module.getItem() instanceof WeaponColorModuleItem
                || module.getItem() instanceof WeaponBarrelItem barrel && !barrel.phaserOnly();
    }

    @Override
    protected void fire(ServerLevel level, LivingEntity shooter, ItemStack weapon, boolean zoomed) {
        float damage = getDamage(weapon, shooter) / SHOTS;
        float accuracy = getAccuracy(weapon, shooter, zoomed);
        for (int i = 0; i < SHOTS; i++) {
            spawnBolt(level, shooter, weapon, damage, accuracy).setRenderSize(0.5f);
        }
        playShot(level, shooter, MOSounds.PLASMA_SHOTGUN_SHOT.get());
        addHeatAfterShot(weapon, level, shooter, (getHeat(weapon) + 6) * 4.2f);
    }
}
