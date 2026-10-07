package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Rendered with the original inscriber.obj; the model is 0.66 blocks tall. */
public class InscriberBlock extends MachineBlock {
    public static final MapCodec<InscriberBlock> CODEC = simpleCodec(InscriberBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11, 16);

    public InscriberBlock(Properties properties) {
        super(MOBlockEntities.INSCRIBER, properties);
    }

    @Override
    protected MapCodec<InscriberBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
