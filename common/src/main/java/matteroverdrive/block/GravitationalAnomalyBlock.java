package matteroverdrive.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.init.MOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 BlockGravitationalAnomaly: unbreakable, no collision, a black sphere you can only target. */
public class GravitationalAnomalyBlock extends BaseEntityBlock {
    public static final MapCodec<GravitationalAnomalyBlock> CODEC = simpleCodec(GravitationalAnomalyBlock::new);
    private static final VoxelShape OUTLINE = Block.box(4, 4, 4, 12, 12, 12);

    public GravitationalAnomalyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GravitationalAnomalyBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;     // drawn by AnomalyRenderer
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravitationalAnomalyBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MOBlockEntities.GRAVITATIONAL_ANOMALY.get(),
                level.isClientSide() ? GravitationalAnomalyBlockEntity::clientTick : GravitationalAnomalyBlockEntity::serverTick);
    }
}
