package matteroverdrive.platform;

import java.util.function.BiConsumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Registers play payloads (the calls of NeoForge's {@code PayloadRegistrar} the mod uses) */
public interface PayloadRegistrar {
    <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler);

    /** Both ways, one handler (it runs on whichever side received the payload) */
    <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler);

    /** Both ways, a handler per side (NeoForge 21.8's playBidirectional with a client and a server handler) */
    default <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                          BiConsumer<T, PacketContext> clientHandler, BiConsumer<T, PacketContext> serverHandler) {
        return this.playBidirectional(type, codec, (payload, context) ->
                (context.player() != null && context.player().level().isClientSide() ? clientHandler : serverHandler).accept(payload, context));
    }

    <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler);
}
