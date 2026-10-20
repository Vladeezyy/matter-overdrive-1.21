package matteroverdrive.block;

import com.mojang.serialization.MapCodec;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** 1.7.10 BlockAndroidSpawner: an unbreakable machine block (hardness -1) for map makers, no recipe. */
public class AndroidSpawnerBlock extends MachineBlock {
    public static final MapCodec<AndroidSpawnerBlock> CODEC = simpleCodec(AndroidSpawnerBlock::new);

    public AndroidSpawnerBlock(Properties properties) {
        super(MOBlockEntities.ANDROID_SPAWNER, properties);
    }

    @Override
    protected MapCodec<AndroidSpawnerBlock> codec() {
        return CODEC;
    }
}
