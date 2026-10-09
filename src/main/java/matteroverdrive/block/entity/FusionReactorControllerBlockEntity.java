package matteroverdrive.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.FusionReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import matteroverdrive.compat.EnergyHandlerUtil;

/**
 * 1.7.10 TileEntityMachineFusionReactorController. The reactor is a ring of 28 blocks behind the controller around
 * a centre 5 blocks back, where a gravitational anomaly must sit within 3 blocks vertically. Ring positions want a
 * machine hull (0), a fusion reactor coil (1), or either of those, an IO port or a decomposer (2).
 * Output: 2048 FE/t x efficiency (1 - anomaly distance / 4) x the anomaly's unsuppressed real mass; it burns
 * 1/80 kM of matter per tick times the same mass factor. Energy leaves through the controller and the IO ports.
 */
public class FusionReactorControllerBlockEntity extends MachineBlockEntity {
    public static final int STRUCTURE_CHECK_DELAY = 40;
    public static final int MAX_ANOMALY_DISTANCE = 3;
    public static final int ENERGY_STORAGE = 100_000_000;
    public static final int MATTER_STORAGE = 2048;
    public static final int ENERGY_PER_TICK = 2048;
    public static final double MATTER_DRAIN_PER_TICK = 1.0 / 80.0;

    /** (x, z) pairs relative to the controller with the ring to the south; 1.7.10 positions[]. */
    public static final int[] POSITIONS = {0, 5, 1, 0, 2, 0, 3, 1, 4, 2, 5, 3, 5, 4, 5, 5, 5, 6, 5, 7, 4, 8, 3, 9, 2, 10, 1, 10,
            0, 10, -1, 10, -2, 10, -3, 9, -4, 8, -5, 7, -5, 6, -5, 5, -5, 4, -5, 3, -4, 2, -3, 1, -2, 0, -1, 0};
    /** What each position wants: 255 anomaly centre, 0 hull, 1 coil, 2 coil/IO/hull/decomposer; 1.7.10 blocks[]. */
    public static final int[] BLOCKS = {255, 2, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 2};
    public static final int POSITION_COUNT = POSITIONS.length / 2;

    private boolean validStructure;
    private String status = "INVALID STRUCTURE";
    private float energyEfficiency;
    private int energyPerTick;
    private float matterPerTick;
    private float matterDrain;
    private @Nullable BlockPos anomalyPos;
    private final List<BlockPos> ioPorts = new ArrayList<>();

