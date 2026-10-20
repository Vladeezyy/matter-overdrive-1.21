package matteroverdrive.block;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** 1.7.10 BlockTransporter: hardness 20, keeps energy and destinations when broken. */
public class TransporterBlock extends MachineBlock {

    public TransporterBlock(Properties properties) {
        super(MOBlockEntities.TRANSPORTER, properties);
    }

}
