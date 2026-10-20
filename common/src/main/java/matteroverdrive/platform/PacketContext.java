package matteroverdrive.platform;

import net.minecraft.world.entity.player.Player;

/** What a packet handler gets (the parts of NeoForge's {@code IPayloadContext} the handlers use) */
public interface PacketContext {
    /** The sender on the server, the local player on the client */
    Player player();

    /** Runs the work on the game thread */
    void enqueueWork(Runnable work);
}
