package matteroverdrive.forge;

import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.compat.codec.CustomPacketPayload;
import matteroverdrive.platform.PacketContext;
import matteroverdrive.platform.Payloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * The mod's payloads on Forge 1.20.1: one channel, each packet the payload's id and then its bytes from {@link Payloads}.
 * Both sides need the mod (the channel is required, like the 1.21 payloads).
 */
final class ForgeNetwork {
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(MatterOverdrive.MODID, "main");
    private static final String VERSION = "1";
    private static EventNetworkChannel channel;
    /** The client's registries (set by the client entry point; the server's are taken from the running server) */
    static volatile Supplier<RegistryAccess> clientRegistries = () -> RegistryAccess.EMPTY;
    /** The local player (set by the client entry point) */
    static volatile Supplier<Player> clientPlayer = () -> null;

    private ForgeNetwork() {}

    static void register() {
        Payloads.registerAll();
        channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_ID).networkProtocolVersion(() -> VERSION)
                .clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).eventNetworkChannel();
        channel.addListener((NetworkEvent.ServerCustomPayloadEvent event) -> {
            if (event instanceof NetworkEvent.ServerCustomPayloadLoginEvent) return;
            NetworkEvent.Context context = event.getSource().get();
            ServerPlayer sender = context.getSender();
            if (sender != null) receive(event.getPayload(), sender.server.registryAccess(), context, sender, false);
            context.setPacketHandled(true);
        });
        channel.addListener((NetworkEvent.ClientCustomPayloadEvent event) -> {
            if (event instanceof NetworkEvent.ClientCustomPayloadLoginEvent) return;
            NetworkEvent.Context context = event.getSource().get();
            receive(event.getPayload(), clientRegistries.get(), context, null, true);
            context.setPacketHandled(true);
        });
    }

    private static void receive(FriendlyByteBuf buf, RegistryAccess registries, NetworkEvent.Context context, Player sender, boolean onClient) {
        ResourceLocation id = buf.readResourceLocation();
        Payloads.INSTANCE.handle(id, buf, registries, new PacketContext() {
            @Override
            public Player player() {
                return onClient ? clientPlayer.get() : sender;
            }

            @Override
            public void enqueueWork(Runnable work) {
                context.enqueueWork(work);
            }
        }, onClient);
    }

    private static Packet<?> packet(CustomPacketPayload payload, RegistryAccess registries, NetworkDirection direction) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(payload.type().id());
        buf.writeBytes(Payloads.INSTANCE.encode(payload, registries));
        return direction.buildPacket(Pair.of(buf, Integer.MIN_VALUE), CHANNEL_ID).getThis();
    }

    private static RegistryAccess serverRegistries() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.SERVER.noArg().send(packet(payload, clientRegistries.get(), NetworkDirection.PLAY_TO_SERVER));
    }

    static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null) player.connection.send(packet(payload, player.server.registryAccess(), NetworkDirection.PLAY_TO_CLIENT));
    }

    static void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity).send(packet(payload, serverRegistries(), NetworkDirection.PLAY_TO_CLIENT));
    }

    static void sendToTrackingChunk(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)).send(packet(payload, level.registryAccess(), NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Not for vanilla clients nor game-test mock players (no connection channel) */
    static boolean canSend(ServerPlayer player) {
        return player.connection != null && player.connection.connection != null && channel.isRemotePresent(player.connection.connection);
    }
}
