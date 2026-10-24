package matteroverdrive.fabric;

import matteroverdrive.platform.PacketContext;
import matteroverdrive.platform.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;

/** The client half of the Fabric networking (only loaded on a physical client) */
public final class FabricClientHooks {
    private FabricClientHooks() {}

    private static RegistryAccess registries() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
    }

    public static void registerNetworking() {
        FabricPlatform.clientSender = payload -> ClientPlayNetworking.send(payload.type().id(), Payloads.INSTANCE.encode(payload, registries()));
        for (Payloads.Entry<?> entry : Payloads.INSTANCE.entries()) {
            if (!entry.toClient()) continue;
            var id = entry.type().id();
            ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, responseSender) ->
                    Payloads.INSTANCE.handle(id, buf, registries(), new PacketContext() {
                        @Override
                        public Player player() {
                            return client.player;
                        }

                        @Override
                        public void enqueueWork(Runnable work) {
                            client.execute(work);
                        }
                    }, true));
        }
    }
}
