package matteroverdrive.forge;

import java.util.List;
import java.util.Map;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.matter.MatterEvents;
import matteroverdrive.matter.MatterValueFiles;
import matteroverdrive.platform.PlayerDataSync;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.starmap.GalaxyServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** The Forge 1.20.1 entry point: registers the common content and passes Forge's events to the common handlers */
@Mod(MatterOverdrive.MODID)
public final class MatterOverdriveForge {
    /** Target loot table → tables the mod adds to it (the 1.21 add_table loot modifiers) */
    private static Map<ResourceLocation, List<ResourceLocation>> addedLoot = Map.of();

    public MatterOverdriveForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        MatterOverdrive.init();
        ForgePlatform.registerAll(modBus);
        ForgeNetwork.register();
        modBus.addListener((FMLCommonSetupEvent event) -> {
            ForgeCaps.registerAll();
            addedLoot = matteroverdrive.world.loot.AddedLootTables.load();
        });
        modBus.addListener(ForgeCaps::onRegisterCapabilities);
        modBus.addListener((EntityAttributeCreationEvent event) -> MatterOverdrive.registerAttributes(event::put));
        modBus.addListener((EntityAttributeModificationEvent event) -> Android.playerAttributes().forEach(a -> event.add(EntityType.PLAYER, a)));
        modBus.addListener((SpawnPlacementRegisterEvent event) -> ForgePlatform.registerSpawnPlacements(event));
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(matteroverdrive.gametest.MOGameTests.class));

        IEventBus game = MinecraftForge.EVENT_BUS;
        game.addGenericListener(net.minecraft.world.level.block.entity.BlockEntity.class, ForgeCaps::onAttachBlockEntity);
        game.addGenericListener(net.minecraft.world.item.ItemStack.class, ForgeCaps::onAttachItem);
        game.addGenericListener(net.minecraft.world.entity.Entity.class, ForgeCaps::onAttachEntity);
        game.addListener(ForgeCaps::onClone);
        game.addListener((ServerAboutToStartEvent event) -> matteroverdrive.world.VillageHouses.onServerAboutToStart(event.getServer()));
        game.addListener((LootTableLoadEvent event) -> {
            List<ResourceLocation> tables = addedLoot.get(event.getName());
            if (tables == null) return;
            for (ResourceLocation table : tables) {
                event.getTable().addPool(LootPool.lootPool().name(MatterOverdrive.MODID + ":" + table.getPath())
                        .add(LootTableReference.lootTableReference(table)).build());
            }
        });
        // matter
        game.addListener((AddReloadListenerEvent event) -> event.addListener((ResourceManagerReloadListener) MatterValueFiles::load));
        game.addListener((EntityItemPickupEvent event) -> MatterEvents.onPickup(event.getEntity(), event.getItem()));
        game.addListener((AnvilUpdateEvent event) -> {
            var result = MatterEvents.onAnvil(event.getLeft(), event.getRight());
            if (result == null) return;
            event.setOutput(result.output());
            event.setCost(result.cost());
            event.setMaterialCost(result.materialCost());
        });
        game.addListener((TagsUpdatedEvent event) -> {
            if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) MatterEvents.onServerDataLoaded();
        });
        game.addListener((OnDatapackSyncEvent event) -> MatterEvents.onDatapackSync(event.getPlayerList().getServer(),
                event.getPlayer() != null ? List.of(event.getPlayer()) : event.getPlayerList().getPlayers()));
        // galaxy
        game.addListener((LevelEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level) GalaxyServer.onLevelLoad(level);
        });
        game.addListener((LevelEvent.Save event) -> {
            if (event.getLevel() instanceof ServerLevel level) GalaxyServer.onLevelSave(level);
        });
        game.addListener((ServerStoppedEvent event) -> GalaxyServer.onServerStopped());
        game.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) GalaxyServer.onLevelTick(level);
        });
        // player data sync (the 1.21 attachment sync)
        game.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerDataSync.syncAll(player);
                GalaxyServer.onPlayerJoin(player);
            }
        });
        game.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) PlayerDataSync.syncAll(player);
        });
        game.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer player) PlayerDataSync.startTracking(event.getTarget(), player);
        });
        // quests
        game.addListener((LivingDeathEvent event) -> QuestEvents.onDeath(event.getEntity(), event.getSource()));
        game.addListener((PlayerEvent.ItemCraftedEvent event) -> QuestEvents.onCrafted(event.getEntity(), event.getCrafting()));
        game.addListener((BlockEvent.BreakEvent event) -> QuestEvents.onBreak(event.getPlayer(), event.getPos(), event.getState()));
        game.addListener((BlockEvent.EntityPlaceEvent event) -> QuestEvents.onPlace(event.getEntity(), event.getPos(), event.getPlacedBlock()));
        game.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            QuestEvents.onPlayerTick(event.player);
            Android.onPlayerTick(event.player);
        });
        // androids
        game.addListener((LivingEvent.LivingJumpEvent event) -> Android.onJump(event.getEntity()));
        game.addListener((LivingFallEvent event) -> event.setDistance(Android.onFall(event.getEntity(), event.getDistance())));
        game.addListener((LivingHurtEvent event) -> {
            float amount = Android.onIncomingDamage(event.getEntity(), event.getSource(), event.getAmount());
            if (amount < 0) event.setCanceled(true);
            else event.setAmount(amount);
        });
        game.addListener((LivingDamageEvent event) -> Android.onDamaged(event.getEntity(), event.getAmount()));
        game.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            Android.onRespawn(event.getEntity(), event.isEndConquered());
            if (event.getEntity() instanceof ServerPlayer player) PlayerDataSync.syncAll(player);
        });

        if (FMLEnvironment.dist.isClient()) MatterOverdriveForgeClient.init(modBus);
    }
}
