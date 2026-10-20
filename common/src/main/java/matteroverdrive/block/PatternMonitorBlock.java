package matteroverdrive.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matternet.MatterNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 BlockPatternMonitor: a 5-pixel holo panel on the back of its block, screen facing the player. */
public class PatternMonitorBlock extends MachineBlock implements MatterNetworkBlock {
    public static final MapCodec<PatternMonitorBlock> CODEC = simpleCodec(PatternMonitorBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(0, 0, 11, 16, 16, 16), Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 5),
            Direction.WEST, Block.box(11, 0, 0, 16, 16, 16), Direction.EAST, Block.box(0, 0, 0, 5, 16, 16));

    public PatternMonitorBlock(Properties properties) {
        super(MOBlockEntities.PATTERN_MONITOR, properties);
    }

    @Override
    protected MapCodec<PatternMonitorBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
