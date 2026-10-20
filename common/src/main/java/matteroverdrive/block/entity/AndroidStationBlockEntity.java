package matteroverdrive.block.entity;

import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.menu.AndroidStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 TileEntityAndroidStation: no work of its own; its GUI edits the android player's parts and stats. */
public class AndroidStationBlockEntity extends MachineBlockEntity {
    public AndroidStationBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.ANDROID_STATION.get(), pos, state, MachineInventory.builder(), false, 0, 0, 0, 0, Set.of());
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AndroidStationMenu(id, inventory, this, dataAccess);
    }
}
