package matteroverdrive.menu;

import matteroverdrive.block.entity.TransporterBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiTransporter (225x220, hotbar only): flash drive and battery slots on the left. */
public class TransporterMenu extends MachineMenu<TransporterBlockEntity> {
    public TransporterMenu(int id, Inventory inventory, TransporterBlockEntity machine, ContainerData data) {
        super(MOMenus.TRANSPORTER.get(), id, inventory, machine, data);
    }

    public TransporterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (TransporterBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    public int height() {
        return 220;
    }

    @Override
    protected boolean showMainInventory() {
        return false;
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, TransporterBlockEntity.FLASH_DRIVE, 8, 55);
        homeSlot(inv, machine.getBatterySlot(), 8, 82);
    }
}
