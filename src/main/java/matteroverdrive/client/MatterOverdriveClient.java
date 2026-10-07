package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.screen.InscriberScreen;
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
    }
}
