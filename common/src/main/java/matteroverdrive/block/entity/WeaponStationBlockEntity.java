package matteroverdrive.block.entity;

import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.menu.WeaponStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7.10 TileEntityWeaponStation: holds one weapon; its module slots are the weapon's own (the menu edits the
 * weapon's {@code weapon_modules} component). No energy, no upgrades.
 */
public class WeaponStationBlockEntity extends MachineBlockEntity {
    public static final int WEAPON = 0;

    public WeaponStationBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.WEAPON_STATION.get(), pos, state, slots(), false, 0, 0, 0, 0, Set.of());
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.INPUT, r -> r.getItem() instanceof EnergyWeaponItem, 1);
        return b;
    }

    public ItemStack getWeapon() {
        return getInventory().getStack(WEAPON);
    }

    /** Clients render the weapon as a hologram, so they need every change. */
    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeaponStationMenu(id, inventory, this, dataAccess);
    }
}
