package matteroverdrive.neoforge;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOAttachments;
import matteroverdrive.platform.INBTSerializable;
import matteroverdrive.platform.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Player data sync before NeoForge 21.6 (which syncs attachments itself): the server sends a player's data to the
 * players its handler allows - on login, respawn, dimension change, when someone starts tracking the player and on
 * {@link #sync} - and the client reads it with the same handler.
 */
final class NeoForgePlayerData {
    private NeoForgePlayerData() {}

    record SyncPayload(ResourceLocation data, int entity, byte[] bytes) implements CustomPacketPayload {
        static final Type<SyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "player_data"));
        static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeResourceLocation(payload.data());
                    buf.writeVarInt(payload.entity());
                    buf.writeByteArray(payload.bytes());
                },
                buf -> new SyncPayload(buf.readResourceLocation(), buf.readVarInt(), buf.readByteArray()));

        @Override
        public Type<SyncPayload> type() {
            return TYPE;
        }
    }

    static void register(IEventBus game) {
        game.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) syncAll(player);
        });
        game.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) syncAll(player);
        });
        game.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) syncAll(player);
        });
        game.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getTarget() instanceof Player holder && event.getEntity() instanceof ServerPlayer to) {
                for (PlayerData<?> data : MOAttachments.ALL) send(holder, to, data, true);
            }
        });
    }

    static void registerPayload(PayloadRegistrar registrar) {
        registrar.optional().playToClient(SyncPayload.TYPE, SyncPayload.CODEC, NeoForgePlayerData::receive);
    }

    private static void syncAll(ServerPlayer player) {
        for (PlayerData<?> data : MOAttachments.ALL) {
            send(player, player, data, true);
            for (ServerPlayer tracking : player.serverLevel().players()) {
                if (tracking != player) send(player, tracking, data, true);
            }
        }
    }

    static <T extends INBTSerializable<CompoundTag>> void sync(ServerPlayer player, PlayerData<T> data) {
        send(player, player, data, false);
        for (ServerPlayer tracking : player.serverLevel().players()) {
            if (tracking != player) send(player, tracking, data, false);
        }
    }

    private static <T extends INBTSerializable<CompoundTag>> void send(Player holder, ServerPlayer to, PlayerData<T> data, boolean initial) {
        if (to.connection == null || !to.connection.hasChannel(SyncPayload.TYPE) || !data.sync().sendToPlayer(holder, to)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), to.registryAccess());
        data.sync().write(buf, data.get(holder), initial);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        PacketDistributor.sendToPlayer(to, new SyncPayload(data.id(), holder.getId(), bytes));
    }

    private static void receive(SyncPayload payload, IPayloadContext context) {
        for (PlayerData<?> data : MOAttachments.ALL) {
            if (data.id().equals(payload.data())) read(payload, context.player(), data);
        }
    }

    private static <T extends INBTSerializable<CompoundTag>> void read(SyncPayload payload, Player clientPlayer, PlayerData<T> data) {
        if (!(clientPlayer.level().getEntity(payload.entity()) instanceof Player holder)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()), clientPlayer.registryAccess());
        var type = NeoForgePlatform.type(data);
        T previous = holder.hasData(type) ? holder.getData(type).value : null;
        T read = data.sync().read(holder, buf, previous);
        if (read != previous) holder.setData(type, new NeoForgePlatform.SerializableData<>(read));
    }
}
