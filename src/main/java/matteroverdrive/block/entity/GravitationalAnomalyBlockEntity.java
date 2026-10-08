package matteroverdrive.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.matter.MatterHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 TileEntityGravitationalAnomaly. Its mass decides everything:
 * real mass m = ln(1 + mass * 1e-5) x suppression, pull range sqrt(m * G / 0.01), block break range half of that,
 * event horizon max(2 G m / c^2, 0.5), break strength 4 m x suppression falling off with distance.
 * It pulls entities, swallows items (their matter becomes mass), falling blocks and living things, tears out
 * up to 6 weak blocks every 6 ticks, and collapses into an explosion when it swallows a nether star.
 * Gravitational stabilizers suppress it while their beam hits it.
 */
public class GravitationalAnomalyBlockEntity extends BlockEntity {
    public static final ResourceKey<DamageType> BLACK_HOLE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "black_hole"));
    public static final int BLOCK_DESTROY_DELAY = 6;
    public static final int MAX_BLOCKS_PER_HARVEST = 6;
    public static final int MAX_LIQUIDS_PER_HARVEST = 32;
    public static final double STRENGTH_MULTIPLIER = 0.00001;
    public static final double G = 6.67384;
    public static final double G2 = G * 2;
    public static final double C = 2.99792458;
    public static final double CC = C * C;

    /** A stabilizer beam: lasts {@code time} ticks unless refreshed, multiplies the real mass by {@code amount}. */
    public record Suppressor(BlockPos source, int time, float amount) {
        public static final Codec<Suppressor> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("source").forGetter(Suppressor::source),
                Codec.INT.fieldOf("time").forGetter(Suppressor::time),
                Codec.FLOAT.fieldOf("amount").forGetter(Suppressor::amount)).apply(i, Suppressor::new));
    }

    private long mass;
    private float suppression = 1;
    private final List<Suppressor> suppressors = new ArrayList<>();
    private long lastSyncedMass;

    public GravitationalAnomalyBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.GRAVITATIONAL_ANOMALY.get(), pos, state);
        this.mass = 2048 + RandomSource.create().nextInt(8192);
    }

    // --- 1.7.10 formulas -------------------------------------------------------------------------

    public long getMass() {
        return mass;
    }

    public void setMass(long mass) {
        this.mass = mass;
        setChanged();
    }

    public float getSuppression() {
        return suppression;
    }

    public double getRealMassUnsuppressed() {
        return Math.log1p(Math.max(mass, 0) * STRENGTH_MULTIPLIER);
    }

    public double getRealMass() {
        return getRealMassUnsuppressed() * suppression;
    }

    public double getEventHorizon() {
        return Math.max(G2 * getRealMass() / CC, 0.5);
    }

    public double getMaxRange() {
        return Math.sqrt(getRealMass() * (G / 0.01));
    }

    public double getBlockBreakRange() {
        return getMaxRange() / 2;
    }

    public double getAcceleration(double distanceSq) {
        return G * (getRealMass() / Math.max(distanceSq, 0.0001));
    }

    public float getBreakStrength(float distance, float maxRange) {
        return (float) getRealMass() * 4 * suppression * (1 - distance / maxRange);
    }

    public void suppress(Suppressor suppressor) {
        for (int i = 0; i < suppressors.size(); i++) {
            Suppressor s = suppressors.get(i);
            if (s.source().equals(suppressor.source())) {
                suppressors.set(i, new Suppressor(s.source(), Math.max(s.time(), suppressor.time()), suppressor.amount()));
                return;
            }
        }
        suppressors.add(suppressor);
    }

    // --- ticking ---------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, GravitationalAnomalyBlockEntity anomaly) {
        anomaly.updateSuppression();
        anomaly.pullEntities((ServerLevel) level);
        if (anomaly.isRemoved()) return;   // collapsed
        if (level.getGameTime() % BLOCK_DESTROY_DELAY == 0) {
            anomaly.destroyBlocks((ServerLevel) level);
        }
        if (level.getGameTime() % 20 == 0 && anomaly.mass != anomaly.lastSyncedMass) {
            anomaly.lastSyncedMass = anomaly.mass;
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    /** Set by the client: keeps the anomaly's wind sound going (1.7.10 manageSound). */
    public static java.util.function.Consumer<GravitationalAnomalyBlockEntity> clientSoundTick = anomaly -> {};
    /** The client's playing wind sound. */
    public @org.jetbrains.annotations.Nullable Object clientSound;

    /** 1.7.10 spawnParticles: one particle per tick from a random point in break range, drawn to the core. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, GravitationalAnomalyBlockEntity anomaly) {
        clientSoundTick.accept(anomaly);
        RandomSource r = level.getRandom();
        double radius = Math.max(1, anomaly.getBlockBreakRange());
        Vec3 dir = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(radius);
        Vec3 c = pos.getCenter();
        level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + dir.x, c.y + dir.y, c.z + dir.z, -dir.x * 0.05, -dir.y * 0.05, -dir.z * 0.05);
    }

    private void updateSuppression() {
        float total = 1;
        Iterator<Suppressor> it = suppressors.iterator();
        List<Suppressor> next = new ArrayList<>();
        while (it.hasNext()) {
            Suppressor s = it.next();
            if (s.time() > 0) {
                total *= s.amount();
                next.add(new Suppressor(s.source(), s.time() - 1, s.amount()));
            }
        }
        suppressors.clear();
        suppressors.addAll(next);
        if (total != suppression) {
            suppression = total;
            setChanged();
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    private void pullEntities(ServerLevel level) {
        double range = getMaxRange() + 1;
        Vec3 center = getBlockPos().getCenter();
        double horizon = getEventHorizon();
        for (Entity entity : level.getEntities((Entity) null, new AABB(getBlockPos()).inflate(range), e -> !e.isSpectator())) {
            if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 pos = entity.position();
            double distSq = pos.distanceToSqr(center);
            if (distSq > range * range) continue;
            Vec3 pull = center.subtract(pos).normalize().scale(getAcceleration(distSq));
            if (intersects(pos, pull, center, horizon)) {
                consume(level, entity);
                if (isRemoved()) return;
            }
            // 1.7.10: the space-time equalizer chestplate, or an android with the equalizer stat, isn't pulled
            if (entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.CHEST).is(MOItems.SPACETIME_EQUALIZER.get())
                    || entity instanceof net.minecraft.world.entity.player.Player player && matteroverdrive.android.Android.isAndroid(player)
                    && matteroverdrive.android.Android.get(player).isUnlocked(matteroverdrive.android.BioticStats.EQUALIZER, 0)) {
                continue;
            }
            if (!entity.isRemoved()) {
                entity.addDeltaMovement(pull);
                entity.hurtMarked = true;      // players only see velocity the server marks as changed
            }
        }
    }

    private static boolean intersects(Vec3 origin, Vec3 dir, Vec3 anomaly, double radius) {
        if (origin.distanceTo(anomaly) <= radius) return true;
        Vec3 d = origin.subtract(anomaly);
        double c = d.length();
        double v = d.dot(dir);
        return radius * radius - (c * c - v * v) >= 0;
    }

    private void consume(ServerLevel level, Entity entity) {
        if (entity instanceof ItemEntity item) {
            ItemStack stack = item.getItem();
            addMass((long) MatterHelper.getMatter(level.getServer(), stack) * stack.getCount());
            item.discard();
            if (stack.is(Items.NETHER_STAR)) collapse(level);
        } else if (entity instanceof FallingBlockEntity falling) {
            addMass(MatterHelper.getMatter(level.getServer(), new ItemStack(falling.getBlockState().getBlock())));
            falling.discard();
        } else if (entity instanceof LivingEntity living) {
            float strength = getBreakStrength((float) Math.sqrt(entity.distanceToSqr(getBlockPos().getCenter())), (float) getMaxRange());
            addMass((long) Math.min(living.getHealth(), strength));
            if (living.getHealth() <= strength && !(living instanceof Player)) {
                living.discard();
            } else {
                living.hurtServer(level, level.damageSources().source(BLACK_HOLE), strength);
            }
            // 1.7.10 MOEventGravitationalAnomalyConsume.Post ("The belly of the beast")
            if (living instanceof Player player) {
                matteroverdrive.quest.QuestEvents.onEvent(player, new matteroverdrive.quest.QuestEvents.AnomalyConsume(getBlockPos()));
            }
        }
        setChanged();
    }

    private void addMass(long amount) {
        try {
            mass = Math.addExact(mass, amount);
        } catch (ArithmeticException e) {
            mass = Long.MAX_VALUE;
        }
    }

    /** 1.7.10 collapse(): the anomaly vanishes in an explosion of twice its unsuppressed real mass. */
    public void collapse(ServerLevel level) {
        BlockPos pos = getBlockPos();
        float power = (float) getRealMassUnsuppressed() * 2;
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
    }

    private void destroyBlocks(ServerLevel level) {
        int range = (int) Math.floor(getBlockBreakRange());
        double horizon = getEventHorizon();
        BlockPos center = getBlockPos();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range - 1, range - 1, range - 1))) {
            if (p.equals(center)) continue;
            BlockState state = level.getBlockState(p);
            if (state.isAir()) continue;
            double distance = Math.sqrt(p.distSqr(center));
            float hardness = state.getBlock() instanceof LiquidBlock ? 1 : state.getDestroySpeed(level, p);
            if (distance <= range && hardness >= 0 && (distance < horizon || hardness < getBreakStrength((float) distance, range))) {
                candidates.add(p.immutable());
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        int solids = 0, liquids = 0;
        for (BlockPos p : candidates) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof LiquidBlock) {
                if (liquids++ < MAX_LIQUIDS_PER_HARVEST) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            } else if (solids < MAX_BLOCKS_PER_HARVEST) {
                // 1.7.10 FALLING_BLOCKS: the block flies into the anomaly instead of dropping
                FallingBlockEntity falling = FallingBlockEntity.fall(level, p, state);
                falling.setNoGravity(true);
                falling.noPhysics = true;
                solids++;
            }
        }
    }

    // --- persistence and sync ----------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("mass", mass);
        output.putFloat("suppression", suppression);
        var list = output.list("suppressors", Suppressor.CODEC);
        suppressors.forEach(list::add);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mass = input.getLongOr("mass", mass);
        suppression = input.getFloatOr("suppression", 1);
        suppressors.clear();
        input.listOrEmpty("suppressors", Suppressor.CODEC).stream().forEach(suppressors::add);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    /** For the anomaly's tooltip when scanned (1.7.10 addInfo). */
    public String describe() {
        return String.format(java.util.Locale.ROOT, "mass %d, range %.2f, break range %.2f, horizon %.2f, strength %.2f",
                mass, getMaxRange(), getBlockBreakRange(), getEventHorizon(), getRealMass() * 4 * suppression);
    }
}
