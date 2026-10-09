package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.OmniToolItem;
import matteroverdrive.item.weapon.PhaserItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * 1.7.10 RenderWeaponsBeam: while a player holds a phaser's use key, a glowing plasmabeam ribbon runs from the
 * phaser to what it hits, in the weapon's colour, pulsing over time. Beams are submitted with the player model
 * (third person, other players) or with the first-person hand.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class PhaserBeamRenderer {
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/fx/plasmabeam.png");
    private static final float WIDTH = 0.06f;

    /** World-space beam relative to the player's render origin. */
    @SubscribeEvent
    static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(event.getRenderState().id) instanceof Player player)) return;
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof EnergyWeaponItem phaser) || !isBeam(phaser)) return;
        float partial = event.getPartialTick();
        Vec3 origin = player.getPosition(partial);
        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 start = eye.add(look.scale(0.7)).add(right.scale(0.35)).add(0, -0.25, 0).subtract(origin);
        Vec3 end = target(mc, player, phaser).subtract(origin);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition().subtract(origin);
        submit(new SubmitNodeCollector(event.getMultiBufferSource()), event.getPoseStack(), start, end, camera, phaser, player);
    }

    /** First person: camera space, the crosshair looks down -z. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (event.getHand() != InteractionHand.MAIN_HAND || player == null || mc.level == null || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof EnergyWeaponItem phaser) || !isBeam(phaser)) return;
        double distance = target(mc, player, phaser).distanceTo(player.getEyePosition(event.getPartialTick()));
        Vec3 start = new Vec3(0.36, -0.16, -0.7);
        Vec3 end = new Vec3(0, 0, -distance);
        submit(new SubmitNodeCollector(event.getMultiBufferSource()), event.getPoseStack(), start, end, Vec3.ZERO, phaser, player);
    }

    /** 1.7.10 RenderWeaponsBeam: the phaser's beam and the omni tool's digging beam. */
    private static boolean isBeam(EnergyWeaponItem weapon) {
        return weapon instanceof PhaserItem || weapon instanceof OmniToolItem;
    }

    /** Where the beam ends: the phaser stops at entities too, the omni tool only at blocks. */
    private static Vec3 target(Minecraft mc, Player player, EnergyWeaponItem weapon) {
        return weapon instanceof OmniToolItem ? OmniToolItem.traceBlock(mc.level, player).getLocation()
                : PhaserItem.trace(mc.level, player, weapon.getRange(player.getUseItem())).getLocation();
    }

    private static void submit(SubmitNodeCollector collector, PoseStack pose, Vec3 start, Vec3 end, Vec3 camera, EnergyWeaponItem phaser, Player player) {
        long time = player.level().getGameTime();
        float pulse = 0.5f + (float) (1 + Math.sin(time * 0.5)) * 0.25f;
        int color = ARGB.color(Math.round(255 * pulse), phaser.getColor(player.getUseItem()));
        RenderType type = RenderType.energySwirl(BEAM, 0, 0);
        collector.submitCustomGeometry(pose, type, (p, vc) -> ribbon(vc, p, start, end, camera, color));
    }

    /** A quad from start to end, turned to face the camera. */
    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, Vec3 start, Vec3 end, Vec3 camera, int color) {
        Vec3 axis = end.subtract(start);
        Vec3 side = axis.cross(start.subtract(camera)).normalize().scale(WIDTH);
        // plasmabeam.png runs vertically: u across the beam, v along it (textures clamp, so v stays in 0..1)
        vertex(vc, pose, start.add(side), color, 0, 0);
        vertex(vc, pose, start.subtract(side), color, 1, 0);
        vertex(vc, pose, end.subtract(side), color, 1, 1);
        vertex(vc, pose, end.add(side), color, 0, 1);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, int color, float u, float v) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
    }

    private PhaserBeamRenderer() {}
}
