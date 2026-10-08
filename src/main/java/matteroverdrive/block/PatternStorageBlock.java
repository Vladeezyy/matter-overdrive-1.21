package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matternet.MatterNetworkBlock;

/** Pattern Storage (1.7.10 BlockPatternStorage), rendered with the original pattern_storage.obj. */
public class PatternStorageBlock extends MachineBlock implements MatterNetworkBlock {
    public static final MapCodec<PatternStorageBlock> CODEC = simpleCodec(PatternStorageBlock::new);

    public PatternStorageBlock(Properties properties) {
        super(MOBlockEntities.PATTERN_STORAGE, properties);
    }

    @Override
    protected MapCodec<PatternStorageBlock> codec() {
        return CODEC;
    }
}
