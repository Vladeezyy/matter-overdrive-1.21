package matteroverdrive.compat;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/** Stand-in for NeoForge 21.6+'s AttachmentSyncHandler (attachment sync is {@link AttachmentSync} before that). */
public interface AttachmentSyncHandler<T> {
    default boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
        return holder == to;
    }

    void write(RegistryFriendlyByteBuf buf, T attachment, boolean initialSync);

    T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable T previous);
}
