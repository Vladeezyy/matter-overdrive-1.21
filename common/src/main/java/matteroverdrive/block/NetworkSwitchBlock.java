package matteroverdrive.block;


import matteroverdrive.matternet.MatterNetworkBlock;
import net.minecraft.world.level.block.Block;

/**
 * 1.7.10 network_switch: there it filtered and forwarded network packets. The port treats the network as a shared
 * bus, so the switch is a connector block; its flash-drive filters are not ported yet.
 */
public class NetworkSwitchBlock extends Block implements MatterNetworkBlock {

    public NetworkSwitchBlock(Properties properties) {
        super(properties);
    }

}
