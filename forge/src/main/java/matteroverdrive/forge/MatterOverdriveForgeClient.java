package matteroverdrive.forge;

import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.client.MOClient;
import matteroverdrive.client.PhaserBeamRenderer;
import matteroverdrive.client.TritaniumArmorModel;
import matteroverdrive.client.WeaponClient;
import matteroverdrive.client.android.AndroidHud;
import matteroverdrive.client.android.ShieldRenderer;
import matteroverdrive.compat.DeltaTracker;
import matteroverdrive.init.MOItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** The client on Forge 1.20.1: Forge's client events to the common client code */
final class MatterOverdriveForgeClient {
    private MatterOverdriveForgeClient() {}

    static void init(IEventBus modBus) {
        ForgeNetwork.clientRegistries = () -> {
            var level = Minecraft.getInstance().level;
            return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
        };
        ForgeNetwork.clientPlayer = () -> Minecraft.getInstance().player;
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            MOClient.init();
            MOClient.registerScreens(new MOClient.Screens() {
                @Override
                public <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                        net.minecraft.world.inventory.MenuType<? extends M> type, MOClient.ScreenFactory<M, U> factory) {
                    registerScreen(type, factory);
                }
            });
            registerItemExtensions();
        }));
        modBus.addListener((ModelEvent.RegisterAdditional event) -> MOClient.extraModels().forEach(event::register));
        modBus.addListener((RegisterColorHandlersEvent.Block event) -> MOClient.blockColors((color, block) -> event.register(color, block)));
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> MOClient.itemColors((color, item) -> event.register(color, item)));
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
        modBus.addListener((RegisterGuiOverlaysEvent event) -> {
            // 1.21 CAMERA_OVERLAYS = vignette .. portal, under the hotbar
            event.registerAbove(VanillaGuiOverlay.PORTAL.id(), MOClient.ANDROID_HUD.getPath(),
                    (gui, g, partialTick, width, height) -> AndroidHud.render(g, DeltaTracker.now(partialTick)));
            event.registerAbove(VanillaGuiOverlay.CHAT_PANEL.id(), MOClient.QUEST_HUD.getPath(),
                    (gui, g, partialTick, width, height) -> matteroverdrive.client.quest.QuestHud.render(g, DeltaTracker.now(partialTick)));
        });

        IEventBus game = MinecraftForge.EVENT_BUS;
        game.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) MOClient.onClientTickStart();
            else MOClient.onClientTickEnd();
        });
        game.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> MOClient.onLoggedOut());
        game.addListener((ItemTooltipEvent event) -> MOClient.onTooltip(event.getItemStack(), event.getToolTip()));
        game.addListener((RenderGuiOverlayEvent.Pre event) -> {
            ResourceLocation name = event.getOverlay().id();
            AndroidHud.VanillaLayer layer = name.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) ? AndroidHud.VanillaLayer.HEALTH
                    : name.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) ? AndroidHud.VanillaLayer.FOOD
                    : name.equals(VanillaGuiOverlay.AIR_LEVEL.id()) ? AndroidHud.VanillaLayer.AIR
                    : name.equals(VanillaGuiOverlay.PLAYER_LIST.id()) ? AndroidHud.VanillaLayer.TAB_LIST : null;
            if (layer != null && AndroidHud.hides(layer)) event.setCanceled(true);
        });
        game.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (event.isAttack() && WeaponClient.cancelsAttack()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        });
        game.addListener((ComputeFovModifierEvent event) -> event.setNewFovModifier(WeaponClient.fovModifier(event.getPlayer(), event.getNewFovModifier())));
        game.addListener((RenderPlayerEvent.Pre event) -> TritaniumArmorModel.hideSkinOverlay(event.getEntity(), event.getRenderer().getModel()));
        game.addListener((RenderPlayerEvent.Post event) -> PhaserBeamRenderer.renderPlayer(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource()));
        game.addListener((RenderHandEvent event) -> PhaserBeamRenderer.renderHand(event.getHand(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource()));
        game.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) ShieldRenderer.renderLevel(event.getPoseStack());
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void registerScreen(
            net.minecraft.world.inventory.MenuType<? extends M> type, MOClient.ScreenFactory<M, U> factory) {
        MenuScreens.register((net.minecraft.world.inventory.MenuType) type, (MenuScreens.ScreenConstructor<M, U>) factory::create);
    }

    /**
     * Forge 1.20.1 asks an item for its client extensions in its constructor (Item.initializeClient); the common items
     * can't override that, so the extensions go into the field it fills (opened by the access transformer).
     */
    private static void registerItemExtensions() {
        // 1.7.10 ModelTritaniumArmor (the layer textures: MOItems' armor getArmorTexture)
        IClientItemExtensions armor = new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return TritaniumArmorModel.posed(slot, original);
            }
        };
        for (Item item : new Item[] {MOItems.TRITANIUM_HELMET.get(), MOItems.TRITANIUM_CHESTPLATE.get(), MOItems.TRITANIUM_LEGGINGS.get(), MOItems.TRITANIUM_BOOTS.get()}) {
            item.renderProperties = armor;
        }
        IClientItemExtensions weapon = new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProgress, float swingProgress) {
                return WeaponClient.applyHandTransform(pose, player, arm, stack);
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return WeaponClient.armPose(entity, hand);
            }
        };
        for (Item item : WeaponClient.weapons()) item.renderProperties = weapon;
    }
}
