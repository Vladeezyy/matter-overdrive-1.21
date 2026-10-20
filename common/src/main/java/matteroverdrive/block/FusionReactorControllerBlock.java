package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** Fusion Reactor Controller (1.7.10 BlockFusionReactorController); faces away from the ring. */
public class FusionReactorControllerBlock extends MachineBlock {
    public static final MapCodec<FusionReactorControllerBlock> CODEC = simpleCodec(FusionReactorControllerBlock::new);

    public FusionReactorControllerBlock(Properties properties) {
        super(MOBlockEntities.FUSION_REACTOR_CONTROLLER, properties);
    }

    @Override
    protected MapCodec<FusionReactorControllerBlock> codec() {
        return CODEC;
    }
}
