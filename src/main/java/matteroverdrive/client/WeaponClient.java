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
import net.neoforged.neoforge.network.PacketDistributor;

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
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem().getItem())
                && weapon.canFire(mc.player.getMainHandItem())) {
            PacketDistributor.sendToServer(new FireWeaponPayload(mc.player.isUsingItem()));
        }
    }

    /** 1.7.10 RenderWeaponsBeam.playWeaponSound: the omni tool hums while it digs (one looping sound per player). */
    private static final java.util.Map<net.minecraft.world.entity.player.Player, OmniToolHum> HUMS = new java.util.WeakHashMap<>();

    @SubscribeEvent
    static void onHum(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (var player : mc.level.players()) {
            OmniToolHum hum = HUMS.get(player);
            if (OmniToolHum.digging(player) && (hum == null || hum.isStopped())) {
                hum = new OmniToolHum(player);
                HUMS.put(player, hum);
                mc.getSoundManager().play(hum);
            }
        }
    }

    private static final class OmniToolHum extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private final net.minecraft.world.entity.player.Player player;

        OmniToolHum(net.minecraft.world.entity.player.Player player) {
            super(matteroverdrive.init.MOSounds.OMNI_TOOL_HUM.get(), net.minecraft.sounds.SoundSource.PLAYERS, player.getRandom());
            this.player = player;
            this.looping = true;
            this.volume = 0.06f + player.getRandom().nextFloat() * 0.04f;
            this.pitch = 0.95f + player.getRandom().nextFloat() * 0.1f;
            tick();
        }

        static boolean digging(net.minecraft.world.entity.player.Player player) {
            return player.isUsingItem() && player.getUseItem().getItem() instanceof matteroverdrive.item.weapon.OmniToolItem;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !digging(player)) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
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
