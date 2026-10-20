package matteroverdrive.menu;

import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiPatternStorage: drive slots in a 3x2 grid at (77, 37), battery and the scanner (link) slot in the slot list. */
public class PatternStorageMenu extends MachineMenu<PatternStorageBlockEntity> {
    public PatternStorageMenu(int id, Inventory inventory, PatternStorageBlockEntity machine, ContainerData data) {
        super(MOMenus.PATTERN_STORAGE.get(), id, inventory, machine, data);
    }

    public PatternStorageMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (PatternStorageBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        for (int i = 0; i < PatternStorageBlockEntity.DRIVES; i++) {
            homeSlot(inv, i, 80 + (i % 3) * 24, 40 + (i / 3) * 24);
        }
        homeSlot(inv, machine.getBatterySlot(), 8, 55);
        homeSlot(inv, PatternStorageBlockEntity.SCANNER, 8, 82);
    }
}
