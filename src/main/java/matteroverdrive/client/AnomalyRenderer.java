package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import net.minecraft.client.renderer.RenderType;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import matteroverdrive.compat.render.BlockEntityRenderState;
import matteroverdrive.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 TileEntityRendererGravitationalAnomaly: a solid black sphere the size of the event horizon (pulsing by
 * 10%), and in front of it a camera-facing plane twice that size with the core and glow textures, turning with the
 * anomaly's strength.
 */
public class AnomalyRenderer implements matteroverdrive.compat.render.StateBlockEntityRenderer<GravitationalAnomalyBlockEntity, AnomalyRenderer.State> {
    private static final ResourceLocation BLACK = tex("black");
    private static final ResourceLocation CORE = tex("gravitational_anomaly_core");
    private static final ResourceLocation GLOW = tex("gravitational_anomaly_glow");
    private static final int STACKS = 12, SLICES = 16;

    public AnomalyRenderer(BlockEntityRendererProvider.Context context) {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/block/" + name + ".png");
    }

    public static class State extends BlockEntityRenderState {
        float radius;
        float spin;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GravitationalAnomalyBlockEntity anomaly, State state, float partialTick, Vec3 camera,
                                   @Nullable Object crumbling) {
        matteroverdrive.compat.render.StateBlockEntityRenderer.super.extractRenderState(anomaly, state, partialTick, camera, crumbling);
        float time = anomaly.getLevel() == null ? 0 : anomaly.getLevel().getGameTime() + partialTick;
        double horizon = anomaly.getEventHorizon();
        state.radius = (float) (horizon * Math.sin(time * 0.2) * 0.1 + horizon * 0.9);
        // 1.7.10 rotated by worldTime x getBreakStrength() (real mass x 4 x suppression) degrees
        state.spin = (float) (time * anomaly.getRealMass() * 4 * anomaly.getSuppression() % 360);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        float r = state.radius;
        // 1.7.10 sphere.obj has radius 0.5, scaled by the (pulsing) event horizon
        collector.submitCustomGeometry(pose, RenderType.entityCutoutNoCull(BLACK), (p, vc) -> sphere(vc, p, r * 0.5f));
        // the glowing plane: twice the sphere, facing the camera, spinning about the view axis
        pose.mulPose(camera.orientation);
        pose.mulPose(Axis.ZP.rotationDegrees(state.spin));
        float half = r;     // a unit plane scaled by 2 x horizon
        // the core is a black disc, the glow a half-transparent white ring; both alpha blended (1.7.10 GL_SRC_ALPHA,
        // GL_ONE_MINUS_SRC_ALPHA) and unlit (lighting disabled)
        collector.submitCustomGeometry(pose, matteroverdrive.compat.render.RenderTypes.entityTranslucentEmissive(CORE), (p, vc) -> plane(vc, p, half));
        collector.submitCustomGeometry(pose, matteroverdrive.compat.render.RenderTypes.entityTranslucentEmissive(GLOW), (p, vc) -> plane(vc, p, half));
        pose.popPose();
    }

    private static void sphere(VertexConsumer vc, PoseStack.Pose pose, float r) {
        for (int i = 0; i < STACKS; i++) {
            double t0 = Math.PI * i / STACKS, t1 = Math.PI * (i + 1) / STACKS;
            for (int j = 0; j < SLICES; j++) {
                double p0 = 2 * Math.PI * j / SLICES, p1 = 2 * Math.PI * (j + 1) / SLICES;
                vertex(vc, pose, r, t0, p0);
                vertex(vc, pose, r, t1, p0);
                vertex(vc, pose, r, t1, p1);
                vertex(vc, pose, r, t0, p1);
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float r, double theta, double phi) {
        float x = (float) (Math.sin(theta) * Math.cos(phi)), y = (float) Math.cos(theta), z = (float) (Math.sin(theta) * Math.sin(phi));
        vc.addVertex(pose, x * r, y * r, z * r).setColor(0xFF000000).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, x, y, z);
    }

    /** Both windings, so it shows whichever way the camera rotation leaves it. */
    private static void plane(VertexConsumer vc, PoseStack.Pose pose, float half) {
        corner(vc, pose, -half, -half, 0, 1);
        corner(vc, pose, half, -half, 1, 1);
        corner(vc, pose, half, half, 1, 0);
        corner(vc, pose, -half, half, 0, 0);
        corner(vc, pose, -half, half, 0, 0);
        corner(vc, pose, half, half, 1, 0);
        corner(vc, pose, half, -half, 1, 1);
        corner(vc, pose, -half, -half, 0, 1);
    }

    private static void corner(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v) {
        vc.addVertex(pose, x, y, 0).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(pose, 0, 0, 1);
    }

    /** The sphere and plane reach past the block when the anomaly is heavy. */
    @Override
    public AABB getRenderBoundingBox(GravitationalAnomalyBlockEntity anomaly) {
        double r = anomaly.getEventHorizon() * 2 + 1;
        return new AABB(anomaly.getBlockPos()).inflate(r);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
