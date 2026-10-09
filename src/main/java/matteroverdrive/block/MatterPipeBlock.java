package matteroverdrive.block;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.MatterPipeBlockEntity;
import matteroverdrive.init.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * 1.7.10 BlockMatterPipe / heavy_matter_pipe: carries Matter Plasma between machines. Connects to other matter
 * pipes and to anything that exposes a fluid handler on that side.
 */
public class MatterPipeBlock extends BaseEntityBlock {
    public static final MapCodec<MatterPipeBlock> CODEC = simpleCodec(p -> new MatterPipeBlock(false, p));
    private static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    // 1.7.10 RendererBlockPipe: a core and arms, each a cube of 1/3 block.
    private static final double A = 16 / 3.0, B = 32 / 3.0;
    private static final VoxelShape CORE = Block.box(A, A, A, B, B, B);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(A, A, 0, B, B, A), Direction.SOUTH, Block.box(A, A, B, B, B, 16),
            Direction.WEST, Block.box(0, A, A, A, B, B), Direction.EAST, Block.box(B, A, A, 16, B, B),
            Direction.DOWN, Block.box(A, 0, A, B, A, B), Direction.UP, Block.box(A, B, A, B, 16, B));

    private final boolean heavy;

    public MatterPipeBlock(boolean heavy, Properties properties) {
        super(properties);
        this.heavy = heavy;
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    /** Heavy pipes hold 128 matter and move it every 5 ticks; normal ones 32 every 10 (1.7.10). */
    public boolean isHeavy() {
        return heavy;
    }

    @Override
    protected MapCodec<MatterPipeBlock> codec() {
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
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(SIDES.get(dir), connects(level, pos, dir));
    }

    private static boolean connects(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (level.getBlockState(other).getBlock() instanceof MatterPipeBlock) return true;
        return level instanceof Level l && l.getCapability(Capabilities.FluidHandler.BLOCK, other, dir.getOpposite()) != null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(SIDES.get(dir))) shape = Shapes.or(shape, ARMS.get(dir));
        }
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, MOBlockEntities.MATTER_PIPE.get(), MatterPipeBlockEntity::serverTick);
    }
}
