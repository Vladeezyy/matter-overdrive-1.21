package matteroverdrive.matter;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.network.MatterValuesPayload;
import matteroverdrive.network.PatternListPayload;
import matteroverdrive.network.PatternRequestPayload;
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
        event.registrar("1")
                .playToClient(MatterValuesPayload.TYPE, MatterValuesPayload.STREAM_CODEC, MatterValuesPayload::handle)
                .playToClient(PatternListPayload.TYPE, PatternListPayload.STREAM_CODEC, PatternListPayload::handle)
                .playToServer(PatternRequestPayload.TYPE, PatternRequestPayload.STREAM_CODEC, PatternRequestPayload::handle)
                .playToServer(matteroverdrive.network.FireWeaponPayload.TYPE, matteroverdrive.network.FireWeaponPayload.STREAM_CODEC,
                        matteroverdrive.network.FireWeaponPayload::handle)
                .playToServer(matteroverdrive.network.UnlockStatPayload.TYPE, matteroverdrive.network.UnlockStatPayload.STREAM_CODEC,
                        matteroverdrive.network.UnlockStatPayload::handle);
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
        // only clients that have this mod's channel (not vanilla clients or game-test mock players)
        event.getRelevantPlayers().filter(player -> player.connection.hasChannel(MatterValuesPayload.TYPE))
                .forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private MatterEvents() {}
}
