package matteroverdrive.block.entity;

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
    private final int phase;
    private final Direction[] order = Direction.values();
    /** Neighbouring matter tanks (pipes and machines), looked up once per side. */
    private final matteroverdrive.util.NeighbourCaps<net.neoforged.neoforge.fluids.capability.IFluidHandler> targets =
            new matteroverdrive.util.NeighbourCaps<>(Capabilities.FluidHandler.BLOCK, this);

    public MatterPipeBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.MATTER_PIPE.get(), pos, state);
        boolean heavy = state.getBlock() instanceof MatterPipeBlock pipe && pipe.isHeavy();
        int capacity = heavy ? 128 : 32;
        this.tank = new MatterTank(capacity, capacity, capacity, t -> 1, this::setChanged);
        this.interval = heavy ? 5 : 10;
        this.phase = (int) Math.floorMod(pos.asLong(), (long) interval);
    }

    public MatterTank getTank() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MatterPipeBlockEntity pipe) {
        // spread over the period by position: a pipe network no longer moves all its matter on the same tick
        if (pipe.tank.getMatter() <= 0 || (level.getGameTime() + pipe.phase) % pipe.interval != 0) return;
        Direction[] dirs = pipe.order;
        // shuffled in place: neighbours take turns being offered first (no list and random allocated per transfer)
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = level.getRandom().nextInt(i + 1);
            Direction t = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = t;
        }
        // consumers first
        for (Direction dir : dirs) {
            if (pipe.tank.getMatter() <= 0) return;
            if (pipe.neighbourPipe(level, pos, dir) != null) continue;
            pipe.offer(dir, pipe.tank.getMatter());
        }
        // then spread to emptier pipes
        for (Direction dir : dirs) {
            if (pipe.tank.getMatter() <= 0) return;
            MatterPipeBlockEntity next = pipe.neighbourPipe(level, pos, dir);
            if (next != null) {
                int diff = pipe.tank.getMatter() - next.tank.getMatter();
                if (diff > 1) pipe.offer(dir, diff / 2);
            }
        }
    }

    private @org.jetbrains.annotations.Nullable MatterPipeBlockEntity neighbourPipe(Level level, BlockPos pos, Direction dir) {
        // only loaded neighbours: a pipe at the edge of the loaded area must not load the next chunk
        BlockPos other = pos.relative(dir);
        return level.isLoaded(other) && level.getBlockEntity(other) instanceof MatterPipeBlockEntity next ? next : null;
    }

    private void offer(Direction dir, int amount) {
        if (amount <= 0) return;
        var target = targets.get(dir);
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
