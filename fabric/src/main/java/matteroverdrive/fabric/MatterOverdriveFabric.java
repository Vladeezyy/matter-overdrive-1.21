package matteroverdrive.fabric;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.item.UseFirst;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matter.MatterEvents;
import matteroverdrive.platform.Payloads;
import matteroverdrive.platform.PlayerDataSync;
import matteroverdrive.platform.PacketContext;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.starmap.GalaxyServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** The Fabric entry point: registers the common content and passes Fabric's events (and the mixins') to the common handlers */
public final class MatterOverdriveFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MatterOverdrive.init();
        FabricPlatform.registerAll();
        FabricStorages.register();
        MatterOverdrive.registerAttributes(FabricDefaultAttributeRegistry::register);
        // NeoForge's EntityAttributeModificationEvent: the player's attributes with the android ones
        var playerAttributes = net.minecraft.world.entity.player.Player.createAttributes();
        Android.playerAttributes().forEach(playerAttributes::add);
        FabricDefaultAttributeRegistry.register(net.minecraft.world.entity.EntityType.PLAYER, playerAttributes);
        MatterOverdrive.registerSpawnPlacements();
        registerMatterValues();
        FabricBiomeModifiers.register();
        var addedLoot = matteroverdrive.world.loot.AddedLootTables.load();
        net.fabricmc.fabric.api.loot.v2.LootTableEvents.MODIFY.register((resources, lootManager, id, builder, source) -> {
            var tables = addedLoot.get(id);
            if (tables == null) return;
            for (var table : tables) {
                builder.withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .add(net.minecraft.world.level.storage.loot.entries.LootTableReference.lootTableReference(table)));
            }
        });
        registerNetworking();
        // player data: saved by PlayerMixin, copied on death, synced like the 1.21 attachments
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> ((matteroverdrive.platform.PlayerDataStore.Holder) newPlayer)
                .matteroverdrive$playerData().copyFrom(((matteroverdrive.platform.PlayerDataStore.Holder) oldPlayer).matteroverdrive$playerData(), newPlayer.server.registryAccess()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerDataSync.syncAll(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PlayerDataSync.syncAll(newPlayer));
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> PlayerDataSync.syncAll(player));
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register(PlayerDataSync::startTracking);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            MatterEvents.onServerDataLoaded();
            matteroverdrive.world.VillageHouses.onServerAboutToStart(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> MatterEvents.onServerDataLoaded());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> MatterEvents.onDatapackSync(player.server, List.of(player)));
        // galaxy (saving: ServerLevelMixin)
        ServerWorldEvents.LOAD.register((server, level) -> GalaxyServer.onLevelLoad(level));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GalaxyServer.onServerStopped();
        });
        ServerTickEvents.END_WORLD_TICK.register(GalaxyServer::onLevelTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> GalaxyServer.onPlayerJoin(handler.player));
        // quests (crafting and placing: ResultSlotMixin, BlockItemMixin; player ticks: PlayerMixin)
        ServerLivingEntityEvents.AFTER_DEATH.register(QuestEvents::onDeath);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> MachineBlock.canBreak(state, level, pos, player));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> QuestEvents.onBreak(player, pos, state));
        // androids (jumps, falls and incoming damage: LivingEntityMixin)
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Android.onRespawn(newPlayer, alive));
        // NeoForge's onItemUseFirst: before the block reacts to the click
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSpectator() || !(stack.getItem() instanceof UseFirst item)) return InteractionResult.PASS;
            return item.onItemUseFirst(stack, new UseOnContext(player, hand, hit));
        });
    }

    private static void registerMatterValues() {
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public net.minecraft.resources.ResourceLocation getFabricId() {
                        return new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, "matter_values");
                    }

                    @Override
                    public java.util.Collection<net.minecraft.resources.ResourceLocation> getFabricDependencies() {
                        return List.of(net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys.TAGS);
                    }

                    @Override
                    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                        matteroverdrive.matter.MatterValueFiles.load(manager);
                    }
                });
    }

    /** Each payload type is a channel named by its id; {@link Payloads} turns payloads into bytes and back */
    private static void registerNetworking() {
        Payloads.registerAll();
        for (Payloads.Entry<?> entry : Payloads.INSTANCE.entries()) {
            if (!entry.toServer()) continue;
            var id = entry.type().id();
            ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) ->
                    Payloads.INSTANCE.handle(id, buf, server.registryAccess(), new PacketContext() {
                        @Override
                        public Player player() {
                            return player;
                        }

                        @Override
                        public void enqueueWork(Runnable work) {
                            server.execute(work);
                        }
                    }, false));
        }
    }
}
