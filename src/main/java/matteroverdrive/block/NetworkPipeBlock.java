package matteroverdrive.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import matteroverdrive.matternet.MatterNetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 network_pipe: links matter network machines. It carries no matter, only connectivity. */
public class NetworkPipeBlock extends Block implements MatterNetworkBlock {
    public static final MapCodec<NetworkPipeBlock> CODEC = simpleCodec(NetworkPipeBlock::new);
    private static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    // same 1/3-block core and arms as the matter pipe (1.7.10 drew both with RendererBlockPipe)
    private static final double A = 16 / 3.0, B = 32 / 3.0;
    private static final VoxelShape CORE = Block.box(A, A, A, B, B, B);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(A, A, 0, B, B, A), Direction.SOUTH, Block.box(A, A, B, B, B, 16),
            Direction.WEST, Block.box(0, A, A, A, B, B), Direction.EAST, Block.box(B, A, A, 16, B, B),
            Direction.DOWN, Block.box(A, 0, A, B, A, B), Direction.UP, Block.box(A, B, A, B, 16, B));

    public NetworkPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<NetworkPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(SIDES.get(dir), connects(context.getLevel(), context.getClickedPos(), dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(SIDES.get(dir), neighbor.getBlock() instanceof MatterNetworkBlock);
    }

    private static boolean connects(LevelReader level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).getBlock() instanceof MatterNetworkBlock;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(SIDES.get(dir))) shape = Shapes.or(shape, ARMS.get(dir));
        }
        return shape;
    }
}
