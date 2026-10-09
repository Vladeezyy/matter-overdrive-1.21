package matteroverdrive.block.entity;

import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.SolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import matteroverdrive.compat.EnergyHandlerUtil;

/** 1.7.10 TileEntityMachineSolarPanel: up to 16 FE/t in full daylight, pushes up to 512 FE/t to each neighbour. */
public class SolarPanelBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_AMOUNT = 16;
    public static final int ENERGY_STORAGE = 64000;
    public static final int MAX_ENERGY_EXTRACT = 512;

    private int chargeAmount;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.SOLAR_PANEL.get(), pos, state, MachineInventory.builder(), false, 2,
                ENERGY_STORAGE, 0, MAX_ENERGY_EXTRACT, Set.of(UpgradeType.POWER_STORAGE));
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        Level level = getLevel();
        float time = 0;
        if (level.dimensionType().hasSkyLight()
                && level.getBrightness(LightLayer.SKY, getBlockPos()) - level.getSkyDarken() >= 15) {
            time = getTime(level);
        }
        chargeAmount = Math.round(CHARGE_AMOUNT * Math.max(0, time));
        boolean active = time > 0.5f;
        if (active && chargeAmount > 0) {
            energy.add(chargeAmount);
        }
        pushEnergy(level);
        return active;
    }

    /** 1.7.10 getTime(): cosine of the sun angle, flattened towards noon. */
    private static float getTime(Level level) {
        float f = level.getSunAngle(1f);
        if (f < (float) Math.PI) {
            f += (0f - f) * 0.2f;
        } else {
            f += ((float) Math.PI * 2f - f) * 0.2f;
        }
        return Mth.cos(f);
    }

    private void pushEnergy(Level level) {
        for (Direction dir : Direction.values()) {
            if (energy.getEnergy() <= 0) return;
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(dir), dir.getOpposite());
            if (target != null) {
                EnergyHandlerUtil.move(energy, target, MAX_ENERGY_EXTRACT);
            }
        }
    }

    public int getChargeAmount() {
        return chargeAmount;
    }

    /** Shown in the GUI as the generation rate (charge / 16). */
    @Override
    public float getProgress() {
        return chargeAmount / (float) CHARGE_AMOUNT;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SolarPanelMenu(id, inventory, this, dataAccess);
    }
}
