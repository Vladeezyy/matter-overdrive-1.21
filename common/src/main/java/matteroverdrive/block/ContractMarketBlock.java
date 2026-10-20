package matteroverdrive.block;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.ContractMarketBlockEntity;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.7.10 BlockContractMarket (a BlockMonitor like the pattern monitor): a holo panel, hardness 20. */
public class ContractMarketBlock extends MachineBlock {
    public static final MapCodec<ContractMarketBlock> CODEC = simpleCodec(ContractMarketBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(0, 0, 11, 16, 16, 16), Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 5),
            Direction.WEST, Block.box(11, 0, 0, 16, 16, 16), Direction.EAST, Block.box(0, 0, 0, 5, 16, 16));

    public ContractMarketBlock(Properties properties) {
        super(MOBlockEntities.CONTRACT_MARKET, properties);
    }

    @Override
    protected MapCodec<ContractMarketBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** 1.7.10 onAdded: the first contract comes after the generation delay. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this) && level.getBlockEntity(pos) instanceof ContractMarketBlockEntity market) {
            market.addGenerationDelay();
        }
    }
}
