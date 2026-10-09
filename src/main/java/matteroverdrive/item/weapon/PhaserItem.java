package matteroverdrive.item.weapon;

import java.util.function.Consumer;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 Phaser: a hand phaser that fires a continuous beam while use is held (range 18). Sneak-use cycles the
 * power level 0-5. Levels 0-2 stun (slowness and mining fatigue for (level + 1)^5 ticks), levels 3-5
 * kill (2, 4, 8 damage). Energy per tick 2.1^(level + 1); heat per tick (heat + 1) x 1.1 (x 1.15 at level 5).
 * Barrels add fire, healing or explosions; a fire barrel also lights what the beam hits.
 */
public class PhaserItem extends EnergyWeaponItem {
    public static final int RANGE = 18;
    public static final int MAX_LEVEL = 6;
    public static final int KILL_MODE_LEVEL = 3;
    public static final double ENERGY_MULTIPLY = 2.1;

    public PhaserItem(Properties properties) {
        super(properties, RANGE, 10, 0, 0, 80, 0, 0);
    }

    public static int getLevel(ItemStack phaser) {
        return phaser.getOrDefault(MODataComponents.PHASER_LEVEL.get(), 0);
    }

    public static boolean isKillMode(ItemStack phaser) {
        return getLevel(phaser) >= KILL_MODE_LEVEL;
    }

    @Override
    public int getEnergyUse(ItemStack weapon) {
        return Math.max(0, (int) modifyStat(WeaponStat.AMMO, weapon, (float) Math.pow(ENERGY_MULTIPLY, getLevel(weapon) + 1)));
    }

    @Override
    public int getEnergyPerShot(ItemStack weapon) {
        return getEnergyUse(weapon);      // the beam drains per tick
    }

    @Override
    public float getDamage(ItemStack weapon, LivingEntity shooter) {
        int level = getLevel(weapon);
        float base = level >= KILL_MODE_LEVEL ? (float) Math.pow(2, level - (KILL_MODE_LEVEL - 1)) : 0;
        return modifyStat(WeaponStat.DAMAGE, weapon, base) + (float) shooter.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    }

    /** 1.7.10 GetSleepTime: (level + 1)^5 ticks in stun mode, scaled by DAMAGE modules; none in kill mode. */
    public int getStunTime(ItemStack weapon) {
        int level = getLevel(weapon);
        return level < KILL_MODE_LEVEL ? (int) (Math.pow(level + 1, 5) * modifyStat(WeaponStat.DAMAGE, weapon, 1)) : 0;
    }

    @Override
    protected float baseAccuracy(ItemStack weapon, boolean zoomed) {
        return getHeat(weapon) / getMaxHeat(weapon);
    }

    /** The phaser doesn't fire with the attack key. */
    @Override
    public void tryFire(ServerPlayer player, ItemStack weapon, boolean zoomed) {}

    @Override
    protected void fire(ServerLevel level, LivingEntity shooter, ItemStack weapon, boolean zoomed) {}

    /** 1.7.10 Phaser: no sights slot; barrels (all four) and colour modules only. */
    @Override
    public boolean supportsSlot(int slot) {
        return slot != WeaponModule.SLOT_SIGHTS;
    }

    @Override
    public boolean supportsModule(ItemStack module) {
        return module.getItem() instanceof WeaponBarrelItem || module.getItem() instanceof WeaponColorModuleItem;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack phaser = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            phaser.set(MODataComponents.PHASER_LEVEL.get(), (getLevel(phaser) + 1) % MAX_LEVEL);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.PHASER_SWITCH_MODE.get(), SoundSource.PLAYERS, 1, 1);
            return InteractionResult.SUCCESS;
        }
        if (getEnergy(phaser) < getEnergyUse(phaser) && EnergyPackItem.reload(player, phaser)) {
            return InteractionResult.SUCCESS;
        }
        if (canFire(phaser)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack phaser, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)) return;
        if (!canFire(phaser)) {
            player.stopUsingItem();
            return;
        }
        setEnergy(phaser, getEnergy(phaser) - getEnergyUse(phaser));
        int levelBonus = (getLevel(phaser) + 1) / MAX_LEVEL;
        addHeatAfterShot(phaser, server, player, (getHeat(phaser) + 1) * (1.1f + 0.05f * levelBonus));
        int used = getUseDuration(phaser, user) - remaining;
        if (used % 10 == 0) {
            server.playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.PHASER_BEAM.get(), SoundSource.PLAYERS, 0.6f, 1);
        }
        shoot(server, player, phaser, used);
    }

    /** What the beam hits: the first entity or block within range along the shooter's look. */
    public static HitResult trace(Level level, Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 blockEnd = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, blockEnd,
                new AABB(eye, blockEnd).inflate(1), e -> !e.isSpectator() && e.isPickable() && e != player, 0.3f);
        return entity != null ? entity : block;
    }

    private void shoot(ServerLevel level, Player player, ItemStack phaser, int used) {
        HitResult hit = trace(level, player, getRange(phaser));
        int power = getLevel(phaser);
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target) {
            if (target instanceof Player && !level.getServer().isPvpAllowed()) return;
            float damage = getDamage(phaser, player);
            if (damage > 0) {
                Vec3 motion = target.getDeltaMovement();
                target.hurtServer(level, level.damageSources().source(matteroverdrive.entity.PlasmaBolt.PLASMA, player), damage);
                target.setDeltaMovement(motion);
            }
            int stun = getStunTime(phaser);
            if (stun > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 100));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, stun, 100));
                // 1.7.10 also gave jump boost -10 (no jumping); 1.21 effect levels can't be negative, slowness 100 pins them anyway
            }
            if (hasStat(WeaponStat.FIRE_DAMAGE, phaser) && isKillMode(phaser)) {
                target.igniteForSeconds(Math.round(modifyStat(WeaponStat.FIRE_DAMAGE, phaser, 0) * power));
            } else if (hasStat(WeaponStat.HEAL, phaser)) {
                target.heal(modifyStat(WeaponStat.HEAL, phaser, 0) * power);
            }
        } else if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK && hasStat(WeaponStat.FIRE_DAMAGE, phaser)) {
            BlockPos fire = bh.getBlockPos().relative(bh.getDirection());
            if (level.getBlockState(bh.getBlockPos()).isFlammable(level, bh.getBlockPos(), bh.getDirection())
                    && level.isEmptyBlock(fire) && player.mayUseItemAt(fire, bh.getDirection(), phaser)) {
                level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
            }
        }
        if (isKillMode(phaser) && hasStat(WeaponStat.EXPLOSION_DAMAGE, phaser) && used % getShootCooldown(phaser) == getShootCooldown(phaser) / 2) {
            Vec3 at = hit.getLocation();
            float power3 = modifyStat(WeaponStat.EXPLOSION_DAMAGE, phaser, 0) * power - MAX_LEVEL / 2f;
            if (power3 > 0) level.explode(player, at.x, at.y, at.z, power3, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void appendHoverText(ItemStack weapon, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        int level = getLevel(weapon);
        tooltip.accept(Component.translatable(isKillMode(weapon) ? "tooltip.matteroverdrive.phaser.kill" : "tooltip.matteroverdrive.phaser.stun",
                level + 1, MAX_LEVEL).withStyle(isKillMode(weapon) ? ChatFormatting.RED : ChatFormatting.BLUE));
        if (!isKillMode(weapon)) {
            tooltip.accept(Component.translatable("tooltip.matteroverdrive.phaser.stun_time", getStunTime(weapon) / 20f).withStyle(ChatFormatting.BLUE));
        }
        super.appendHoverText(weapon, context, tooltipLines, flag);
    }
}
