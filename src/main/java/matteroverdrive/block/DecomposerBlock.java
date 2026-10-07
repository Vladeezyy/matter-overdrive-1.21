package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** Matter Decomposer (1.7.10 BlockDecomposer). */
public class DecomposerBlock extends MachineBlock {
    public static final MapCodec<DecomposerBlock> CODEC = simpleCodec(DecomposerBlock::new);

    public DecomposerBlock(Properties properties) {
        super(MOBlockEntities.DECOMPOSER, properties);
    }

    @Override
    protected MapCodec<DecomposerBlock> codec() {
        return CODEC;
    }
}
