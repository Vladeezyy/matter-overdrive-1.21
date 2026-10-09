package matteroverdrive.block.entity;

import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.MatterDustItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.matter.MatterRegistry;
import matteroverdrive.menu.ReplicatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 1.7.10 TileEntityMachineReplicator: replicates the item of its current task from Matter Plasma.
 * Time 120 * ln(1 + m)^2 (SPEED scales the part above 60 ticks), energy m * 16000, matter m per item.
 * Failure chance 0.5% x FAIL plus up to 50% for an incomplete pattern; a failure leaves matter dust.
 * While working without 5 tritanium plates of shielding it irradiates living things within 8 blocks.
 */
public class ReplicatorBlockEntity extends MachineBlockEntity {
    public static final int OUTPUT = 0;
    public static final int SECOND_OUTPUT = 1;
    public static final int SHIELDING = 2;
    public static final int MATTER_STORAGE = 1024;
    public static final int ENERGY_STORAGE = 512000;
    public static final int MATTER_TRANSFER = 128;
    public static final int SPEED_PER_MATTER = 120;
    public static final int ENERGY_PER_MATTER = 16000;
    public static final double FAIL_CHANCE = 0.005;
    public static final int RADIATION_DELAY = 5;
    public static final int RADIATION_RANGE = 8;
    public static final int FULL_SHIELDING = 5;

