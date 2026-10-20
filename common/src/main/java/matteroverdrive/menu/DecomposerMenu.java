package matteroverdrive.menu;

import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiDecomposer: slot list on the left (input, battery), big output slot at (129, 55). */
public class DecomposerMenu extends MachineMenu<DecomposerBlockEntity> {
    public DecomposerMenu(int id, Inventory inventory, DecomposerBlockEntity machine, ContainerData data) {
        super(MOMenus.DECOMPOSER.get(), id, inventory, machine, data);
    }

    public DecomposerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (DecomposerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, DecomposerBlockEntity.INPUT, 8, 55);
        homeSlot(inv, machine.getBatterySlot(), 8, 82);
        homeSlot(inv, DecomposerBlockEntity.OUTPUT, 132, 58);
    }
}
