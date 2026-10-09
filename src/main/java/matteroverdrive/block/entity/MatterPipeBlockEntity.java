package matteroverdrive.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import matteroverdrive.block.MatterPipeBlock;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOFluids;
import matteroverdrive.machine.MatterTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * A matter pipe segment. Every transfer period it offers its matter to adjacent non-pipe tanks first (machines
 * that accept matter), then evens out with neighbouring pipes that hold less, so matter flows from sources to
 * consumers instead of sloshing back and forth.
 */
public class MatterPipeBlockEntity extends BlockEntity {
    private final MatterTank tank;
    private final int interval;

    public MatterPipeBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.MATTER_PIPE.get(), pos, state);
        boolean heavy = state.getBlock() instanceof MatterPipeBlock pipe && pipe.isHeavy();
        int capacity = heavy ? 128 : 32;
        this.tank = new MatterTank(capacity, capacity, capacity, t -> 1, this::setChanged);
        this.interval = heavy ? 5 : 10;
    }

    public MatterTank getTank() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MatterPipeBlockEntity pipe) {
        if (pipe.tank.getMatter() <= 0 || level.getGameTime() % pipe.interval != 0) return;
        List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(dirs, new java.util.Random(level.getRandom().nextLong()));
        // consumers first
        for (Direction dir : dirs) {
            BlockPos other = pos.relative(dir);
            if (level.getBlockEntity(other) instanceof MatterPipeBlockEntity) continue;
            pipe.offer(level, other, dir, pipe.tank.getMatter());
        }
        // then spread to emptier pipes
        for (Direction dir : dirs) {
            if (pipe.tank.getMatter() <= 0) return;
            if (level.getBlockEntity(pos.relative(dir)) instanceof MatterPipeBlockEntity next) {
                int diff = pipe.tank.getMatter() - next.tank.getMatter();
                if (diff > 1) pipe.offer(level, pos.relative(dir), dir, diff / 2);
            }
        }
    }

    private void offer(Level level, BlockPos other, Direction dir, int amount) {
        if (amount <= 0) return;
        var target = level.getCapability(Capabilities.FluidHandler.BLOCK, other, dir.getOpposite());
        if (target == null) return;
        int moved = target.fill(new net.neoforged.neoforge.fluids.FluidStack(MOFluids.MATTER_PLASMA.get(), amount), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (moved > 0) tank.add(-moved);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("matter"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("matter").ifPresent(tank::deserialize);
    }
}
