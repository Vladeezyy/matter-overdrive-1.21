package matteroverdrive.neoforge;

import matteroverdrive.init.MOFluids;

import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.MOClient;
import matteroverdrive.client.PhaserBeamRenderer;
import matteroverdrive.client.TritaniumArmorModel;
import matteroverdrive.client.WeaponClient;
import matteroverdrive.client.android.AndroidHud;
import matteroverdrive.client.android.ShieldRenderer;
import matteroverdrive.init.MOItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** The client on NeoForge: NeoForge's client events to the common client code */
@Mod(value = MatterOverdrive.MODID, dist = Dist.CLIENT)
public final class MatterOverdriveNeoForgeClient {
    public MatterOverdriveNeoForgeClient(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(MOClient::init));
        modBus.addListener((ModelEvent.RegisterStandalone event) -> MOClient.extraModels().forEach(id ->
                event.register(NeoForgeClientPlatform.key(id), net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel.blockStateModel(id))));
        // the star map screen's hologram (1.21.6+ GUI: a picture-in-picture renderer)
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) ->
                event.register(matteroverdrive.client.starmap.StarMapPipRenderer.State.class, matteroverdrive.client.starmap.StarMapPipRenderer::new));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) ->
                matteroverdrive.client.starmap.HoloRenderTypes.registerPipelines(event::registerPipeline));
        modBus.addListener((RegisterColorHandlersEvent.BlockTintSources event) -> MOClient.blockColors((tints, block) -> event.register(tints, block)));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent event) -> MOClient.registerSelectProperties(event::register));
        modBus.addListener((RegisterMenuScreensEvent event) -> MOClient.registerScreens(new MOClient.Screens() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type, MOClient.ScreenFactory<M, U> factory) {
                event.register(type, factory::create);
            }
        }));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> MOClient.registerRenderers(new MOClient.Renderers() {
            @Override
            public <E extends net.minecraft.world.entity.Entity> void entity(net.minecraft.world.entity.EntityType<? extends E> type,
                                                                           net.minecraft.client.renderer.entity.EntityRendererProvider<E> provider) {
                event.registerEntityRenderer(type, provider);
            }

            @Override
            public <T extends net.minecraft.world.level.block.entity.BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void blockEntity(
                    net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type, net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> MOClient.registerLayers(event::registerLayerDefinition));
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            MOClient.registerKeyCategories(event::registerCategory);
            MOClient.registerKeys(event::register);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) ->
                event.register(MOClient.matterPlasmaModel(), MOFluids.MATTER_PLASMA.get(), MOFluids.MATTER_PLASMA_FLOWING.get()));
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, MOClient.ANDROID_HUD, AndroidHud::render);
            event.registerAbove(VanillaGuiLayers.CHAT, MOClient.QUEST_HUD, matteroverdrive.client.quest.QuestHud::render);
        });
        modBus.addListener(MatterOverdriveNeoForgeClient::registerClientExtensions);

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((ClientTickEvent.Pre event) -> MOClient.onClientTickStart());
        game.addListener((ClientTickEvent.Post event) -> {
            MOClient.onClientTickEnd();
            matteroverdrive.client.PerfScene.onTick();
        });
        game.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) -> matteroverdrive.client.PerfScene.onFrame());
        game.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> MOClient.onLoggedOut());
        game.addListener((ItemTooltipEvent event) -> MOClient.onTooltip(event.getItemStack(), event.getToolTip()));
        game.addListener((RenderGuiLayerEvent.Pre event) -> {
            Identifier name = event.getName();
            AndroidHud.VanillaLayer layer = name.equals(VanillaGuiLayers.PLAYER_HEALTH) ? AndroidHud.VanillaLayer.HEALTH
                    : name.equals(VanillaGuiLayers.FOOD_LEVEL) ? AndroidHud.VanillaLayer.FOOD
                    : name.equals(VanillaGuiLayers.AIR_LEVEL) ? AndroidHud.VanillaLayer.AIR
                    : name.equals(VanillaGuiLayers.TAB_LIST) ? AndroidHud.VanillaLayer.TAB_LIST : null;
            if (layer != null && AndroidHud.hides(layer)) event.setCanceled(true);
        });
        game.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (event.isAttack() && WeaponClient.cancelsAttack()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        });
        game.addListener((ComputeFovModifierEvent event) -> event.setNewFovModifier(WeaponClient.fovModifier(event.getPlayer(), event.getNewFovModifier())));
        game.addListener((RenderPlayerEvent.Pre<?> event) -> TritaniumArmorModel.hideSkinOverlay(event.getRenderState()));
        game.addListener((RenderPlayerEvent.Post<?> event) -> {
            // 1.21.2+: the event has the render state; the beam needs the player
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.getEntity(event.getRenderState().id) instanceof net.minecraft.world.entity.player.Player player) {
                PhaserBeamRenderer.renderPlayer(player, event.getPartialTick(), event.getPoseStack(), event.getSubmitNodeCollector());
            }
        });
        game.addListener((RenderHandEvent event) -> PhaserBeamRenderer.renderHand(event.getHand(), event.getPartialTick(), event.getPoseStack(), event.getSubmitNodeCollector()));
        game.addListener((net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent event) -> ShieldRenderer.renderLevel(event.getPoseStack(), event.getSubmitNodeCollector()));
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // Matter Plasma textures (1.7.10 matter_plasma_still / _flowing): 26.x fluid models (RegisterFluidModelsEvent)
        // 1.7.10 ModelTritaniumArmor and its two layer textures
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(ItemStack stack, net.minecraft.client.resources.model.EquipmentClientInfo.LayerType layerType, Model original) {
                // the poses come from the render state in setupAnim; don't copy the vanilla model's visibility
                return TritaniumArmorModel.armorModel(stack);
            }

            @Override
            public Identifier getArmorTexture(ItemStack stack, net.minecraft.client.resources.model.EquipmentClientInfo.LayerType type,
                                                    net.minecraft.client.resources.model.EquipmentClientInfo.Layer layer, Identifier _default) {
                return TritaniumArmorModel.armorTexture(stack);
            }
        }, MOItems.TRITANIUM_HELMET.get(), MOItems.TRITANIUM_CHESTPLATE.get(), MOItems.TRITANIUM_LEGGINGS.get(), MOItems.TRITANIUM_BOOTS.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, net.minecraft.client.renderer.state.level.PlayerRenderState state, HumanoidArm arm,
                                                   ItemStack stack, float partialTick, float equipProgress, float swingProgress) {
                // 26.3 passes the render state; the first person hand is always the local player's
                LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
                return player != null && WeaponClient.applyHandTransform(pose, player, arm, stack);
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return WeaponClient.armPose(entity, hand);
            }
        }, WeaponClient.weapons());
    }
}
