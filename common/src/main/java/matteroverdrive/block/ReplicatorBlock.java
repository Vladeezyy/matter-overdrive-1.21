package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matternet.MatterNetworkBlock;

/** Replicator (1.7.10 BlockReplicator), rendered with the original replicator.obj. */
public class ReplicatorBlock extends MachineBlock implements MatterNetworkBlock {
    public static final MapCodec<ReplicatorBlock> CODEC = simpleCodec(ReplicatorBlock::new);

    public ReplicatorBlock(Properties properties) {
        super(MOBlockEntities.REPLICATOR, properties);
    }

    @Override
    protected MapCodec<ReplicatorBlock> codec() {
        return CODEC;
    }
}
