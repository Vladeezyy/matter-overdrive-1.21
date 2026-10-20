package matteroverdrive.block;


import matteroverdrive.matternet.MatterNetworkBlock;
import net.minecraft.world.level.block.Block;

/**
 * 1.7.10 network_router: there it filtered and forwarded network packets. The port treats the network as a shared
 * bus, so the router is a connector block; its flash-drive filters are not ported yet.
 */
public class NetworkRouterBlock extends Block implements MatterNetworkBlock {

    public NetworkRouterBlock(Properties properties) {
        super(properties);
    }

}
