package matteroverdrive.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1.20.2+ GameTestHelper calls missing from 1.20.1 */
public final class GameTests {
    private GameTests() {}

    /** {@code matteroverdrive.compat.GameTests.<T>blockEntity(helper, pos)} */
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> T blockEntity(GameTestHelper helper, BlockPos pos) {
        return (T) helper.getBlockEntity(pos);
    }

    /**
     * The 1.20.2+ {@code helper.makeMockServerPlayerInLevel()}: a creative player on an embedded channel (the 1.20.1 one
     * has a connection without a channel, which Forge's login handling dereferences)
     */
    public static net.minecraft.server.level.ServerPlayer mockServerPlayer(GameTestHelper helper) {
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }

    /** {@code helper.getTestRotation()} */
    public static net.minecraft.world.level.block.Rotation rotation(GameTestHelper helper) {
        return helper.testInfo.getRotation();
    }
}
