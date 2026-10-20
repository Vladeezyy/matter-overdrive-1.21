package matteroverdrive.menu;

import matteroverdrive.block.entity.SolarPanelBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class SolarPanelMenu extends MachineMenu<SolarPanelBlockEntity> {
    public SolarPanelMenu(int id, Inventory inventory, SolarPanelBlockEntity machine, ContainerData data) {
        super(MOMenus.SOLAR_PANEL.get(), id, inventory, machine, data);
    }

    public SolarPanelMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (SolarPanelBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    /** The solar panel has no slots of its own (1.7.10 GuiSolarPanel removed the slot list). */
    @Override
    protected void addMachineSlots(MachineInventory inv) {}
}
