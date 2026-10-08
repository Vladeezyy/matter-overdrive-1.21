package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.GravitationalStabilizerBlockEntity;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Gravitational Stabilizer (1.7.10 BlockGravitationalStabilizer): no GUI; while active its beam shows as particles. */
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        Direction facing = state.getValue(FACING);
        BlockPos target = GravitationalStabilizerBlockEntity.findAnomaly(level, pos, facing);
        if (target == null) return;
        double length = Math.sqrt(pos.distSqr(target));
        for (int i = 0; i < 3; i++) {
            double t = random.nextDouble() * length;
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + facing.getStepX() * t, pos.getY() + 0.5 + facing.getStepY() * t,
                    pos.getZ() + 0.5 + facing.getStepZ() * t, 0, 0, 0);
        }
    }
}
