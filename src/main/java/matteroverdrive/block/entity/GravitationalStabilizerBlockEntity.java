package matteroverdrive.block.entity;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.block.GravitationalAnomalyBlock;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.util.MOMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7.10 TileEntityMachineGravitationalStabilizer: casts a beam up to 64 blocks forward; an anomaly it hits is
 * suppressed to 0.7 of its mass for as long as the beam stays on (each hit lasts 20 ticks). Redstone controlled,
 * uses no energy.
 */
public class GravitationalStabilizerBlockEntity extends MachineBlockEntity {
    public static final int RANGE = 64;
    public static final int SUPPRESS_TIME = 20;
    public static final float SUPPRESS_AMOUNT = 0.7f;

    public GravitationalStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.GRAVITATIONAL_STABILIZER.get(), pos, state, MachineInventory.builder(), false, 0, 512, 512, 512, Set.of());
    }

    /** The first anomaly along the beam, or null if an opaque block or the range ends it first. */
    public static @Nullable BlockPos findAnomaly(Level level, BlockPos pos, Direction facing) {
        for (int i = 1; i < RANGE; i++) {
            BlockPos p = pos.relative(facing, i);
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof GravitationalAnomalyBlock) return p;
            if (state.canOcclude()) return null;
        }
        return null;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (!redstoneAllows) return false;
        BlockPos hit = findAnomaly(getLevel(), getBlockPos(), getBlockState().getValue(MachineBlock.FACING));
        if (hit != null && getLevel().getBlockEntity(hit) instanceof GravitationalAnomalyBlockEntity anomaly) {
            anomaly.suppress(new GravitationalAnomalyBlockEntity.Suppressor(getBlockPos(), SUPPRESS_TIME, SUPPRESS_AMOUNT));
            return true;
        }
        return false;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getLoopSound() {
        return matteroverdrive.init.MOSounds.FORCE_FIELD.get();
    }

    /** 1.7.10: half the brightest channel of the beam colour, which drifts with the time of day. */
    @Override
    public float getLoopVolume() {
        return (float) Math.max(Math.max(getBeamColorR(), getBeamColorG()), getBeamColorB()) * 0.5f;
    }

    public double getBeamColorR() {
        return MOMath.noise(0, getBlockPos().getY(), getLevel().getDayTime() * 0.01);
    }

    public double getBeamColorG() {
        return MOMath.noise(getBlockPos().getX(), 0, getLevel().getDayTime() * 0.01);
    }

    public double getBeamColorB() {
        return MOMath.noise(0, 0, getBlockPos().getZ() + getLevel().getDayTime() * 0.01);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;   // no GUI in 1.7.10
    }
}
