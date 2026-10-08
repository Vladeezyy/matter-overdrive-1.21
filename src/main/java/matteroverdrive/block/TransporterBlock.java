package matteroverdrive.block;

import com.mojang.serialization.MapCodec;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** 1.7.10 BlockTransporter: hardness 20, keeps energy and destinations when broken. */
public class TransporterBlock extends MachineBlock {
    public static final MapCodec<TransporterBlock> CODEC = simpleCodec(TransporterBlock::new);

    public TransporterBlock(Properties properties) {
        super(MOBlockEntities.TRANSPORTER, properties);
    }

    @Override
    protected MapCodec<TransporterBlock> codec() {
        return CODEC;
    }
}
