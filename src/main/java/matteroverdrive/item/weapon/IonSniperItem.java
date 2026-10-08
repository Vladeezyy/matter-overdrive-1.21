package matteroverdrive.item.weapon;

import matteroverdrive.init.MOSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 IonSniper: 21 damage every 30 ticks for 3072 FE, range 96, speed 8, 100 max heat (+80% of max per shot),
 * accuracy 1 + heat x 0.1 zoomed, 5 + heat x 0.3 from the hip; zoom 0.4.
 */
public class IonSniperItem extends EnergyWeaponItem {
    public IonSniperItem(Properties properties) {
        super(properties, 96, 30, 21, 3072, 100, 8, 0.4f);
    }

    @Override
    protected float baseAccuracy(ItemStack weapon, boolean zoomed) {
        return zoomed ? 1f + getHeat(weapon) * 0.1f : 5f + getHeat(weapon) * 0.3f;
    }

    @Override
    protected void fire(ServerLevel level, Player shooter, ItemStack weapon, boolean zoomed) {
        spawnBolt(level, shooter, weapon, getDamage(weapon, shooter), getAccuracy(weapon, shooter, zoomed)).setRenderSize(0.8f);
        playShot(level, shooter, MOSounds.SNIPER_RIFLE_FIRE.get());
        addHeatAfterShot(weapon, level, shooter, getHeat(weapon) + getMaxHeat(weapon) * 0.8f);
    }
}
