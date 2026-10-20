package matteroverdrive.neoforge;

import java.util.function.BiConsumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.matter.MatterEvents;
import matteroverdrive.network.MOPayloads;
import matteroverdrive.platform.PacketContext;
import matteroverdrive.platform.PayloadRegistrar;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.starmap.GalaxyServer;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** The NeoForge entry point: registers the common content and passes NeoForge's events to the common handlers */
@Mod(MatterOverdrive.MODID)
public final class MatterOverdriveNeoForge {
    public MatterOverdriveNeoForge(IEventBus modBus, ModContainer container) {
        MatterOverdrive.init();
        NeoForgePlatform.registerAll(modBus);
        modBus.addListener(MatterOverdriveNeoForge::registerCapabilities);
        modBus.addListener((EntityAttributeCreationEvent event) -> MatterOverdrive.registerAttributes(event::put));
        modBus.addListener((EntityAttributeModificationEvent event) -> Android.playerAttributes().forEach(a -> event.add(EntityType.PLAYER, a)));
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> NeoForgePlatform.registerSpawnPlacements(event));
        modBus.addListener((RegisterDataMapTypesEvent event) -> event.register(MatterValues.MATTER));
        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            MOPayloads.register(new Payloads(event.registrar("1")));
            NeoForgePlayerData.registerPayload(event.registrar("1"));
        });
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(matteroverdrive.gametest.MOGameTests.class));

        IEventBus game = NeoForge.EVENT_BUS;
        NeoForgePlayerData.register(game);
        game.addListener((ServerAboutToStartEvent event) -> matteroverdrive.world.VillageHouses.onServerAboutToStart(event.getServer()));
        // matter
        game.addListener((ItemEntityPickupEvent.Pre event) -> MatterEvents.onPickup(event.getPlayer(), event.getItemEntity()));
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
        game.addListener((OnDatapackSyncEvent event) -> MatterEvents.onDatapackSync(event.getPlayerList().getServer(), event.getRelevantPlayers().toList()));
        // galaxy
        game.addListener((LevelEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level) GalaxyServer.onLevelLoad(level);
        });
        game.addListener((LevelEvent.Save event) -> {
            if (event.getLevel() instanceof ServerLevel level) GalaxyServer.onLevelSave(level);
        });
        game.addListener((ServerStoppedEvent event) -> GalaxyServer.onServerStopped());
        game.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) GalaxyServer.onLevelTick(level);
        });
        game.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) GalaxyServer.onPlayerJoin(player);
        });
        // quests
        game.addListener((LivingDeathEvent event) -> QuestEvents.onDeath(event.getEntity(), event.getSource()));
        game.addListener((PlayerEvent.ItemCraftedEvent event) -> QuestEvents.onCrafted(event.getEntity(), event.getCrafting()));
        game.addListener((BlockEvent.BreakEvent event) -> QuestEvents.onBreak(event.getPlayer(), event.getPos(), event.getState()));
        game.addListener((BlockEvent.EntityPlaceEvent event) -> QuestEvents.onPlace(event.getEntity(), event.getPos(), event.getPlacedBlock()));
        game.addListener((PlayerTickEvent.Post event) -> {
            QuestEvents.onPlayerTick(event.getEntity());
            Android.onPlayerTick(event.getEntity());
        });
        // androids
        game.addListener((LivingEvent.LivingJumpEvent event) -> Android.onJump(event.getEntity()));
        game.addListener((LivingFallEvent event) -> event.setDistance(Android.onFall(event.getEntity(), event.getDistance())));
        game.addListener((LivingIncomingDamageEvent event) -> {
            float amount = Android.onIncomingDamage(event.getEntity(), event.getSource(), event.getAmount());
            if (amount < 0) event.setCanceled(true);
            else event.setAmount(amount);
        });
        game.addListener((LivingDamageEvent.Post event) -> Android.onDamaged(event.getEntity(), event.getNewDamage()));
        game.addListener((PlayerEvent.PlayerRespawnEvent event) -> Android.onRespawn(event.getEntity(), event.isEndConquered()));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        MatterOverdrive.registerCapabilities(new MatterOverdrive.Capabilities() {
            @Override
            public void energyItem(java.util.function.Function<ItemStack, matteroverdrive.platform.transfer.IEnergyStorage> handler, Item... items) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) -> NeoForgeTransfer.toNeo(handler.apply(stack)), items);
            }

            @Override
            public void fluidItem(java.util.function.Function<ItemStack, matteroverdrive.platform.transfer.IFluidHandlerItem> handler, Item... items) {
                event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> NeoForgeTransfer.toNeoItem(handler.apply(stack)), items);
            }

            @Override
            public <T extends BlockEntity> void energy(BlockEntityType<T> type, java.util.function.BiFunction<T, Direction, matteroverdrive.platform.transfer.IEnergyStorage> handler) {
                event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> NeoForgeTransfer.toNeo(handler.apply(be, side)));
            }

            @Override
            public <T extends BlockEntity> void items(BlockEntityType<T> type, java.util.function.BiFunction<T, Direction, matteroverdrive.platform.transfer.IItemHandler> handler) {
                event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> NeoForgeTransfer.toNeo(handler.apply(be, side)));
            }

            @Override
            public <T extends BlockEntity> void fluids(BlockEntityType<T> type, java.util.function.BiFunction<T, Direction, matteroverdrive.platform.transfer.IFluidHandler> handler) {
                event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> NeoForgeTransfer.toNeo(handler.apply(be, side)));
            }
        });
    }

    /** The common payload registrar on NeoForge's */
    private record Payloads(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) implements PayloadRegistrar {
        private static <T extends CustomPacketPayload> net.neoforged.neoforge.network.handling.IPayloadHandler<T> handler(BiConsumer<T, PacketContext> handler) {
            return (payload, context) -> handler.accept(payload, new Context(context));
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
            this.registrar.playToClient(type, codec, handler(handler));
            return this;
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
            this.registrar.playBidirectional(type, codec, handler(handler));
            return this;
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
            this.registrar.playToServer(type, codec, handler(handler));
            return this;
        }
    }

    private record Context(IPayloadContext context) implements PacketContext {
        @Override
        public Player player() {
            return this.context.player();
        }

        @Override
        public void enqueueWork(Runnable work) {
            this.context.enqueueWork(work);
        }
    }
}
