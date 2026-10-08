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
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MOMenus.SOLAR_PANEL.get(), SolarPanelScreen::new);
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
    static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(matteroverdrive.init.MOEntities.PLASMA_BOLT.get(), PlasmaBoltRenderer::new);
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
    }
}
