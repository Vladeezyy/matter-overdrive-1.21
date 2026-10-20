package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 BlockChargingStation: a three-block-high pylon (the base holds the machine, the two blocks above were
 * invisible bounding blocks). Breaking any part breaks the whole station.
 */
public class ChargingStationBlock extends MachineBlock {
    public static final MapCodec<ChargingStationBlock> CODEC = simpleCodec(ChargingStationBlock::new);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape ROD = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape TOP = Block.box(5, 0, 5, 11, 5, 11);

    public ChargingStationBlock(Properties properties) {
        super(MOBlockEntities.CHARGING_STATION, properties);
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override
    protected MapCodec<ChargingStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case 0 -> BASE;
            case 1 -> ROD;
            default -> TOP;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == 0 ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    /** 1.7.10 canPlaceBlockAt: the two blocks above must be free. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() + 2 > level.getMaxY()) return null;
        for (int i = 1; i <= 2; i++) {
            if (!level.getBlockState(pos.above(i)).canBeReplaced(context)) return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        for (int i = 1; i <= 2; i++) {
            level.setBlock(pos.above(i), state.setValue(PART, i), Block.UPDATE_ALL);
        }
    }

    /** Any part without the rest of the station disappears (like a door half). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        int part = state.getValue(PART);
        boolean below = direction == Direction.DOWN && part > 0, above = direction == Direction.UP && part < 2;
        if ((below || above) && !(neighbor.is(this) && neighbor.getValue(PART) == part + (above ? 1 : -1))) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    /** Breaking an upper part breaks the base properly (it holds the machine and drops the item). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        int part = state.getValue(PART);
        if (part > 0 && !level.isClientSide()) {
            BlockPos base = pos.below(part);
            if (level.getBlockState(base).is(this)) {
                if (player.isCreative()) {
                    level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                } else {
                    level.destroyBlock(base, true, player);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? super.newBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(PART) == 0 ? super.getTicker(level, state, type) : null;
    }

    /** The upper parts open the base's GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos base = pos.below(state.getValue(PART));
        return super.useWithoutItem(level.getBlockState(base), level, base, player, hit);
    }
}
