package matteroverdrive.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.FusionReactorIOBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 BlockFusionReactorIO: a ring block that passes energy and matter to and from the controller. */
public class FusionReactorIOBlock extends BaseEntityBlock {
    public static final MapCodec<FusionReactorIOBlock> CODEC = simpleCodec(FusionReactorIOBlock::new);

    public FusionReactorIOBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FusionReactorIOBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorIOBlockEntity(pos, state);
    }
}
