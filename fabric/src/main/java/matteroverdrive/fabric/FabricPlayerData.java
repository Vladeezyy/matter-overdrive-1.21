package matteroverdrive.fabric;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import io.netty.buffer.Unpooled;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.platform.INBTSerializable;
import matteroverdrive.platform.PlayerData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The player data on Fabric: a persistent data attachment copied on death, and the mod's own payload for what
 * NeoForge's attachment sync does (on login, respawn, dimension change, start of tracking, and when the code asks).
 */
final class FabricPlayerData {
    private static final Map<ResourceLocation, AttachmentType<?>> TYPES = new HashMap<>();
    private static final Map<ResourceLocation, PlayerData<?>> DATA = new HashMap<>();
    /** The registries saved data is read and written with (the running server's) */
    static volatile MinecraftServer server;

    private FabricPlayerData() {}

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

    private static HolderLookup.Provider registries() {
        MinecraftServer current = server;
        return current != null ? current.registryAccess() : RegistryAccess.EMPTY;
    }

    static void register() {
        for (PlayerData<?> data : matteroverdrive.init.MOAttachments.ALL) attachment(data);
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> syncAll(handler.player, true));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> syncAll(newPlayer, true));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> syncAll(player, true));
        EntityTrackingEvents.START_TRACKING.register((tracked, player) -> {
            if (tracked instanceof Player holder) {
                for (PlayerData<?> data : DATA.values()) send(holder, player, data, true);
            }
        });
    }

    private static <T extends INBTSerializable<CompoundTag>> void attachment(PlayerData<T> data) {
        Codec<T> codec = CompoundTag.CODEC.xmap(tag -> {
            T value = data.factory().get();
            value.deserializeNBT(registries(), tag);
            return value;
        }, value -> value.serializeNBT(registries()));
        AttachmentType<T> type = AttachmentRegistry.<T>builder()
                .persistent(codec)
                .copyOnDeath()
                .initializer(data.factory())
                .buildAndRegister(data.id());
        TYPES.put(data.id(), type);
        DATA.put(data.id(), data);
    }

    @SuppressWarnings("unchecked")
    private static <T extends INBTSerializable<CompoundTag>> AttachmentType<T> type(PlayerData<T> data) {
        return (AttachmentType<T>) TYPES.get(data.id());
    }

    static <T extends INBTSerializable<CompoundTag>> T get(Player player, PlayerData<T> data) {
        return player.getAttachedOrCreate(type(data));
    }

    private static void syncAll(ServerPlayer player, boolean initial) {
        for (PlayerData<?> data : DATA.values()) {
            send(player, player, data, initial);
            for (ServerPlayer tracking : PlayerLookup.tracking(player)) send(player, tracking, data, initial);
        }
    }

    static <T extends INBTSerializable<CompoundTag>> void sync(ServerPlayer player, PlayerData<T> data) {
        send(player, player, data, false);
        for (ServerPlayer tracking : PlayerLookup.tracking(player)) send(player, tracking, data, false);
    }

    private static <T extends INBTSerializable<CompoundTag>> void send(Player holder, ServerPlayer to, PlayerData<T> data, boolean initial) {
        if (!data.sync().sendToPlayer(holder, to) || !ServerPlayNetworking.canSend(to, SyncPayload.TYPE)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), to.registryAccess());
        data.sync().write(buf, get(holder, data), initial);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ServerPlayNetworking.send(to, new SyncPayload(data.id(), holder.getId(), bytes));
    }

    /** Client side: the data of a player from the server */
    @SuppressWarnings("unchecked")
    static <T extends INBTSerializable<CompoundTag>> void receive(SyncPayload payload, Player clientPlayer) {
        PlayerData<T> data = (PlayerData<T>) DATA.get(payload.data());
        Entity entity = clientPlayer.level().getEntity(payload.entity());
        if (data == null || !(entity instanceof Player holder)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()), clientPlayer.registryAccess());
        T previous = holder.getAttached(type(data));
        T read = data.sync().read(holder, buf, previous);
        if (read != previous) holder.setAttached(type(data), read);
    }
}
