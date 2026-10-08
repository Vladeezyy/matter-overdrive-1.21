package matteroverdrive.menu;

import matteroverdrive.block.entity.ReplicatorBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/**
 * 1.7.10 GuiReplicator: the current pattern on top of the slot list (5, 49), then shielding and battery;
 * big output slots at (70, 52) and (96, 52).
 */
public class ReplicatorMenu extends MachineMenu<ReplicatorBlockEntity> {
    public ReplicatorMenu(int id, Inventory inventory, ReplicatorBlockEntity machine, ContainerData data) {
        super(MOMenus.REPLICATOR.get(), id, inventory, machine, data);
    }

    public ReplicatorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (ReplicatorBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, ReplicatorBlockEntity.OUTPUT, 73, 55);
        homeSlot(inv, ReplicatorBlockEntity.SECOND_OUTPUT, 99, 55);
        homeSlot(inv, ReplicatorBlockEntity.SHIELDING, 8, 79);
        homeSlot(inv, machine.getBatterySlot(), 8, 106);
    }
}
