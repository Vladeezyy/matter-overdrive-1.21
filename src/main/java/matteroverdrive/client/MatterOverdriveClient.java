package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.screen.AnalyzerScreen;
import matteroverdrive.client.screen.DecomposerScreen;
import matteroverdrive.client.screen.FusionReactorScreen;
import matteroverdrive.client.screen.InscriberScreen;
import matteroverdrive.client.screen.PatternMonitorScreen;
import matteroverdrive.client.screen.PatternStorageScreen;
import matteroverdrive.client.screen.ReplicatorScreen;
import matteroverdrive.client.screen.RecyclerScreen;
import matteroverdrive.init.MOFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;
import matteroverdrive.init.MOItems;
import matteroverdrive.client.screen.SolarPanelScreen;
import matteroverdrive.init.MOMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = MatterOverdrive.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public class MatterOverdriveClient {
    /** 1.7.10 BlockDecorative.colorMultiplier / BlockTritaniumCrate.getBlockColor: plates and crate overlays take their dye colour. */
    @SubscribeEvent
    static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        var colors = net.minecraft.world.item.DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            int rgb = colors[i].getTextureDiffuseColor();
            event.register((state, level, pos, tint) -> rgb, matteroverdrive.init.MODecorative.COLORED_PLATES.get(i).get());
            event.register((state, level, pos, tint) -> tint == 0 ? rgb : -1, matteroverdrive.init.MOBlocks.TRITANIUM_CRATES.get(i).get());
        }
    }

    public MatterOverdriveClient() {
        matteroverdrive.block.HoloSignBlock.openEditor = pos -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            String text = mc.level.getBlockEntity(pos) instanceof matteroverdrive.block.entity.HoloSignBlockEntity sign ? sign.getText() : "";
            mc.setScreen(new matteroverdrive.client.screen.HoloSignScreen(pos, text));
        };
        // 1.7.10 AndroidPlayer.playTransformMusic
        matteroverdrive.android.AndroidClientHooks.transformationStarted = player -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (player == mc.player) {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(
                        matteroverdrive.init.MOSounds.TRANSFORMATION_MUSIC.get(), 1));
            }
        };
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MOMenus.SOLAR_PANEL.get(), SolarPanelScreen::new);
        event.register(MOMenus.WEAPON_STATION.get(), matteroverdrive.client.screen.WeaponStationScreen::new);
        event.register(MOMenus.ANDROID_STATION.get(), matteroverdrive.client.screen.AndroidStationScreen::new);
        event.register(MOMenus.CHARGING_STATION.get(), matteroverdrive.client.screen.ChargingStationScreen::new);
        event.register(MOMenus.INSCRIBER.get(), InscriberScreen::new);
        event.register(MOMenus.DECOMPOSER.get(), DecomposerScreen::new);
        event.register(MOMenus.RECYCLER.get(), RecyclerScreen::new);
        event.register(MOMenus.ANALYZER.get(), AnalyzerScreen::new);
        event.register(MOMenus.PATTERN_STORAGE.get(), PatternStorageScreen::new);
        event.register(MOMenus.PATTERN_MONITOR.get(), PatternMonitorScreen::new);
        event.register(MOMenus.REPLICATOR.get(), ReplicatorScreen::new);
        event.register(MOMenus.FUSION_REACTOR.get(), FusionReactorScreen::new);
    }

    @SubscribeEvent
    static void registerSelectProperties(net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "barrel"), BarrelProperty.TYPE);
    }

    @SubscribeEvent
    static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.PLASMA_BOLT.get(), PlasmaBoltRenderer::new);
        event.registerBlockEntityRenderer(matteroverdrive.init.MOBlockEntities.WEAPON_STATION.get(), WeaponStationRenderer::new);
        event.registerBlockEntityRenderer(matteroverdrive.init.MOBlockEntities.HOLO_SIGN.get(), HoloSignRenderer::new);
        event.registerBlockEntityRenderer(matteroverdrive.init.MOBlockEntities.GRAVITATIONAL_ANOMALY.get(), AnomalyRenderer::new);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.ROGUE_ANDROID.get(), RogueAndroidRenderer::melee);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.RANGED_ROGUE_ANDROID.get(), RogueAndroidRenderer::ranged);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.MUTANT_SCIENTIST.get(), MutantScientistRenderer::new);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.FAILED_PIG.get(), FailedAnimalRenderers::pig);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.FAILED_COW.get(), FailedAnimalRenderers::cow);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.FAILED_CHICKEN.get(), FailedAnimalRenderers::chicken);
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.FAILED_SHEEP.get(), FailedAnimalRenderers::sheep);
    }

    @SubscribeEvent
    static void registerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RogueAndroidRenderer.MELEE, RogueAndroidRenderer::meleeLayer);
        event.registerLayerDefinition(RogueAndroidRenderer.RANGED, RogueAndroidRenderer::rangedLayer);
        event.registerLayerDefinition(RogueAndroidRenderer.VISOR, RogueAndroidRenderer::visorLayer);
        event.registerLayerDefinition(MutantScientistRenderer.LAYER, HulkingScientistModel::createLayer);
    }

    /** Matter Plasma textures (1.7.10 matter_plasma_still / _flowing), shown by tanks of other mods. */
    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "block/matter_plasma_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "block/matter_plasma_flowing");
            }
        }, MOFluids.MATTER_PLASMA_TYPE.get());
        // 1.7.10 weapons stayed raised while firing; vanilla drops the hand after every successful use (itemUsed)
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProgress, float swingProgress) {
                if (!player.isUsingItem() || player.getUseItem().getItem() != stack.getItem()) return false;
                pose.translate(arm == HumanoidArm.RIGHT ? 0.56f : -0.56f, -0.52f, -0.72f);
                return true;
            }

            /** Aim the weapon down the line of sight while firing or zooming, as seen by other players. */
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return entity.isUsingItem() && entity.getUsedItemHand() == hand ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
            }
        }, MOItems.PHASER.get(), MOItems.PHASER_RIFLE.get(), MOItems.PLASMA_SHOTGUN.get(), MOItems.ION_SNIPER.get());
    }
}
