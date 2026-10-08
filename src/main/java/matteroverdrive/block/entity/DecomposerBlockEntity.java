package matteroverdrive.block.entity;

import java.util.EnumSet;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.MatterDustItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.matter.MatterHelper;
import matteroverdrive.menu.DecomposerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * 1.7.10 TileEntityMachineDecomposer: turns an item into its matter (as Matter Plasma) over
 * 80 * ln(1 + matter)^2 ticks for matter * 8000 FE. With a 0.5% (x FAIL upgrade squared) chance the item becomes
 * matter dust instead. Every 32 ticks it pushes its matter into neighbouring tanks and pipes.
 */
public class DecomposerBlockEntity extends MachineBlockEntity {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int MATTER_STORAGE = 1024;
    public static final int ENERGY_STORAGE = 512000;
    public static final int MATTER_EXTRACT_SPEED = 32;
    public static final float FAIL_CHANCE = 0.005f;
    public static final int SPEED_PER_MATTER = 80;
    public static final int ENERGY_PER_MATTER = 8000;

    private int decomposeTime;

    public DecomposerBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.DECOMPOSER.get(), pos, state, slots(), true, 4, ENERGY_STORAGE, ENERGY_STORAGE, ENERGY_STORAGE,
                EnumSet.complementOf(EnumSet.of(UpgradeType.RANGE, UpgradeType.SECOND_OUTPUT)));
        initMatter(MATTER_STORAGE, 0, MATTER_STORAGE);
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.INPUT, r -> MatterHelper.hasMatter(r.toStack()));
        b.add(MachineInventory.Role.OUTPUT, r -> r.is(MOItems.MATTER_DUST.get()));
        return b;
    }

    private int inputMatter() {
        MinecraftServer server = getLevel().getServer();
        return server == null ? 0 : MatterHelper.getMatter(server, inventory.getStack(INPUT));
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (getLevel().getGameTime() % MATTER_EXTRACT_SPEED == 0) {
            pushMatter();
        }
        int amount = inputMatter();
        boolean canWork = redstoneAllows && amount > 0 && amount <= matter.getFreeSpace() && canPutInOutput(amount);
        if (!canWork) {
            decomposeTime = 0;
            return false;
        }
        int drain = getEnergyDrainPerTick(amount);
        if (energy.getEnergy() < drain) {
            return false;
        }
        energy.add(-drain);
        decomposeTime++;
        if (decomposeTime >= getSpeed(amount)) {
            decomposeTime = 0;
            decompose(amount);
        }
        setChanged();
        return true;
    }

    private void decompose(int amount) {
        if (getLevel().getRandom().nextFloat() < getFailChance()) {
            ItemStack out = inventory.getStack(OUTPUT);
            inventory.setStack(OUTPUT, out.isEmpty() ? MatterDustItem.withMatter(MOItems.MATTER_DUST.get(), amount) : out.copyWithCount(out.getCount() + 1));
        } else {
            matter.add(amount);
        }
        inventory.shrink(INPUT, 1);
    }

    /** Failed decompositions stack only onto dust of the same matter amount (1.7.10 compared the damage value). */
    private boolean canPutInOutput(int amount) {
        ItemStack out = inventory.getStack(OUTPUT);
        return out.isEmpty() || (MatterDustItem.getMatter(out) == amount && out.getCount() < out.getMaxStackSize());
    }

    private void pushMatter() {
        for (Direction dir : Direction.values()) {
            if (matter.getMatter() <= 0) return;
            var target = getLevel().getCapability(Capabilities.Fluid.BLOCK, getBlockPos().relative(dir), dir.getOpposite());
            if (target == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int moved = target.insert(FluidResource.of(matteroverdrive.init.MOFluids.MATTER_PLASMA.get()), matter.getMatter(), tx);
                if (moved > 0) {
                    tx.commit();
                    matter.add(-moved);
                }
            }
        }
    }

    public double getFailChance() {
        double m = getUpgradeMultiplier(UpgradeType.FAIL);
        return FAIL_CHANCE * m * m;
    }

    public int getSpeed(int amount) {
        double m = Math.log1p(amount);
        return (int) Math.round(SPEED_PER_MATTER * m * m * getUpgradeMultiplier(UpgradeType.SPEED));
    }

    public int getEnergyDrainPerTick(int amount) {
        int speed = getSpeed(amount);
        if (speed <= 0) return 0;
        return (int) Math.round(amount * ENERGY_PER_MATTER * getUpgradeMultiplier(UpgradeType.POWER_USAGE)) / speed;
    }

    @Override
    public float getProgress() {
        int speed = getSpeed(inputMatter());
        return speed > 0 ? (float) decomposeTime / speed : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("decompose_time", decomposeTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        decomposeTime = input.getIntOr("decompose_time", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DecomposerMenu(id, inventory, this, dataAccess);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getLoopSound() {
        return matteroverdrive.init.MOSounds.MACHINE.get();
    }
}
