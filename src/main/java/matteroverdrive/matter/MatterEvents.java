package matteroverdrive.matter;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.network.MatterValuesPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@EventBusSubscriber(modid = MatterOverdrive.MODID)
public final class MatterEvents {
    @SubscribeEvent
    static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(MatterRegistry.MATTER);
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(MatterValuesPayload.TYPE, MatterValuesPayload.STREAM_CODEC, MatterValuesPayload::handle);
    }

    /** Recipes and data maps were (re)loaded on the server: recalculate on next use. */
    @SubscribeEvent
    static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            MatterRegistry.invalidate();
        }
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        var payload = new MatterValuesPayload(MatterRegistry.values(event.getPlayerList().getServer()));
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private MatterEvents() {}
}
