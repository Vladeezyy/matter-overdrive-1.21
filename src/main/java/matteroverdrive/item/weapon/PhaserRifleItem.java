package matteroverdrive.item.weapon;

import matteroverdrive.init.MOSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 PhaserRifle: 8 damage bolts every 11 ticks for 1024 FE, range 32, speed 4, 80 max heat
 * (heat after a shot: (heat + 4) x 2.2), accuracy 1 + heat / (zoomed ? 30 : 10), zoom 0.2.
 */
public class PhaserRifleItem extends EnergyWeaponItem {
    public PhaserRifleItem(Properties properties) {
        super(properties, 32, 11, 8, 1024, 80, 4, 0.2f);
    }

    @Override
    protected float baseAccuracy(ItemStack weapon, boolean zoomed) {
        return 1f + getHeat(weapon) / (zoomed ? 30f : 10f);
    }

    @Override
    protected void fire(ServerLevel level, Player shooter, ItemStack weapon, boolean zoomed) {
        spawnBolt(level, shooter, weapon, getDamage(weapon, shooter), getAccuracy(weapon, shooter, zoomed));
        playShot(level, shooter, MOSounds.PHASER_RIFLE_SHOT.get());
        addHeatAfterShot(weapon, level, shooter, (getHeat(weapon) + 4) * 2.2f);
    }
}
