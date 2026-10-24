package matteroverdrive.block;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** 1.7.10 BlockAndroidSpawner: an unbreakable machine block (hardness -1) for map makers, no recipe. */
public class AndroidSpawnerBlock extends MachineBlock {

    public AndroidSpawnerBlock(Properties properties) {
        super(MOBlockEntities.ANDROID_SPAWNER, properties);
    }

}
