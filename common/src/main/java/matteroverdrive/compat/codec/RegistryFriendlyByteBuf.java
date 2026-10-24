package matteroverdrive.compat.codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;

/** 1.20.1 stand-in for the 1.20.5+ {@code RegistryFriendlyByteBuf}: a {@link FriendlyByteBuf} that knows the registries. */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(ByteBuf source, RegistryAccess registryAccess) {
        super(source);
        this.registryAccess = registryAccess;
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    /** The buffer as a registry-aware one: the same object, or a wrapper with the given registries */
    public static RegistryFriendlyByteBuf of(ByteBuf buf, RegistryAccess access) {
        return buf instanceof RegistryFriendlyByteBuf r ? r : new RegistryFriendlyByteBuf(buf, access);
    }
}
