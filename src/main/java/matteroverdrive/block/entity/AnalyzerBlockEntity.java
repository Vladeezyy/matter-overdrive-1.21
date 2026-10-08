package matteroverdrive.block.entity;

import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.item.PatternDriveItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.matter.MatterHelper;
import matteroverdrive.menu.AnalyzerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 1.7.10 TileEntityMachineMatterAnalyzer: analyses an item that holds matter for 800 ticks and 64000 FE, consuming
 * it and adding 20% to its pattern (5 items make a complete pattern). The pattern goes to the pattern drive in
 * the database slot.
 */
public class AnalyzerBlockEntity extends MachineBlockEntity {
    public static final int INPUT = 0;
    public static final int DATABASE = 1;
    public static final int PROGRESS_PER_ITEM = 20;
    public static final int ENERGY_STORAGE = 512000;
    public static final int ENERGY_TRANSFER = 512;
    public static final int ANALYZE_SPEED = 800;
    public static final int ENERGY_PER_ITEM = 64000;

    private int analyzeTime;

    public AnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.ANALYZER.get(), pos, state, slots(), true, 4, ENERGY_STORAGE, ENERGY_TRANSFER, ENERGY_TRANSFER,
                Set.of(UpgradeType.POWER_USAGE, UpgradeType.POWER_STORAGE, UpgradeType.FAIL, UpgradeType.OUTPUT, UpgradeType.SPEED));
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.INPUT, r -> MatterHelper.hasMatter(r.toStack()));
        b.add(MachineInventory.Role.OTHER, r -> r.getItem() instanceof PatternDriveItem, 1);
        return b;
    }

    /** Whether the current input can be analysed into the drive. */
    public boolean canAnalyze() {
        ItemStack input = inventory.getStack(INPUT);
        ItemStack drive = inventory.getStack(DATABASE);
        return !input.isEmpty() && MatterHelper.hasMatter(input)
                && drive.getItem() instanceof PatternDriveItem d && d.canAccept(drive, input.getItem());
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (!redstoneAllows || !canAnalyze()) {
            analyzeTime = 0;
            return false;
        }
        int drain = getEnergyDrainPerTick();
        if (energy.getEnergy() < drain) return false;
        energy.add(-drain);
        if (++analyzeTime >= getSpeed()) {
            analyzeTime = 0;
            analyze();
        }
        setChanged();
        return true;
    }

    private void analyze() {
        ItemStack drive = inventory.getStack(DATABASE).copy();
        ItemStack input = inventory.getStack(INPUT);
        if (((PatternDriveItem) drive.getItem()).addProgress(drive, input.getItem(), PROGRESS_PER_ITEM)) {
            inventory.setStack(DATABASE, drive);
            inventory.shrink(INPUT, 1);
        }
    }

    public int getSpeed() {
        return Math.max(1, (int) Math.round(ANALYZE_SPEED * getUpgradeMultiplier(UpgradeType.SPEED)));
    }

    public int getEnergyDrainPerTick() {
        return (int) Math.round(ENERGY_PER_ITEM * getUpgradeMultiplier(UpgradeType.POWER_USAGE)) / getSpeed();
    }

    @Override
    public float getProgress() {
        return (float) analyzeTime / getSpeed();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("analyze_time", analyzeTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        analyzeTime = input.getIntOr("analyze_time", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AnalyzerMenu(id, inventory, this, dataAccess);
    }
}
