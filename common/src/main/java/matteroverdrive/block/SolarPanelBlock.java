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

/** Half-height panel, as the 1.7.10 BlockSolarPanel bounds (0, 0, 0)-(1, 0.5, 1). */
public class SolarPanelBlock extends MachineBlock {
    public static final MapCodec<SolarPanelBlock> CODEC = simpleCodec(SolarPanelBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public SolarPanelBlock(Properties properties) {
        super(MOBlockEntities.SOLAR_PANEL, properties);
    }

    @Override
    protected MapCodec<SolarPanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
