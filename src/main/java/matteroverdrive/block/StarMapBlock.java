package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 BlockStarMap: a 9 px tall table (hardness 20, light 10); sneak-use zooms the hologram. */
public class StarMapBlock extends MachineBlock {
    public static final MapCodec<StarMapBlock> CODEC = simpleCodec(StarMapBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

    public StarMapBlock(Properties properties) {
        super(MOBlockEntities.STAR_MAP, properties);
    }

    @Override
    protected MapCodec<StarMapBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StarMapBlockEntity starMap) starMap.zoom();
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof StarMapBlockEntity starMap) {
            starMap.onPlaced(player);
        }
    }
}
