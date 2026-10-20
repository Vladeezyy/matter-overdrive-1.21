package matteroverdrive.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.item.UseFirst;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.matter.MatterEvents;
import matteroverdrive.network.MOPayloads;
import matteroverdrive.platform.PacketContext;
import matteroverdrive.platform.PayloadRegistrar;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.starmap.GalaxyServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** The Fabric entry point: registers the common content and passes Fabric's events (and the mixins') to the common handlers */
public final class MatterOverdriveFabric implements ModInitializer {
    /** A payload the client handles, registered by the client entry point */
    record ClientPayload<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, BiConsumer<T, PacketContext> handler) {}

    static final List<ClientPayload<?>> CLIENT_PAYLOADS = new ArrayList<>();

    @Override
    public void onInitialize() {
        MatterOverdrive.init();
        FabricPlatform.registerAll();
        FabricPlayerData.register();
        FabricStorages.register();
        MatterOverdrive.registerAttributes(FabricDefaultAttributeRegistry::register);
        // NeoForge's EntityAttributeModificationEvent: the player's attributes with the android ones
        var playerAttributes = net.minecraft.world.entity.player.Player.createAttributes();
        Android.playerAttributes().forEach(playerAttributes::add);
        FabricDefaultAttributeRegistry.register(net.minecraft.world.entity.EntityType.PLAYER, playerAttributes);
        MatterOverdrive.registerSpawnPlacements();
        FabricMatterValues.register();
        FabricBiomeModifiers.register();
        FabricLootModifiers.register();
        MOPayloads.register(new Payloads());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FabricPlayerData.server = server;
            MatterEvents.onServerDataLoaded();
            matteroverdrive.world.VillageHouses.onServerAboutToStart(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> MatterEvents.onServerDataLoaded());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> MatterEvents.onDatapackSync(player.level().getServer(), List.of(player)));
        // galaxy (saving: ServerLevelMixin)
        ServerLevelEvents.LOAD.register((server, level) -> GalaxyServer.onLevelLoad(level));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GalaxyServer.onServerStopped();
            FabricPlayerData.server = null;
        });
        ServerTickEvents.END_LEVEL_TICK.register(GalaxyServer::onLevelTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> GalaxyServer.onPlayerJoin(handler.player));
        // quests (crafting and placing: ResultSlotMixin, BlockItemMixin; player ticks: PlayerMixin)
        ServerLivingEntityEvents.AFTER_DEATH.register(QuestEvents::onDeath);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> MachineBlock.canBreak(state, level, pos, player));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> QuestEvents.onBreak(player, pos, state));
        // androids (jumps, falls and incoming damage: LivingEntityMixin)
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> Android.onDamaged(entity, damage));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Android.onRespawn(newPlayer, alive));
        // NeoForge's onItemUseFirst: before the block reacts to the click
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSpectator() || !(stack.getItem() instanceof UseFirst item)) return InteractionResult.PASS;
            return item.onItemUseFirst(stack, new UseOnContext(player, hand, hit));
        });
    }

    /** The common payload registrar on Fabric's networking */
    private static final class Payloads implements PayloadRegistrar {
        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
            PayloadTypeRegistry.clientboundPlay().register(type, codec);
            CLIENT_PAYLOADS.add(new ClientPayload<>(type, handler));
            return this;
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
            this.playToClient(type, codec, handler);
            return this.playToServer(type, codec, handler);
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, PacketContext> handler) {
            PayloadTypeRegistry.serverboundPlay().register(type, codec);
            ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, new PacketContext() {
                @Override
                public Player player() {
                    return context.player();
                }

                @Override
                public void enqueueWork(Runnable work) {
                    context.server().execute(work);
                }
            }));
            return this;
        }
    }
}
