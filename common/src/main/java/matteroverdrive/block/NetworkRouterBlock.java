package matteroverdrive.block;

import com.mojang.serialization.MapCodec;

import matteroverdrive.matternet.MatterNetworkBlock;
import net.minecraft.world.level.block.Block;

/**
 * 1.7.10 network_router: there it filtered and forwarded network packets. The port treats the network as a shared
 * bus, so the router is a connector block; its flash-drive filters are not ported yet.
 */
public class NetworkRouterBlock extends Block implements MatterNetworkBlock {
    public static final MapCodec<NetworkRouterBlock> CODEC = simpleCodec(NetworkRouterBlock::new);

    public NetworkRouterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<NetworkRouterBlock> codec() {
        return CODEC;
    }
}
