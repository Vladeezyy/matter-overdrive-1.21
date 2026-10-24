package matteroverdrive.platform;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.compat.codec.CustomPacketPayload;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.StreamCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's payloads on 1.20.1, where neither loader has typed payloads: each payload type is a channel named by its id,
 * and this turns payloads into the bytes the loader sends and back. The loaders only move the bytes.
 */
public final class Payloads implements PayloadRegistrar {
    public static final Payloads INSTANCE = new Payloads();

    public record Entry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         BiConsumer<T, PacketContext> handler, boolean toClient, boolean toServer) {}

    private final Map<ResourceLocation, Entry<?>> entries = new LinkedHashMap<>();

    private Payloads() {}

    /** Registers every payload of the mod once (both loaders call it at startup) */
    public static synchronized void registerAll() {
        if (INSTANCE.entries.isEmpty()) {
            matteroverdrive.network.MOPayloads.register(INSTANCE);
            INSTANCE.playToClient(PlayerDataSync.SyncPayload.TYPE, PlayerDataSync.SyncPayload.CODEC, PlayerDataSync::receive);
        }
    }

    public Collection<Entry<?>> entries() {
        return entries.values();
    }

    public @Nullable Entry<?> entry(ResourceLocation id) {
        return entries.get(id);
    }

    private <T extends CustomPacketPayload> Payloads add(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         BiConsumer<T, PacketContext> handler, boolean toClient, boolean toServer) {
        if (entries.put(type.id(), new Entry<>(type, codec, handler, toClient, toServer)) != null) {
            throw new IllegalStateException("Payload registered twice: " + type.id());
        }
        return this;
    }

    @Override
    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
        return add(type, codec, handler, true, false);
    }

    @Override
    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
        return add(type, codec, handler, true, true);
    }

    @Override
    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
        return add(type, codec, handler, false, true);
    }

    /** The payload's bytes (without its id) */
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> FriendlyByteBuf encode(T payload, RegistryAccess registries) {
        Entry<T> entry = (Entry<T>) entries.get(payload.type().id());
        if (entry == null) throw new IllegalStateException("Unregistered payload " + payload.type().id());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        entry.codec().encode(buf, payload);
        return buf;
    }

    /** Decodes on the network thread and runs the handler on the game thread; a payload from the wrong side is dropped */
    public <T extends CustomPacketPayload> void handle(ResourceLocation id, FriendlyByteBuf bytes, RegistryAccess registries, PacketContext context, boolean onClient) {
        @SuppressWarnings("unchecked")
        Entry<T> entry = (Entry<T>) entries.get(id);
        if (entry == null || !(onClient ? entry.toClient() : entry.toServer())) {
            MatterOverdrive.LOGGER.warn("Dropped payload {} sent to the {}", id, onClient ? "client" : "server");
            return;
        }
        T payload = entry.codec().decode(new RegistryFriendlyByteBuf(bytes, registries));
        context.enqueueWork(() -> entry.handler().accept(payload, context));
    }
}
