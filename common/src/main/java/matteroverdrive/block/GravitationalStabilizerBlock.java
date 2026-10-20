package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Gravitational Stabilizer (1.7.10 BlockGravitationalStabilizer): no GUI; the beam, motes and screen are drawn by client/StabilizerRenderer. */
public class GravitationalStabilizerBlock extends MachineBlock {
    public static final MapCodec<GravitationalStabilizerBlock> CODEC = simpleCodec(GravitationalStabilizerBlock::new);

    public GravitationalStabilizerBlock(Properties properties) {
        super(MOBlockEntities.GRAVITATIONAL_STABILIZER, properties);
    }

    @Override
    protected MapCodec<GravitationalStabilizerBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
