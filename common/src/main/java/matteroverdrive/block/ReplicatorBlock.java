package matteroverdrive.block;


import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matternet.MatterNetworkBlock;

/** Replicator (1.7.10 BlockReplicator), rendered with the original replicator.obj. */
public class ReplicatorBlock extends MachineBlock implements MatterNetworkBlock {

    public ReplicatorBlock(Properties properties) {
        super(MOBlockEntities.REPLICATOR, properties);
    }

}
