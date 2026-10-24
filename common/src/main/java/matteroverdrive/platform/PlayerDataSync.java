package matteroverdrive.platform;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.compat.codec.CustomPacketPayload;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.StreamCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * What the 1.21 attachment sync did, for both 1.20.1 loaders: the data goes to the clients its {@link PlayerData.SyncHandler}
 * picks on login, respawn, dimension change, start of tracking (the loaders call those) and when the code asks.
 */
public final class PlayerDataSync {
    private PlayerDataSync() {}

    public record SyncPayload(ResourceLocation data, int entity, byte[] bytes) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(new ResourceLocation(MatterOverdrive.MODID, "player_data"));
        public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
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

    /** Login, respawn, dimension change: the player's data to itself and to the players tracking it */
    public static void syncAll(ServerPlayer player) {
        for (PlayerData<?> data : matteroverdrive.init.MOAttachments.ALL) sync(player, data, true);
    }

    /** {@code to} starts tracking {@code tracked} */
    public static void startTracking(Entity tracked, ServerPlayer to) {
        if (tracked instanceof Player holder) {
            for (PlayerData<?> data : matteroverdrive.init.MOAttachments.ALL) send(holder, to, data, true);
        }
    }

    public static <T extends INBTSerializable<CompoundTag>> void sync(ServerPlayer player, PlayerData<T> data, boolean initial) {
        send(player, player, data, initial);
        for (ServerPlayer tracking : player.serverLevel().getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (tracking != player && tracking.distanceToSqr(player) < 256 * 256) send(player, tracking, data, initial);
        }
    }

    private static <T extends INBTSerializable<CompoundTag>> void send(Player holder, ServerPlayer to, PlayerData<T> data, boolean initial) {
        if (!data.sync().sendToPlayer(holder, to) || !Platform.INSTANCE.canSend(to, SyncPayload.TYPE)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), to.level().registryAccess());
        data.sync().write(buf, data.get(holder), initial);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        Platform.INSTANCE.sendToPlayer(to, new SyncPayload(data.id(), holder.getId(), bytes));
    }

    /** Client side: the data of a player from the server */
    @SuppressWarnings("unchecked")
    static <T extends INBTSerializable<CompoundTag>> void receive(SyncPayload payload, PacketContext context) {
        PlayerData<T> data = null;
        for (PlayerData<?> each : matteroverdrive.init.MOAttachments.ALL) {
            if (each.id().equals(payload.data())) data = (PlayerData<T>) each;
        }
        Player clientPlayer = context.player();
        if (data == null || clientPlayer == null) return;
        Entity entity = clientPlayer.level().getEntity(payload.entity());
        if (!(entity instanceof Player holder)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()), clientPlayer.level().registryAccess());
        PlayerDataStore store = Platform.INSTANCE.playerData(holder);
        T previous = store.get(data);
        T read = data.sync().read(holder, buf, previous);
        if (read != previous) store.set(data, read);
    }
}
