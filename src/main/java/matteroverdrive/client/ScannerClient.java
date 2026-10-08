package matteroverdrive.client;

import com.mojang.blaze3d.platform.InputConstants;
import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.android.AndroidKeys;
import matteroverdrive.client.screen.MatterScannerScreen;
import matteroverdrive.init.MOSounds;
import matteroverdrive.item.MatterScannerItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Matter scanner client side: the C key opens its screen (1.7.10 KeyHandler.MATTER_SCANNER_KEY), the scanning sound. */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class ScannerClient {
    public static final KeyMapping OPEN = new KeyMapping("key." + MatterOverdrive.MODID + ".matter_scanner", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C, AndroidKeys.CATEGORY);

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
        MatterScannerItem.scannerKeyName = () -> OPEN.getTranslatedKeyMessage().getString();
        MatterScannerItem.startScanSound = player -> Minecraft.getInstance().getSoundManager().play(new ScanSound(player));
    }

    /** 1.7.10 MatterScanner.DisplayGuiScreen: the held scanner, else the first one in the inventory. */
    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            var inventory = mc.player.getInventory();
            int slot = inventory.getSelectedItem().getItem() instanceof MatterScannerItem ? inventory.getSelectedSlot() : -1;
            for (int i = 0; slot < 0 && i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).getItem() instanceof MatterScannerItem) slot = i;
            }
            if (slot >= 0) mc.setScreen(new MatterScannerScreen(slot));
        }
    }

    /** 1.7.10 scanner_scanning MachineSound: plays while the scan lasts. */
    private static final class ScanSound extends AbstractTickableSoundInstance {
        private final Player player;

        ScanSound(Player player) {
            super(MOSounds.SCANNER_SCANNING.get(), SoundSource.PLAYERS, player.getRandom());
            this.player = player;
            this.looping = true;
            this.volume = 0.6f;
            tick();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof MatterScannerItem
                    || player.getUseItem().getItem() instanceof matteroverdrive.item.DataPadItem)) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
        }
    }

    private ScannerClient() {}
}
