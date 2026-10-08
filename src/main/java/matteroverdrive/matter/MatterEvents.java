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
        matteroverdrive.network.AndroidPayloads.register(event.registrar("1"));
        matteroverdrive.network.ScannerPayloads.register(event.registrar("1"));
        event.registrar("1")
                .playToClient(MatterValuesPayload.TYPE, MatterValuesPayload.STREAM_CODEC, MatterValuesPayload::handle)
                .playToClient(PatternListPayload.TYPE, PatternListPayload.STREAM_CODEC, PatternListPayload::handle)
                .playToServer(PatternRequestPayload.TYPE, PatternRequestPayload.STREAM_CODEC, PatternRequestPayload::handle)
                .playToServer(matteroverdrive.network.FireWeaponPayload.TYPE, matteroverdrive.network.FireWeaponPayload.STREAM_CODEC,
                        matteroverdrive.network.FireWeaponPayload::handle)
                .playToServer(matteroverdrive.network.TransporterPayload.TYPE, matteroverdrive.network.TransporterPayload.STREAM_CODEC,
                        matteroverdrive.network.TransporterPayload::handle)
                .playToServer(matteroverdrive.network.HoloSignPayload.TYPE, matteroverdrive.network.HoloSignPayload.STREAM_CODEC,
                        matteroverdrive.network.HoloSignPayload::handle)
                .playToServer(matteroverdrive.network.UnlockStatPayload.TYPE, matteroverdrive.network.UnlockStatPayload.STREAM_CODEC,
                        matteroverdrive.network.UnlockStatPayload::handle);
    }

    /** 1.7.10 EntityHandler.onEntityItemPickup: portable decomposers in the hotbar eat the listed items first. */
    @SubscribeEvent
    static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        var player = event.getPlayer();
        var item = event.getItemEntity().getItem();
        if (player.level().isClientSide() || item.isEmpty() || !MatterHelper.hasMatter(item)) return;
        for (int i = 0; i < 9 && !item.isEmpty(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof matteroverdrive.item.PortableDecomposerItem) {
                matteroverdrive.item.PortableDecomposerItem.decompose(player.level().getServer(), stack, item);
            }
        }
        if (item.isEmpty()) event.getItemEntity().discard();
        else event.getItemEntity().setItem(item);
    }

    /** 1.7.10 PlayerEventHandler.onAnvilRepair: decomposer + an item adds that item to its list (3 levels, 1 item). */
    @SubscribeEvent
    static void onAnvil(net.neoforged.neoforge.event.AnvilUpdateEvent event) {
        if (!(event.getLeft().getItem() instanceof matteroverdrive.item.PortableDecomposerItem) || event.getRight().isEmpty()) return;
        var output = event.getLeft().copy();
        if (!matteroverdrive.item.PortableDecomposerItem.addToList(output, event.getRight())) return;
        event.setOutput(output);
        event.setMaterialCost(1);
        event.setXpCost(3);
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
