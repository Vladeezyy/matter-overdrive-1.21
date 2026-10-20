package matteroverdrive.menu;

import matteroverdrive.block.entity.RecyclerBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiRecycler: slot list on the left (input, battery), big output slot at (64, 52). */
public class RecyclerMenu extends MachineMenu<RecyclerBlockEntity> {
    public RecyclerMenu(int id, Inventory inventory, RecyclerBlockEntity machine, ContainerData data) {
        super(MOMenus.RECYCLER.get(), id, inventory, machine, data);
    }

    public RecyclerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (RecyclerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, RecyclerBlockEntity.INPUT, 8, 55);
        homeSlot(inv, machine.getBatterySlot(), 8, 82);
        homeSlot(inv, RecyclerBlockEntity.OUTPUT, 67, 55);
    }
}
