package matteroverdrive.menu;

import matteroverdrive.block.entity.ChargingStationBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class ChargingStationMenu extends MachineMenu<ChargingStationBlockEntity> {
    public ChargingStationMenu(int id, Inventory inventory, ChargingStationBlockEntity machine, ContainerData data) {
        super(MOMenus.CHARGING_STATION.get(), id, inventory, machine, data);
    }

    public ChargingStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (ChargingStationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    /** The charging station has no slots of its own besides its upgrades. */
    @Override
    protected void addMachineSlots(MachineInventory inv) {}
}
