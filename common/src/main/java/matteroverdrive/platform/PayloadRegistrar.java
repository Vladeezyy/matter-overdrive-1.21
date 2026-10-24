package matteroverdrive.platform;

import java.util.function.BiConsumer;

import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.CustomPacketPayload;

/** Registers play payloads (the calls of NeoForge's {@code PayloadRegistrar} the mod uses) */
public interface PayloadRegistrar {
    <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler);

    /** Both ways, one handler (it runs on whichever side received the payload) */
    <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler);

    <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler);
}
