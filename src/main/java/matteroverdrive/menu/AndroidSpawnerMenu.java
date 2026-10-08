package matteroverdrive.menu;

import matteroverdrive.block.entity.AndroidSpawnerBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/** 1.7.10 ContainerAndroidSpawner: five flash drive slots in a row, the colour module, the spawned count, "Kill All". */
public class AndroidSpawnerMenu extends MachineMenu<AndroidSpawnerBlockEntity> {
    public static final int BUTTON_KILL_ALL = 1;
    private final ContainerData data;

    public AndroidSpawnerMenu(int id, Inventory inventory, AndroidSpawnerBlockEntity machine, ContainerData data) {
        super(MOMenus.ANDROID_SPAWNER.get(), id, inventory, machine, data);
        this.data = data;
    }

    public AndroidSpawnerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (AndroidSpawnerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(MachineBlockEntity.DATA_COUNT + 1));
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, AndroidSpawnerBlockEntity.COLOR_MODULE_SLOT, 8, 55);
        for (int i = 0; i < AndroidSpawnerBlockEntity.FLASH_DRIVE_COUNT; i++) {
            homeSlot(inv, AndroidSpawnerBlockEntity.FLASH_DRIVE_SLOT_START + i, 63 + 24 * i, 35);
        }
    }

    public int getSpawnedCount() {
        return data.get(MachineBlockEntity.DATA_COUNT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_KILL_ALL) {
            machine.removeAllAndroids();
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
