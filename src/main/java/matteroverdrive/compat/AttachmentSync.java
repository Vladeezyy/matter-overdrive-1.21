package matteroverdrive.compat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Player attachment sync before NeoForge 21.6 (which syncs them itself): the server sends a player's attachment to
 * the players its handler allows - on login, respawn, dimension change, when someone starts tracking the player and on
 * {@link #sync} - and the client reads it with the same handler.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID)
public final class AttachmentSync {
    private record Entry<T>(Supplier<AttachmentType<T>> type, AttachmentSyncHandler<T> handler) {}

    private static final Map<ResourceLocation, Entry<?>> ENTRIES = new LinkedHashMap<>();

    public record Payload(int entity, ResourceLocation attachment, byte[] data) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "sync_attachment"));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::entity, ResourceLocation.STREAM_CODEC, Payload::attachment, ByteBufCodecs.BYTE_ARRAY, Payload::data,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    private AttachmentSync() {}

    public static <T> void register(String name, Supplier<AttachmentType<T>> type, AttachmentSyncHandler<T> handler) {
        ENTRIES.put(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, name), new Entry<>(type, handler));
    }

    /** Whether a player can receive the sync (GameTest mock players have no channels). */
    public static boolean canReceive(ServerPlayer player) {
        return player.connection.hasChannel(Payload.TYPE);
    }

    /** Sends the player's attachment to everyone its handler allows (the player and those tracking it). */
    public static <T> void sync(Player player, Supplier<AttachmentType<T>> type) {
        if (!(player instanceof ServerPlayer server)) return;
        for (var e : ENTRIES.entrySet()) {
            if ((Object) e.getValue().type() == type) send(e.getKey(), e.getValue(), server, server, false);
        }
        for (ServerPlayer to : ((ServerLevel) server.level()).players()) {
            if (to == server) continue;
            for (var e : ENTRIES.entrySet()) {
                if ((Object) e.getValue().type() == type) send(e.getKey(), e.getValue(), server, to, false);
            }
        }
    }

    private static void syncAll(ServerPlayer holder, ServerPlayer to, boolean initial) {
        ENTRIES.forEach((id, entry) -> send(id, entry, holder, to, initial));
    }

    private static <T> void send(ResourceLocation id, Entry<T> entry, ServerPlayer holder, ServerPlayer to, boolean initial) {
        if (!canReceive(to) || !entry.handler().sendToPlayer(holder, to)) return;
        if (to != holder && !holder.level().players().contains(to)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), holder.registryAccess());
        entry.handler().write(buf, holder.getData(entry.type()), initial);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        PacketDistributor.sendToPlayer(to, new Payload(holder.getId(), id, data));
    }

    @SuppressWarnings("unchecked")
    private static <T> void receive(Payload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        Entry<T> entry = (Entry<T>) ENTRIES.get(payload.attachment());
        if (entry == null || context.player() == null) return;
        if (!(context.player().level().getEntity(payload.entity()) instanceof Player holder)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), holder.registryAccess());
        T previous = holder.hasData(entry.type()) ? holder.getData(entry.type()) : null;
        holder.setData(entry.type(), entry.handler().read(holder, buf, previous));
    }

    @EventBusSubscriber(modid = MatterOverdrive.MODID)
    static final class ModEvents {
        @SubscribeEvent
        static void registerPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").optional().playToClient(Payload.TYPE, Payload.STREAM_CODEC, AttachmentSync::receive);
        }
    }

    @SubscribeEvent
    static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncAll(player, player, true);
    }

    @SubscribeEvent
    static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncAll(player, player, true);
    }

    @SubscribeEvent
    static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncAll(player, player, true);
    }

    @SubscribeEvent
    static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer holder && event.getEntity() instanceof ServerPlayer to) syncAll(holder, to, true);
    }
}
