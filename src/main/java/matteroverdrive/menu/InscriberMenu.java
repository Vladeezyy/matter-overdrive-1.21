package matteroverdrive.menu;

import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class InscriberMenu extends MachineMenu<InscriberBlockEntity> {
    // Positions from the 1.7.10 GuiInscriber / ElementSlotsList (big slots 22x22, item inset by 3).
    public static final int MAIN_X = 8, MAIN_Y = 55;
    public static final int SECONDARY_X = 8, SECONDARY_Y = 82;
    public static final int BATTERY_X = 8, BATTERY_Y = 109;
    public static final int OUTPUT_X = 132, OUTPUT_Y = 58;

    public InscriberMenu(int id, Inventory inventory, InscriberBlockEntity machine, ContainerData data) {
        super(MOMenus.INSCRIBER.get(), id, inventory, machine, data);
    }

    public InscriberMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (InscriberBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, InscriberBlockEntity.MAIN, MAIN_X, MAIN_Y);
        homeSlot(inv, InscriberBlockEntity.SECONDARY, SECONDARY_X, SECONDARY_Y);
        homeSlot(inv, InscriberBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y);
        homeSlot(inv, machine.getBatterySlot(), BATTERY_X, BATTERY_Y);
    }
}
