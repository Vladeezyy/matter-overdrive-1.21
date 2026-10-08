package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** Matter Analyzer (1.7.10 BlockMatterAnalyzer). */
public class AnalyzerBlock extends MachineBlock implements matteroverdrive.matternet.MatterNetworkBlock {
    public static final MapCodec<AnalyzerBlock> CODEC = simpleCodec(AnalyzerBlock::new);

    public AnalyzerBlock(Properties properties) {
        super(MOBlockEntities.ANALYZER, properties);
    }

    @Override
    protected MapCodec<AnalyzerBlock> codec() {
        return CODEC;
    }
}
