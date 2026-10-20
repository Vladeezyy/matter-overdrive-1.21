package matteroverdrive.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/** The client's world for common code (only called on a physical client: this class is never loaded on a server) */
public final class ClientAccess {
    public static @Nullable Level level() {
        return Minecraft.getInstance().level;
    }

    private ClientAccess() {}
}
