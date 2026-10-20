package matteroverdrive.entity;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 PlasmaBolt: a straight, gravity-free energy bolt that dies after travelling the weapon's range.
 * It deals plasma damage without knockback, sets targets on fire with a fire barrel, and detonates TNT it hits.
 */
public class PlasmaBolt extends Projectile {
    public static final ResourceKey<DamageType> PLASMA = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "plasma"));
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(PlasmaBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RENDER_SIZE = SynchedEntityData.defineId(PlasmaBolt.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float range = 32;
    private float fireMultiplier;
    private double travelled;

    public PlasmaBolt(EntityType<? extends PlasmaBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public PlasmaBolt(Level level, LivingEntity shooter, float damage, float range, int color) {
        this(MOEntities.PLASMA_BOLT.get(), level);
        setOwner(shooter);
        // leave from about the muzzle: a little ahead of and below the eyes
        Vec3 muzzle = shooter.getEyePosition().add(shooter.getLookAngle().scale(0.6)).add(0, -0.15, 0);
        setPos(muzzle.x, muzzle.y, muzzle.z);
        this.damage = damage;
        this.range = range;
        entityData.set(COLOR, color);
    }

    public void setFireMultiplier(float fireMultiplier) {
        this.fireMultiplier = fireMultiplier;
    }

    public void setRenderSize(float size) {
        entityData.set(RENDER_SIZE, size);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    public float getRenderSize() {
        return entityData.get(RENDER_SIZE);
    }

    public float getDamage() {
        return damage;
    }

    /** Fraction of the range still ahead (1 at the muzzle, 0 at the end); the renderer fades with it. */
    public float getLife() {
        return (float) Math.max(0, 1 - travelled / range);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0xFFFFFF);
        builder.define(RENDER_SIZE, 2f);   // 1.7.10 PlasmaBolt.renderSize default
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                hitTargetOrDeflectSelf(hit);
                if (isRemoved()) return;
            }
            travelled += motion.length();
            if (travelled > range) {
                discard();
                return;
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 before = target.getDeltaMovement();
        DamageSource source = server.damageSources().source(PLASMA, this, getOwner());
        target.invulnerableTime = 0;
        if (damage > 0 && target.hurt(source, damage)) {
            target.setDeltaMovement(before);     // 1.7.10 restored the target's motion: no knockback
            if (fireMultiplier > 0) target.igniteForSeconds(10 * fireMultiplier);   // 1.7.10 setFire(10 x multiplier)
        }
        matteroverdrive.network.BoltHitPayload.send(server, result.getLocation(), getDeltaMovement().scale(-1), getColor(), getRenderSize(),
                target instanceof LivingEntity ? matteroverdrive.network.BoltHitPayload.LIVING : matteroverdrive.network.BoltHitPayload.ENTITY);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        if (level() instanceof ServerLevel server && level().getBlockState(pos).getBlock() instanceof TntBlock) {
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            PrimedTnt tnt = new PrimedTnt(server, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    getOwner() instanceof LivingEntity l ? l : null);
            tnt.setFuse(0);
            server.addFreshEntity(tnt);
        }
        if (level() instanceof ServerLevel server) {
            matteroverdrive.network.BoltHitPayload.send(server, result.getLocation(), new Vec3(result.getDirection().getStepX(), result.getDirection().getStepY(), result.getDirection().getStepZ()),
                    getColor(), getRenderSize(), matteroverdrive.network.BoltHitPayload.BLOCK);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ValueOutput output = ValueOutput.of(tag, registryAccess());
        output.putFloat("damage", damage);
        output.putFloat("range", range);
        output.putDouble("travelled", travelled);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ValueInput input = ValueInput.of(tag, registryAccess());
        damage = input.getFloatOr("damage", 0);
        range = input.getFloatOr("range", 32);
        travelled = input.getDoubleOr("travelled", 0);
    }
}
