package matteroverdrive.block;


import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlock;

/** Matter Analyzer (1.7.10 BlockMatterAnalyzer). */
public class AnalyzerBlock extends MachineBlock implements matteroverdrive.matternet.MatterNetworkBlock {

    public AnalyzerBlock(Properties properties) {
        super(MOBlockEntities.ANALYZER, properties);
    }

}
