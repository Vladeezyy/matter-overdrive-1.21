package matteroverdrive.block.entity;

import java.util.Set;

import matteroverdrive.android.Android;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.ChargingStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.7.10 TileEntityMachineChargingStation: 512000 FE, takes 512 FE/t, and charges androids within 8 blocks (range
 * upgrades, up to x8) with up to 512 FE/t each, less the farther they stand.
 */
public class ChargingStationBlockEntity extends MachineBlockEntity {
    public static final int ENERGY_CAPACITY = 512000;
    public static final int ENERGY_TRANSFER = 512;
    public static final int BASE_MAX_RANGE = 8;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.CHARGING_STATION.get(), pos, state, MachineInventory.builder(), false, 2, ENERGY_CAPACITY,
                ENERGY_TRANSFER, ENERGY_TRANSFER, Set.of(UpgradeType.RANGE, UpgradeType.POWER_STORAGE, UpgradeType.POWER_USAGE));
    }

    public int getRange() {
        return (int) (BASE_MAX_RANGE * Math.min(8, getUpgradeMultiplier(UpgradeType.RANGE)));
    }

    public int getMaxCharging() {
        return (int) (ENERGY_TRANSFER / getUpgradeMultiplier(UpgradeType.POWER_USAGE));
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (energy.getEnergy() <= 0) return false;
        int range = getRange();
        boolean charged = false;
        for (Player player : getLevel().getEntitiesOfClass(Player.class, new AABB(getBlockPos()).inflate(range))) {
            if (!Android.isAndroid(player)) continue;
            double distance = player.position().distanceTo(getBlockPos().getCenter());
            int required = (int) (ENERGY_TRANSFER * (1 - Mth.clamp(distance / range, 0, 1)));
            int toGive = Math.min(required, Math.min(energy.getEnergy(), getMaxCharging()));
            int received = Android.receiveEnergy(player, toGive, false);
            if (received > 0) {
                energy.add(-received);
                charged = true;
            }
        }
        return charged;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChargingStationMenu(id, inventory, this, dataAccess);
    }
}
