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

/** 1.7.10 BlockWeaponStation: a 9/16 high table that glows (light 10) and shows the weapon on it as a hologram. */
public class WeaponStationBlock extends MachineBlock {
    public static final MapCodec<WeaponStationBlock> CODEC = simpleCodec(WeaponStationBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

    public WeaponStationBlock(Properties properties) {
        super(MOBlockEntities.WEAPON_STATION, properties);
    }

    @Override
    protected MapCodec<WeaponStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
