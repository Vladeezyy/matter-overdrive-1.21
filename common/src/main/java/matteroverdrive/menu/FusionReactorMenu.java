package matteroverdrive.menu;

import matteroverdrive.block.entity.FusionReactorControllerBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiFusionReactor: the battery slot it charges; readouts in the middle. */
public class FusionReactorMenu extends MachineMenu<FusionReactorControllerBlockEntity> {
    public FusionReactorMenu(int id, Inventory inventory, FusionReactorControllerBlockEntity machine, ContainerData data) {
        super(MOMenus.FUSION_REACTOR.get(), id, inventory, machine, data);
    }

    public FusionReactorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (FusionReactorControllerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, machine.getBatterySlot(), 8, 55);
    }
}