    /** The pattern being replicated and how many copies are still wanted. */
    public record Task(ItemPattern pattern, int count) {
        public static final Codec<Task> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemPattern.CODEC.fieldOf("pattern").forGetter(Task::pattern),
                Codec.INT.fieldOf("count").forGetter(Task::count)).apply(i, Task::new));
    }

    private @Nullable Task task;
    private int replicateTime;

    public ReplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.REPLICATOR.get(), pos, state, withFilterSlot(slots()), true, 4, ENERGY_STORAGE, ENERGY_STORAGE, ENERGY_STORAGE,
                Set.of(UpgradeType.POWER_STORAGE, UpgradeType.SPEED, UpgradeType.FAIL, UpgradeType.POWER_USAGE, UpgradeType.MATTER_STORAGE));
        initMatter(MATTER_STORAGE, MATTER_TRANSFER, 0);
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.OUTPUT, r -> true);
        b.add(MachineInventory.Role.OUTPUT, r -> r.is(MOItems.MATTER_DUST.get()));
        b.add(MachineInventory.Role.INPUT, r -> r.is(MOItems.TRITANIUM_PLATE.get()));
        return b;
    }

    // --- tasks -----------------------------------------------------------------------------------

    public boolean isIdle() {
        return task == null;
    }

    public @Nullable Task getTask() {
        return task;
    }

    /** Called by a pattern monitor on the same network. */
    public void setTask(@Nullable Task task) {
        this.task = task;
        this.replicateTime = 0;
        setChanged();
        if (getLevel() != null && !getLevel().isClientSide()) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private int matterPerItem() {
        return task == null ? 0 : MatterRegistry.get(getLevel().getServer(), task.pattern().item().value());
    }

    // --- ticking ---------------------------------------------------------------------------------

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (task == null) return false;
        int m = matterPerItem();
        if (m <= 0) {           // the item lost its matter value (datapack change): drop the task
            setTask(null);
            return false;
        }
        ItemStack product = task.pattern().toStack();
        if (!redstoneAllows || matter.getMatter() < m || !canOutput(product) || !canOutputDust(m)) {
            return false;
        }
        int drain = getEnergyDrainPerTick(m);
        if (energy.getEnergy() < drain) return false;
        energy.add(-drain);
        if (getLevel().getGameTime() % RADIATION_DELAY == 0) irradiate();
        if (++replicateTime >= getSpeed(m)) {
            replicateTime = 0;
            replicate(product, m);
            // played whether the replication worked or failed, like 1.7.10
            getLevel().playSound(null, getBlockPos(), matteroverdrive.init.MOSounds.REPLICATE_SUCCESS.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.25f, 1);
        }
        setChanged();
        return true;
    }

    private void replicate(ItemStack product, int m) {
        matter.add(-m);
        if (getLevel().getRandom().nextFloat() < getFailChance(task.pattern())) {
            ItemStack dust = inventory.getStack(SECOND_OUTPUT);
            inventory.setStack(SECOND_OUTPUT, dust.isEmpty() ? MatterDustItem.withMatter(MOItems.MATTER_DUST.get(), m) : dust.copyWithCount(dust.getCount() + 1));
            return;
        }
        ItemStack out = inventory.getStack(OUTPUT);
        inventory.setStack(OUTPUT, out.isEmpty() ? product : out.copyWithCount(out.getCount() + 1));
        int left = task.count() - 1;
        setTask(left > 0 ? new Task(task.pattern(), left) : null);
    }

    private boolean canOutput(ItemStack product) {
        ItemStack out = inventory.getStack(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, product) && out.getCount() < out.getMaxStackSize());
    }

    private boolean canOutputDust(int m) {
        ItemStack dust = inventory.getStack(SECOND_OUTPUT);
        return dust.isEmpty() || (MatterDustItem.getMatter(dust) == m && dust.getCount() < dust.getMaxStackSize());
    }

    public int getShielding() {
        return Math.min(FULL_SHIELDING, inventory.getStack(SHIELDING).getCount());
    }

    /** 1.7.10 manageRadiation: nausea, weakness, hunger and poison, stronger closer and with less shielding. */
    private void irradiate() {
        int shielding = getShielding();
        if (shielding >= FULL_SHIELDING) return;
        BlockPos pos = getBlockPos();
        for (LivingEntity e : getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIATION_RANGE))) {
            double d = 1 - Mth.clamp(Math.sqrt(e.distanceToSqr(pos.getCenter())) / RADIATION_RANGE, 0, 1);
            d *= FULL_SHIELDING - shielding;
            addEffect(e, MobEffects.NAUSEA, Math.pow(5, d));
            addEffect(e, MobEffects.WEAKNESS, Math.pow(10, d));
            addEffect(e, MobEffects.HUNGER, Math.pow(12, d));
            addEffect(e, MobEffects.POISON, Math.pow(5, d));
        }
    }

    private static void addEffect(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, double duration) {
        int ticks = (int) Math.round(duration);
        if (ticks > 1) e.addEffect(new MobEffectInstance(effect, ticks, 0));
    }

    public double getFailChance(ItemPattern pattern) {
        double progressChance = 1 - pattern.progress() / (double) ItemPattern.MAX_PROGRESS;
        double m = getUpgradeMultiplier(UpgradeType.FAIL);
        return FAIL_CHANCE * m + progressChance * 0.5 + progressChance * 0.5 * m;
    }

    public int getSpeed(int matterAmount) {
        double l = Math.log1p(matterAmount);
        return (int) Math.round((SPEED_PER_MATTER * l * l - 60) * getUpgradeMultiplier(UpgradeType.SPEED)) + 60;
    }

    public int getEnergyDrainPerTick(int matterAmount) {
        return (int) Math.round(matterAmount * ENERGY_PER_MATTER * getUpgradeMultiplier(UpgradeType.POWER_USAGE)) / getSpeed(matterAmount);
    }

    @Override
    public float getProgress() {
        int m = matterPerItem();
        return m > 0 ? (float) replicateTime / getSpeed(m) : 0;
    }

    // --- persistence -----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("task", Task.CODEC, task);
        output.putInt("replicate_time", replicateTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        task = input.read("task", Task.CODEC).orElse(null);
        replicateTime = input.getIntOr("replicate_time", 0);
    }

    /** The client's view of the current task (synced with the block entity), for the GUI. */
    public Optional<Task> clientTask() {
        return Optional.ofNullable(task);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReplicatorMenu(id, inventory, this, dataAccess);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getLoopSound() {
        return matteroverdrive.init.MOSounds.MACHINE.get();
    }
}
