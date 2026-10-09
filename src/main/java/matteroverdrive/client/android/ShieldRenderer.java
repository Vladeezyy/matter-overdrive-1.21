package matteroverdrive.client.android;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidClientHooks;
import matteroverdrive.android.AndroidData;
import matteroverdrive.android.BioticStats;
import matteroverdrive.client.ObjMesh;
import matteroverdrive.client.starmap.HoloRenderTypes;
import matteroverdrive.init.MOSounds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import matteroverdrive.compat.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 1.7.10 BioticStatRendererShield: an android's active shield is a holo-blue additive bubble around the player (the
 * shield mesh, a forcefield sphere and a slowly turning plasma sphere), each hit flashing a damage spot towards the
 * attacker; the own bubble fades in and out. Plus the shield's loop sound and the 1.7.10 BioticStatRendererTeleporter
 * marker: a spinning glow where the teleport would land while the ability key is held.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class ShieldRenderer {
    private static final int HOLO = 0xA9E2FB;
    private static final ResourceLocation SHIELD = fx("shield"), DAMAGE = fx("shield_damage"),
            FORCEFIELD = fx("forcefield_plasma"), PLASMA = fx("forcefield_plasma_2");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/block/gravitational_anomaly_glow.png");
    private static final int HIT_TIME = 10;
    private static ObjMesh shieldMesh, sphere;
    /** The own bubble's fade (1.7.10 opacityLerp); other players' bubbles are always at full strength. */
    private static float opacity;
    /** Per player id: hit directions waiting to fade, only the first one counting down (1.7.10 TAG_HITS). */
    private static final Map<Integer, Deque<Hit>> HITS = new HashMap<>();
    private static SoundInstance loop;

    private static final class Hit {
        final Vec3 offset;
        int time = HIT_TIME;

        Hit(Vec3 offset) {
            this.offset = offset;
        }
    }

    private static ResourceLocation fx(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/fx/" + name + ".png");
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        AndroidClientHooks.shieldHit = (id, offset) -> HITS.computeIfAbsent(id, k -> new ArrayDeque<>()).add(new Hit(offset));
    }

    private static boolean shieldOn(Player player) {
        AndroidData data = Android.get(player);
        return data.isAndroid() && BioticStats.SHIELD.isActive(player, data, 1);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            HITS.clear();
            opacity = 0;
            return;
        }
        boolean on = shieldOn(mc.player);
        opacity = on ? Math.min(1, opacity + 0.1f) : Math.max(0, opacity - 0.2f);
        HITS.entrySet().removeIf(e -> {
            Deque<Hit> hits = e.getValue();
            if (!(mc.level.getEntity(e.getKey()) instanceof Player p) || !shieldOn(p)) return true;   // 1.7.10 dropped the hits with the shield
            Hit first = hits.peek();
            if (first != null && first.time-- <= 0) hits.poll();
            return hits.isEmpty();
        });
        // the shield's hum while the own shield is up (1.7.10 played it at the world origin by mistake)
        if (on && (loop == null || !mc.getSoundManager().isActive(loop))) {
            loop = new ShieldLoop(mc.player);
            mc.getSoundManager().play(loop);
        }
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean drew = false;
        for (Player player : mc.level.players()) {
            boolean own = player == mc.player;
            float strength = own ? opacity : 1;
            if (!(own ? opacity > 0 || shieldOn(player) : shieldOn(player)) || strength <= 0) continue;
            renderShield(mc, pose, buffers, player, camera, partial, own, strength);
            drew = true;
        }
        if (AndroidKeys.isAimingTeleport() && AndroidKeys.ABILITY_USE.isDown()
                && Android.get(mc.player).isUnlocked(BioticStats.TELEPORT, BioticStats.TELEPORT.maxLevel())) {
            Vec3 target = AndroidKeys.teleportTarget(mc.player);
            if (target != null) {
                renderTeleportMarker(mc, pose, buffers, target, camera);
                drew = true;
            }
        }
        if (drew) buffers.endBatch();
    }

    private static void renderShield(Minecraft mc, PoseStack pose, MultiBufferSource buffers, Player player, Vec3 camera, float partial,
                                     boolean own, float strength) {
        if (shieldMesh == null) {
            shieldMesh = ObjMesh.load(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "models/fx/shield_sphere.obj"));
            sphere = ObjMesh.load(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "models/fx/sphere.obj"));
        }
        double time = mc.level.getDayTime();
        Vec3 pos = player.getPosition(partial);
        // 1.7.10: the own bubble sat 0.5 below the eyes, other players' 1 below the top of their head
        double centre = own ? player.getEyeHeight() - 0.5 : player.getBbHeight() - 1;
        pose.pushPose();
        pose.translate(pos.x - camera.x, pos.y + centre - camera.y, pos.z - camera.z);
        pose.scale(3, 3, 3);
        Vec3 motion = player.getDeltaMovement();
        pose.mulPose(Axis.XN.rotationDegrees((float) (motion.z * 45)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (motion.x * 45)));
        // seen from outside the shield mesh shows its front faces only; from inside (first person) all of them
        boolean outside = !own || mc.options.getCameraType() != CameraType.FIRST_PERSON;
        RenderType shieldType = outside ? HoloRenderTypes.texturedCulled(SHIELD) : HoloRenderTypes.textured(SHIELD);
        shieldMesh.render(buffers.getBuffer(shieldType), pose.last(), holo(0.2f * strength));

        Deque<Hit> hits = HITS.get(player.getId());
        if (hits != null) {
            for (Hit hit : hits) {
                Vector3f dir = new Vector3f((float) hit.offset.x, (float) -hit.offset.y, (float) -hit.offset.z);
                if (dir.lengthSquared() < 1e-6f) continue;
                dir.normalize();
                // turn the damage spot (the sphere's +x) towards the hit, as 1.7.10 renderAttack
                Vector3f axis = dir.cross(new Vector3f(1, 0, 0), new Vector3f());
                pose.pushPose();
                if (axis.lengthSquared() > 1e-8f) {
                    pose.mulPose(new Quaternionf().rotationAxis((float) Math.acos(Math.max(-1, Math.min(1, dir.x))), axis.normalize()));
                }
                sphere.render(buffers.getBuffer(HoloRenderTypes.textured(DAMAGE)), pose.last(), holo(hit.time / (float) HIT_TIME * strength));
                pose.popPose();
            }
        }

        pose.scale(1.02f, 1.02f, 1.02f);
        sphere.render(buffers.getBuffer(HoloRenderTypes.textured(FORCEFIELD)), pose.last(), holo(0.1f * strength));
        pose.pushPose();
        Vector3f spin = new Vector3f((float) Math.sin(time * 0.01), (float) Math.cos(time * 0.01), 0).normalize();
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(time * 0.005), spin));
        pose.scale(1.01f, 1.01f, 1.01f);
        sphere.render(buffers.getBuffer(HoloRenderTypes.textured(PLASMA)), pose.last(), holo(0.05f * strength));
        pose.popPose();
        pose.popPose();
    }

    /** 1.7.10 BioticStatRendererTeleporter: a 1-block glow facing the player's view, spinning, at the landing spot. */
    private static void renderTeleportMarker(Minecraft mc, PoseStack pose, MultiBufferSource buffers, Vec3 target, Vec3 camera) {
        Player player = mc.player;
        pose.pushPose();
        pose.translate(target.x - camera.x, target.y - camera.y, target.z - camera.z);
        pose.mulPose(Axis.YN.rotationDegrees(player.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(player.getXRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(mc.level.getDayTime() * 10 % 360));
        pose.translate(-0.5f, -0.5f, 0);
        var vc = buffers.getBuffer(HoloRenderTypes.textured(GLOW));
        int color = holo(0.5f);
        PoseStack.Pose last = pose.last();
        vc.addVertex(last, 0, 0, 0).setUv(0, 1).setColor(color);
        vc.addVertex(last, 1, 0, 0).setUv(1, 1).setColor(color);
        vc.addVertex(last, 1, 1, 0).setUv(1, 0).setColor(color);
        vc.addVertex(last, 0, 1, 0).setUv(0, 0).setColor(color);
        pose.popPose();
    }

    /** 1.7.10 RenderUtils.applyColorWithMultipy(COLOR_HOLO, m): additive, so the brightness is the strength. */
    private static int holo(float multiply) {
        float m = Math.max(0, Math.min(1, multiply));
        return ARGB.color(255, Math.round(ARGB.red(HOLO) * m), Math.round(ARGB.green(HOLO) * m), Math.round(ARGB.blue(HOLO) * m));
    }

    /** 1.7.10 shield_loop, 0.3-0.5 volume, following the player while the shield is up. */
    private static final class ShieldLoop extends AbstractTickableSoundInstance {
        private final Player player;

        ShieldLoop(Player player) {
            super(MOSounds.SHIELD_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.3f + random.nextFloat() * 0.2f;
            tick();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !shieldOn(player)) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
        }
    }

    private ShieldRenderer() {}
}
