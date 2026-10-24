package matteroverdrive.platform;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Data kept on a player, saved with it, copied on death and synced to clients (NeoForge: an attachment type with a
 * sync handler; Fabric: a data attachment and the mod's own sync payload).
 */
public record PlayerData<T extends INBTSerializable<CompoundTag>>(ResourceLocation id, Supplier<T> factory, SyncHandler<T> sync) {
    /** What goes to which client (the methods of NeoForge's {@code AttachmentSyncHandler}) */
    public interface SyncHandler<T> {
        /** Whether {@code to} gets the data of {@code holder} (the player itself and/or players tracking it) */
        boolean sendToPlayer(Player holder, ServerPlayer to);

        void write(RegistryFriendlyByteBuf buf, T data, boolean initialSync);

        T read(Player holder, RegistryFriendlyByteBuf buf, @Nullable T previous);
    }

    public T get(Player player) {
        return Platform.INSTANCE.playerData(player).get(this);
    }

    /** Sends the data to the clients that get it (server side) */
    public void sync(Player player) {
        if (player instanceof ServerPlayer server) PlayerDataSync.sync(server, this, false);
    }
}
