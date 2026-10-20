package matteroverdrive.neoforge;

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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
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
        modBus.addListener((ModelEvent.RegisterAdditional event) -> MOClient.extraModels().forEach(id -> event.register(ModelResourceLocation.standalone(id))));
        modBus.addListener((RegisterColorHandlersEvent.Block event) -> MOClient.blockColors((color, block) -> event.register(color, block)));
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> MOClient.itemColors((color, item) -> event.register(color, item)));
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
            public <T extends net.minecraft.world.level.block.entity.BlockEntity> void blockEntity(net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                                                                                                net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> MOClient.registerLayers(event::registerLayerDefinition));
        modBus.addListener((RegisterKeyMappingsEvent event) -> MOClient.registerKeys(event::register));
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, MOClient.ANDROID_HUD, AndroidHud::render);
            event.registerAbove(VanillaGuiLayers.CHAT, MOClient.QUEST_HUD, matteroverdrive.client.quest.QuestHud::render);
        });
        modBus.addListener(MatterOverdriveNeoForgeClient::registerClientExtensions);

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((ClientTickEvent.Pre event) -> MOClient.onClientTickStart());
        game.addListener((ClientTickEvent.Post event) -> MOClient.onClientTickEnd());
        game.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> MOClient.onLoggedOut());
        game.addListener((ItemTooltipEvent event) -> MOClient.onTooltip(event.getItemStack(), event.getToolTip()));
        game.addListener((RenderGuiLayerEvent.Pre event) -> {
            ResourceLocation name = event.getName();
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
        game.addListener((RenderPlayerEvent.Pre event) -> TritaniumArmorModel.hideSkinOverlay(event.getRenderState()));
        game.addListener((RenderPlayerEvent.Post event) -> {
            // 1.21.2+: the event has the render state; the beam needs the player
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.getEntity(event.getRenderState().id) instanceof net.minecraft.world.entity.player.Player player) {
                PhaserBeamRenderer.renderPlayer(player, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource());
            }
        });
        game.addListener((RenderHandEvent event) -> PhaserBeamRenderer.renderHand(event.getHand(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource()));
        game.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) ShieldRenderer.renderLevel(event.getPoseStack());
        });
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // Matter Plasma textures (1.7.10 matter_plasma_still / _flowing), shown by tanks of other mods
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "block/matter_plasma_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "block/matter_plasma_flowing");
            }
        }, NeoForgePlatform.MATTER_PLASMA_TYPE.get());
        // 1.7.10 ModelTritaniumArmor (the layer textures: MOItems' armor getArmorTexture)
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(ItemStack stack, net.minecraft.world.item.equipment.EquipmentModel.LayerType layerType, Model original) {
                // the poses come from the render state in setupAnim; don't copy the vanilla model's visibility
                return TritaniumArmorModel.armorModel(stack);
            }
        }, MOItems.TRITANIUM_HELMET.get(), MOItems.TRITANIUM_CHESTPLATE.get(), MOItems.TRITANIUM_LEGGINGS.get(), MOItems.TRITANIUM_BOOTS.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProgress, float swingProgress) {
                return WeaponClient.applyHandTransform(pose, player, arm, stack);
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return WeaponClient.armPose(entity, hand);
            }
        }, WeaponClient.weapons());
    }
}
