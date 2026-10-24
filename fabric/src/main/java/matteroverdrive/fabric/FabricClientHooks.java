package matteroverdrive.fabric;

import matteroverdrive.platform.PacketContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** The client half of the Fabric networking (only loaded on a physical client) */
public final class FabricClientHooks {
    private FabricClientHooks() {}

    public static void registerNetworking() {
        FabricPlatform.clientSender = ClientPlayNetworking::send;
        for (MatterOverdriveFabric.ClientPayload<?> payload : MatterOverdriveFabric.CLIENT_PAYLOADS) receiver(payload);
        ClientPlayNetworking.registerGlobalReceiver(FabricPlayerData.SyncPayload.TYPE, (payload, context) -> FabricPlayerData.receive(payload, context.player()));
    }

    private static <T extends CustomPacketPayload> void receiver(MatterOverdriveFabric.ClientPayload<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (message, context) -> payload.handler().accept(message, new PacketContext() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable work) {
                context.client().execute(work);
            }
        }));
    }
}