    public FusionReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.FUSION_REACTOR_CONTROLLER.get(), pos, state, MachineInventory.builder(), true, 4,
                ENERGY_STORAGE, 0, ENERGY_STORAGE, Set.of(UpgradeType.POWER_STORAGE, UpgradeType.RANGE, UpgradeType.SPEED));
        initMatter(MATTER_STORAGE, MATTER_STORAGE, 0);
    }

    /** Position i of the ring in world coordinates (1.7.10 getPosition, rotated by the controller's back side). */
    public BlockPos getPosition(int i) {
        int x = POSITIONS[i * 2], z = POSITIONS[i * 2 + 1];
        Direction back = getBlockState().getValue(MachineBlock.FACING).getOpposite();
        int rx, rz;
        switch (back) {
            case NORTH -> { rx = -x; rz = -z; }
            case WEST -> { rx = -z; rz = x; }
            case EAST -> { rx = z; rz = -x; }
            default -> { rx = x; rz = z; }
        }
        return getBlockPos().offset(rx, 0, rz);
    }

    @Override
    protected boolean batteryChargesItem() {
        return true;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        Level level = getLevel();
        if (level.getGameTime() % STRUCTURE_CHECK_DELAY == 0) {
            checkStructure(level);
        }
        boolean generating = validStructure && redstoneAllows && isGeneratingPower();
        if (generating) {
            int before = energy.getEnergy();
            energy.add(energyPerTick);
            int received = energy.getEnergy() - before;
            if (received > 0) {
                matterDrain += matterPerTick * ((float) received / energyPerTick);
                if (matterDrain >= 1) {
                    matter.add(-(int) matterDrain);
                    matterDrain -= (int) matterDrain;
                }
            }
        }
        pushEnergy(level, getBlockPos());
        for (BlockPos io : ioPorts) pushEnergy(level, io);
        return generating;
    }

    public boolean isGeneratingPower() {
        return energyEfficiency > 0 && energy.getEnergy() < energy.getCapacity() && matter.getMatter() > matterPerTick;
    }

    private void pushEnergy(Level level, BlockPos from) {
        for (Direction dir : Direction.values()) {
            if (energy.getEnergy() <= 0) return;
            BlockPos to = from.relative(dir);
            if (to.equals(getBlockPos()) || ioPorts.contains(to)) continue;
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, to, dir.getOpposite());
            if (target != null) EnergyHandlerUtil.move(energy, target, energy.getEnergy());
        }
    }

    private void checkStructure(Level level) {
        boolean valid = true;
        String info = status;
        float efficiency = 0;
        List<BlockPos> ios = new ArrayList<>();
        BlockPos anomaly = null;
        for (int i = 0; i < POSITION_COUNT; i++) {
            BlockPos p = getPosition(i);
            if (BLOCKS[i] == 255) {
                anomaly = findAnomaly(level, p);
                if (anomaly == null) {
                    valid = false;
                    info = "NO\nGRAVITATIONAL\nANOMALY";
                    break;
                }
                int distance = Math.abs(anomaly.getY() - p.getY());
                efficiency = 1f - distance / (float) (MAX_ANOMALY_DISTANCE + 1);
                continue;
            }
            BlockState state = level.getBlockState(p);
            if (state.isAir()) {
                valid = false;
                info = "INVALID\nSTRUCTURE";
                break;
            } else if (state.is(MOBlocks.MACHINE_HULL.get())) {
                if (BLOCKS[i] == 1) {
                    valid = false;
                    info = "NEED\nMORE\nCOILS";
                    break;
                }
            } else if (state.is(MOBlocks.FUSION_REACTOR_COIL.get()) || state.is(MOBlocks.FUSION_REACTOR_IO.get())) {
                if (BLOCKS[i] == 0) {
                    valid = false;
                    info = "INVALID\nMATERIALS";
                    break;
                }
                if (state.is(MOBlocks.FUSION_REACTOR_IO.get())) ios.add(p);
            } else if (!(state.is(MOBlocks.DECOMPOSER.get()) && BLOCKS[i] == 2)) {
                valid = false;
                info = "INVALID\nMATERIALS";
                break;
            }
        }
        anomalyPos = valid ? anomaly : null;
        double massFactor = anomalyMassFactor(level);
        if (valid) {
            energyPerTick = (int) Math.round(ENERGY_PER_TICK * efficiency * massFactor);
            matterPerTick = (float) (MATTER_DRAIN_PER_TICK * massFactor);
            info = "POWER " + Math.round(efficiency * 100) + "%";
        } else {
            efficiency = 0;
            energyPerTick = 0;
        }
        for (BlockPos io : ioPorts) {
            if (!ios.contains(io) && level.getBlockEntity(io) instanceof FusionReactorIOBlockEntity port) port.setController(null);
        }
        ioPorts.clear();
        ioPorts.addAll(ios);
        for (BlockPos io : ioPorts) {
            if (level.getBlockEntity(io) instanceof FusionReactorIOBlockEntity port) port.setController(getBlockPos());
        }
        if (valid != validStructure || !info.equals(status) || efficiency != energyEfficiency) {
            validStructure = valid;
            status = info;
            energyEfficiency = efficiency;
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    private static @Nullable BlockPos findAnomaly(Level level, BlockPos centre) {
        for (int dy = -MAX_ANOMALY_DISTANCE; dy <= MAX_ANOMALY_DISTANCE; dy++) {
            BlockPos p = centre.above(dy);
            if (level.getBlockState(p).is(MOBlocks.GRAVITATIONAL_ANOMALY.get())) return p;
        }
        return null;
    }

    private double anomalyMassFactor(Level level) {
        if (anomalyPos != null && level.getBlockEntity(anomalyPos) instanceof GravitationalAnomalyBlockEntity anomaly) {
            return anomaly.getRealMassUnsuppressed();
        }
        return 0;
    }

    public boolean isValidStructure() {
        return validStructure;
    }

    public String getStatus() {
        return status;
    }

    public float getEnergyEfficiency() {
        return energyEfficiency;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public float getMatterPerTick() {
        return matterPerTick;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("valid_structure", validStructure);
        output.putString("status", status);
        output.putFloat("efficiency", energyEfficiency);
        output.putInt("energy_per_tick", energyPerTick);
        output.putFloat("matter_per_tick", matterPerTick);
        output.putFloat("matter_drain", matterDrain);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        validStructure = input.getBooleanOr("valid_structure", false);
        status = input.getStringOr("status", "INVALID STRUCTURE");
        energyEfficiency = input.getFloatOr("efficiency", 0);
        energyPerTick = input.getIntOr("energy_per_tick", 0);
        matterPerTick = input.getFloatOr("matter_per_tick", 0);
        matterDrain = input.getFloatOr("matter_drain", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FusionReactorMenu(id, inventory, this, dataAccess);
    }
}
