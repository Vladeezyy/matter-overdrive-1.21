package matteroverdrive.client;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import matteroverdrive.client.screen.AnalyzerScreen;
import matteroverdrive.client.screen.DecomposerScreen;
import matteroverdrive.client.screen.FusionReactorScreen;
import matteroverdrive.client.screen.InscriberScreen;
import matteroverdrive.client.screen.PatternMonitorScreen;
import matteroverdrive.client.screen.PatternStorageScreen;
import matteroverdrive.client.screen.RecyclerScreen;
import matteroverdrive.client.screen.ReplicatorScreen;
import matteroverdrive.client.screen.SolarPanelScreen;
import matteroverdrive.init.MOMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The client side of the mod on both loaders: each loader's client entry point calls these from its own events
 * (NeoForge: the mod and game buses; Fabric: its client callbacks and a few mixins).
 */
public final class MOClient {
    /** Client setup (main thread): hooks from common code into the client */
    public static void init() {
        matteroverdrive.block.HoloSignBlock.openEditor = pos -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            String text = mc.level.getBlockEntity(pos) instanceof matteroverdrive.block.entity.HoloSignBlockEntity sign ? sign.getText() : "";
            mc.setScreen(new matteroverdrive.client.screen.HoloSignScreen(pos, text));
        };
        // 1.7.10 AndroidPlayer.playTransformMusic
        matteroverdrive.quest.PlayerQuests.clientUpdated = matteroverdrive.client.quest.QuestHud::onSync;
        matteroverdrive.network.GuideRecipePayload.onGrid = matteroverdrive.client.guide.GuideRecipes::receive;
        matteroverdrive.item.ContractItem.openScreen = hand -> net.minecraft.client.Minecraft.getInstance().setScreen(
                new matteroverdrive.client.quest.ContractScreen(hand));
        matteroverdrive.item.ContractItem.clientPlayer = () -> net.minecraft.client.Minecraft.getInstance().player;
        matteroverdrive.item.DataPadItem.openScreen = hand -> net.minecraft.client.Minecraft.getInstance().setScreen(
                new matteroverdrive.client.quest.DataPadScreen(hand));
        matteroverdrive.dialog.DialogPayloads.openScreen = npc -> net.minecraft.client.Minecraft.getInstance().setScreen(
                new matteroverdrive.client.quest.DialogScreen(npc, net.minecraft.client.Minecraft.getInstance().player));
        matteroverdrive.dialog.DialogMessage.showOnClient = message -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen screen) {
                if (message == null) screen.onClose();
                else screen.setCurrent(message);
            }
        };
        matteroverdrive.dialog.DialogMessage.shownOnClient = () ->
                net.minecraft.client.Minecraft.getInstance().screen instanceof matteroverdrive.client.quest.DialogScreen screen ? screen.getCurrent() : null;
        matteroverdrive.android.AndroidClientHooks.transformationStarted = player -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (player == mc.player) {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(
                        matteroverdrive.init.MOSounds.TRANSFORMATION_MUSIC.get(), 1));
            }
        };        AnomalyParticles.setup();
        BoltHitEffects.setup();
        MachineSounds.setup();
        matteroverdrive.client.android.ShieldRenderer.setup();
    }

    public interface Screens {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, ScreenFactory<M, U> factory);
    }

    public interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, net.minecraft.world.entity.player.Inventory inventory, Component title);
    }

    public static void registerScreens(Screens screens) {
        screens.register(MOMenus.SOLAR_PANEL.get(), SolarPanelScreen::new);
        screens.register(MOMenus.WEAPON_STATION.get(), matteroverdrive.client.screen.WeaponStationScreen::new);
        screens.register(MOMenus.ANDROID_STATION.get(), matteroverdrive.client.screen.AndroidStationScreen::new);
        screens.register(MOMenus.CHARGING_STATION.get(), matteroverdrive.client.screen.ChargingStationScreen::new);
        screens.register(MOMenus.INSCRIBER.get(), InscriberScreen::new);
        screens.register(MOMenus.DECOMPOSER.get(), DecomposerScreen::new);
        screens.register(MOMenus.RECYCLER.get(), RecyclerScreen::new);
        screens.register(MOMenus.ANALYZER.get(), AnalyzerScreen::new);
        screens.register(MOMenus.PATTERN_STORAGE.get(), PatternStorageScreen::new);
        screens.register(MOMenus.PATTERN_MONITOR.get(), PatternMonitorScreen::new);
        screens.register(MOMenus.REPLICATOR.get(), ReplicatorScreen::new);
        screens.register(MOMenus.FUSION_REACTOR.get(), FusionReactorScreen::new);
        screens.register(MOMenus.TRANSPORTER.get(), matteroverdrive.client.screen.TransporterScreen::new);
        screens.register(MOMenus.ANDROID_SPAWNER.get(), matteroverdrive.client.screen.AndroidSpawnerScreen::new);
        screens.register(MOMenus.CONTRACT_MARKET.get(), matteroverdrive.client.screen.ContractMarketScreen::new);
        screens.register(MOMenus.STAR_MAP.get(), matteroverdrive.client.screen.StarMapScreen::new);    }

    public interface Renderers {
        <E extends Entity> void entity(EntityType<? extends E> type, EntityRendererProvider<E> provider);

        <T extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void blockEntity(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
    }

    public static void registerRenderers(Renderers renderers) {
        renderers.entity(matteroverdrive.init.MOEntities.PLASMA_BOLT.get(), PlasmaBoltRenderer::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.WEAPON_STATION.get(), WeaponStationRenderer::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.GRAVITATIONAL_STABILIZER.get(), StabilizerRenderer::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.STAR_MAP.get(), matteroverdrive.client.starmap.StarMapRenderer::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.REPLICATOR.get(), MachineRenderers.Replicator::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.INSCRIBER.get(), MachineRenderers.Inscriber::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.PATTERN_MONITOR.get(),
                c -> new MachineRenderers.Monitor<>(c, "pattern_monitor_holo"));
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.CONTRACT_MARKET.get(),
                c -> new MachineRenderers.Monitor<>(c, "contract_station_holo"));
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.HOLO_SIGN.get(), HoloSignRenderer::new);
        renderers.blockEntity(matteroverdrive.init.MOBlockEntities.GRAVITATIONAL_ANOMALY.get(), AnomalyRenderer::new);
        renderers.entity(matteroverdrive.init.MOEntities.ROGUE_ANDROID.get(), RogueAndroidRenderer::melee);
        renderers.entity(matteroverdrive.init.MOEntities.RANGED_ROGUE_ANDROID.get(), RogueAndroidRenderer::ranged);
        renderers.entity(matteroverdrive.init.MOEntities.MUTANT_SCIENTIST.get(), MutantScientistRenderer::new);
        renderers.entity(matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), MadScientistRenderer::new);
        renderers.entity(matteroverdrive.init.MOEntities.FAILED_PIG.get(), FailedAnimalRenderers::pig);
        renderers.entity(matteroverdrive.init.MOEntities.FAILED_COW.get(), FailedAnimalRenderers::cow);
        renderers.entity(matteroverdrive.init.MOEntities.FAILED_CHICKEN.get(), FailedAnimalRenderers::chicken);
        renderers.entity(matteroverdrive.init.MOEntities.FAILED_SHEEP.get(), FailedAnimalRenderers::sheep);    }

    public static void registerLayers(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> layers) {
        layers.accept(RogueAndroidRenderer.MELEE, RogueAndroidRenderer::meleeLayer);
        layers.accept(RogueAndroidRenderer.RANGED, RogueAndroidRenderer::rangedLayer);
        layers.accept(RogueAndroidRenderer.VISOR, RogueAndroidRenderer::visorLayer);
        layers.accept(MutantScientistRenderer.LAYER, HulkingScientistModel::createLayer);    }

    /** 1.7.10 BlockDecorative.colorMultiplier / BlockTritaniumCrate.getBlockColor: plates and crate overlays take their dye colour. */
    public static void blockColors(BiConsumer<BlockColor, Block> event) {
        var colors = net.minecraft.world.item.DyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            int rgb = colors[i].getTextureDiffuseColor();
            event.accept((state, level, pos, tint) -> rgb, matteroverdrive.init.MODecorative.COLORED_PLATES.get(i).get());
            event.accept((state, level, pos, tint) -> tint == 0 ? rgb : -1, matteroverdrive.init.MOBlocks.TRITANIUM_CRATES.get(i).get());
        }    }

    /** 1.21.4+: the item model select properties the client item definitions use */
    public static void registerSelectProperties(BiConsumer<ResourceLocation, net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty.Type<?, ?>> event) {
        event.accept(ResourceLocation.fromNamespaceAndPath(matteroverdrive.MatterOverdrive.MODID, "barrel"), BarrelProperty.TYPE);
    }

    public static void registerKeys(Consumer<KeyMapping> keys) {
        ScannerClient.registerKeys(keys);
        matteroverdrive.client.android.AndroidKeys.registerKeys(keys);
    }

    /** Extra block models the renderers draw */
    public static List<ResourceLocation> extraModels() {
        return MachineRenderers.extraModels();
    }

    /** 1.7.10 GalaxyClient.onClientTick: the client's galaxy runs its travel events too. */
    public static void onClientTickStart() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var galaxy = matteroverdrive.starmap.GalaxyClient.getGalaxy();
        if (galaxy != null && mc.level != null && !mc.isPaused() && mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            galaxy.update(mc.level);
        }
    }

    public static void onClientTickEnd() {
        WeaponClient.onClientTick();
        ScannerClient.onClientTick();
        matteroverdrive.client.quest.QuestHud.onClientTick();
        matteroverdrive.client.android.AndroidKeys.onClientTick();
        matteroverdrive.client.android.ShieldRenderer.onClientTick();
        DevScene.onClientTick();
    }

    /** Leaving a world forgets its galaxy. */
    public static void onLoggedOut() {
        matteroverdrive.starmap.GalaxyClient.setGalaxy(null);
    }

    public static void onTooltip(ItemStack stack, List<Component> tooltip) {
        MatterTooltip.onTooltip(stack, tooltip);
        MachineTooltip.onTooltip(stack, tooltip);
    }

    /** The HUD layers: the android HUD above the camera overlays, the quest HUD above the chat */
    public static final ResourceLocation ANDROID_HUD = ResourceLocation.fromNamespaceAndPath(matteroverdrive.MatterOverdrive.MODID, "android_hud");
    public static final ResourceLocation QUEST_HUD = ResourceLocation.fromNamespaceAndPath(matteroverdrive.MatterOverdrive.MODID, "quest_hud");

    private MOClient() {}
}
