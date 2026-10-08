package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.network.FireWeaponPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client side of the energy weapons (1.7.10 ClientWeaponHandler): the attack key fires instead of swinging, and
 * holding use zooms by the weapon's zoom factor.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class WeaponClient {
    @SubscribeEvent
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isAttack() && mc.player != null && mc.player.getMainHandItem().getItem() instanceof EnergyWeaponItem) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.keyAttack.isDown()) return;
        if (mc.player.getMainHandItem().getItem() instanceof EnergyWeaponItem weapon
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem())
                && weapon.canFire(mc.player.getMainHandItem())) {
            ClientPacketDistributor.sendToServer(new FireWeaponPayload(mc.player.isUsingItem()));
        }
    }

    @SubscribeEvent
    static void onFov(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof EnergyWeaponItem weapon && weapon.getZoom(player.getUseItem()) > 0) {
            event.setNewFovModifier(event.getNewFovModifier() * (1 - weapon.getZoom(player.getUseItem())));
        }
    }

    private WeaponClient() {}
}
