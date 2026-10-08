package matteroverdrive.menu;

import matteroverdrive.block.entity.AnalyzerBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiMatterAnalyzer: slot list on the left (input, pattern drive, battery). */
public class AnalyzerMenu extends MachineMenu<AnalyzerBlockEntity> {
    public AnalyzerMenu(int id, Inventory inventory, AnalyzerBlockEntity machine, ContainerData data) {
        super(MOMenus.ANALYZER.get(), id, inventory, machine, data);
    }

    public AnalyzerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (AnalyzerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, AnalyzerBlockEntity.INPUT, 8, 55);
        homeSlot(inv, AnalyzerBlockEntity.DATABASE, 8, 82);
        homeSlot(inv, machine.getBatterySlot(), 8, 109);
    }
}
