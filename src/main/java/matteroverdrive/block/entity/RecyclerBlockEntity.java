package matteroverdrive.block.entity;

import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.MatterDustItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.RecyclerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;

/**
 * 1.7.10 TileEntityMachineMatterRecycler: refines matter dust into refined matter dust carrying the same matter,
 * over 80 * ln(1 + matter)^2 ticks for matter * 1000 FE.
 */
public class RecyclerBlockEntity extends MachineBlockEntity {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int ENERGY_STORAGE = 512000;
    public static final int SPEED_PER_MATTER = 80;
    public static final int ENERGY_PER_MATTER = 1000;

    private int recycleTime;

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.RECYCLER.get(), pos, state, slots(), true, 4, ENERGY_STORAGE, ENERGY_STORAGE, ENERGY_STORAGE,
                Set.of(UpgradeType.SPEED, UpgradeType.POWER_STORAGE, UpgradeType.POWER_USAGE));
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.INPUT, r -> r.is(MOItems.MATTER_DUST.get()));
        b.add(MachineInventory.Role.OUTPUT, r -> r.is(MOItems.MATTER_DUST_REFINED.get()));
        return b;
    }

    private int inputMatter() {
        ItemStack in = inventory.getStack(INPUT);
        return in.is(MOItems.MATTER_DUST.get()) ? MatterDustItem.getMatter(in) : 0;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        int amount = inputMatter();
        if (!redstoneAllows || amount <= 0 || !canPutInOutput(amount)) {
            recycleTime = 0;
            return false;
        }
        int drain = getEnergyDrainPerTick(amount);
        if (energy.getEnergy() < drain) {
            return false;
        }
        energy.add(-drain);
        recycleTime++;
        if (recycleTime >= getSpeed(amount)) {
            recycleTime = 0;
            ItemStack out = inventory.getStack(OUTPUT);
            inventory.setStack(OUTPUT, out.isEmpty() ? MatterDustItem.withMatter(MOItems.MATTER_DUST_REFINED.get(), amount) : out.copyWithCount(out.getCount() + 1));
            inventory.shrink(INPUT, 1);
        }
        setChanged();
        return true;
    }

    private boolean canPutInOutput(int amount) {
        ItemStack out = inventory.getStack(OUTPUT);
        return out.isEmpty() || (MatterDustItem.getMatter(out) == amount && out.getCount() < out.getMaxStackSize());
    }

    public int getSpeed(int amount) {
        double m = Math.log1p(amount);
        return Math.max(1, (int) Math.round(SPEED_PER_MATTER * m * m * getUpgradeMultiplier(UpgradeType.SPEED)));
    }

    public int getEnergyDrainPerTick(int amount) {
        return (int) Math.round(amount * ENERGY_PER_MATTER * getUpgradeMultiplier(UpgradeType.POWER_USAGE)) / getSpeed(amount);
    }

    @Override
    public float getProgress() {
        int amount = inputMatter();
        return amount > 0 ? (float) recycleTime / getSpeed(amount) : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("recycle_time", recycleTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        recycleTime = input.getIntOr("recycle_time", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RecyclerMenu(id, inventory, this, dataAccess);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getLoopSound() {
        return matteroverdrive.init.MOSounds.MACHINE.get();
    }
}
