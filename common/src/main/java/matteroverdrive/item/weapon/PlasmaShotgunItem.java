package matteroverdrive.item.weapon;

import matteroverdrive.init.MOSounds;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 PlasmaShotgun: 10 bolts sharing 16 damage every 22 ticks for 2560 FE, range 16, speed 3, 80 max heat
 * (heat after a shot: (heat + 6) x 4.2), accuracy 5 + heat x 0.3. No zoom. The attack key fires the full spread;
 * holding use charges the shot (plasma_shotgun_charging) and releasing it fires fewer, bigger, tighter bolts that fly
 * further: over 20 ticks the count drops from 10 to 1 (the damage is split between them), the spread and the extra
 * range follow the count.
 */
public class PlasmaShotgunItem extends EnergyWeaponItem {
    public static final int SHOTS = 10;
    public static final int MAX_CHARGE_TIME = 20;

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
        fireBolts(level, shooter, weapon, SHOTS, zoomed);
    }

    /** 1.7.10 spawnProjectile: render size (10 / count) x 0.5 with integer division, as 1.7.10. */
    private void fireBolts(ServerLevel level, LivingEntity shooter, ItemStack weapon, int count, boolean zoomed) {
        float percent = count / (float) SHOTS;
        float damage = getDamage(weapon, shooter) / count;
        float accuracy = getAccuracy(weapon, shooter, zoomed) * percent;
        int range = getRange(weapon) + (int) (getRange(weapon) * (1 - percent));
        for (int i = 0; i < count; i++) {
            spawnBolt(level, shooter, weapon, damage, accuracy, range).setRenderSize((SHOTS / count) * 0.5f);
        }
        playShot(level, shooter, MOSounds.PLASMA_SHOTGUN_SHOT.get());
        addHeatAfterShot(weapon, level, shooter, (getHeat(weapon) + 6) * 4.2f);
    }

    /** 1.7.10 onItemRightClick: start charging when the weapon could fire. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var reload = super.use(level, player, hand);
        if (reload.getResult() != InteractionResult.PASS) return reload;
        ItemStack weapon = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(weapon.getItem()) || !canFire(weapon) || player.isUsingItem()) return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server) {
            // 1.7.10: volume 3-3.2, pitch 0.9 x rand x 0.2 (clamped to the 0.5 minimum)
            server.playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.PLASMA_SHOTGUN_CHARGING.get(), SoundSource.PLAYERS,
                    3 + server.getRandom().nextFloat() * 0.2f, 0.9f * server.getRandom().nextFloat() * 0.2f);
        }
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /** 1.7.10 onPlayerStoppedUsing: the longer the charge, the fewer bolts. */
    @Override
    public void releaseUsing(ItemStack weapon, Level level, LivingEntity entity, int timeLeft) {
        release(weapon, level, entity, timeLeft);
    }

    /** releaseUsing; returns whether it fired (1.21.10's releaseUsing result, used by the GameTests). */
    public boolean release(ItemStack weapon, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return false;
        stopChargingSound(server, player);
        if (player.getCooldowns().isOnCooldown(weapon.getItem()) || !canFire(weapon)) return false;
        int elapsed = getUseDuration(weapon, entity) - timeLeft;
        int count = chargedShots(elapsed);
        setEnergy(weapon, getEnergy(weapon) - getEnergyPerShot(weapon));
        fireBolts(server, player, weapon, count, false);
        player.getCooldowns().addCooldown(weapon.getItem(), getShootCooldown(weapon));
        return true;
    }

    public static int chargedShots(int chargeTicks) {
        return Math.max(1, (int) ((1f - chargeTicks / (float) MAX_CHARGE_TIME) * SHOTS));
    }

    /** 1.7.10 stopChargingSound (client side there): stops the charge sound for everyone near. */
    private static void stopChargingSound(ServerLevel level, ServerPlayer player) {
        var stop = new ClientboundStopSoundPacket(MOSounds.PLASMA_SHOTGUN_CHARGING.getId(), SoundSource.PLAYERS);
        for (ServerPlayer to : level.players()) {
            if (to.distanceToSqr(player) < 64 * 64) to.connection.send(stop);
        }
    }
}
