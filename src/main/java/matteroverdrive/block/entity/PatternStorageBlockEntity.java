package matteroverdrive.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.item.PatternDriveItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.menu.PatternStorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7.10 TileEntityMachinePatternStorage: holds six pattern drives and offers their patterns to the matter network.
 * It works while it has energy (64000 FE buffer, 128 FE/t in).
 */
public class PatternStorageBlockEntity extends MachineBlockEntity {
    public static final int DRIVES = 6;
    public static final int ENERGY_CAPACITY = 64000;
    public static final int ENERGY_TRANSFER = 128;

    public PatternStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.PATTERN_STORAGE.get(), pos, state, slots(), true, 4, ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER,
                Set.of(UpgradeType.POWER_STORAGE, UpgradeType.POWER_USAGE));
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        for (int i = 0; i < DRIVES; i++) {
            b.add(MachineInventory.Role.OTHER, r -> r.getItem() instanceof PatternDriveItem, 1);
        }
        return b;
    }

    public boolean isOnline() {
        return energy.getEnergy() > 0;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        return isOnline();
    }

    public List<ItemPattern> getPatterns() {
        List<ItemPattern> all = new ArrayList<>();
        if (!isOnline()) return all;
        for (int i = 0; i < DRIVES; i++) all.addAll(PatternDriveItem.getPatterns(inventory.getStack(i)));
        return all;
    }

    public boolean canAccept(Item item) {
        if (!isOnline()) return false;
        for (int i = 0; i < DRIVES; i++) {
            ItemStack drive = inventory.getStack(i);
            if (drive.getItem() instanceof PatternDriveItem d && d.canAccept(drive, item)) return true;
        }
        return false;
    }

    /** Adds analysis progress, preferring a drive that already holds this item's pattern. */
    public boolean addProgress(Item item, int amount) {
        if (!isOnline()) return false;
        for (boolean existingOnly : new boolean[] {true, false}) {
            for (int i = 0; i < DRIVES; i++) {
                ItemStack drive = inventory.getStack(i);
                if (!(drive.getItem() instanceof PatternDriveItem d)) continue;
                boolean has = PatternDriveItem.getPatterns(drive).stream().anyMatch(p -> p.is(item));
                if (existingOnly && !has) continue;
                ItemStack copy = drive.copy();
                if (d.addProgress(copy, item, amount)) {
                    inventory.setStack(i, copy);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PatternStorageMenu(id, inventory, this, dataAccess);
    }
}
