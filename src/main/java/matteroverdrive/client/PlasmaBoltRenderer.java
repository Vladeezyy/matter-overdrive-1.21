package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.PlasmaBolt;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

/**
 * 1.7.10 EntityRendererPhaserFire: two crossed, additive quads with PlasmaFire.png, tinted with the bolt's colour,
 * fading out over the bolt's range.
 */
public class PlasmaBoltRenderer extends EntityRenderer<PlasmaBolt, PlasmaBoltRenderer.State> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/plasma_fire.png");

    public static class State extends EntityRenderState {
        float yRot, xRot, size, life, speed;
        int color;
    }

    public PlasmaBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected int getBlockLightLevel(PlasmaBolt entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void extractRenderState(PlasmaBolt bolt, State state, float partialTick) {
        super.extractRenderState(bolt, state, partialTick);
        state.yRot = bolt.getYRot(partialTick);
        state.xRot = bolt.getXRot(partialTick);
        state.size = bolt.getRenderSize();
        state.life = bolt.getLife();
        state.color = bolt.getColor();
        state.speed = (float) bolt.getDeltaMovement().length();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yRot - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        float alpha = 1 - (1 - state.life) * (1 - state.life);        // 1.7.10 Quad.easeOut over the life
        int color = ARGB.color(Math.round(255 * alpha), state.color);
        // 1.7.10: length (6 x speed + 10) and width renderSize, both scaled by 0.05625
        float s = Math.max(0.06f, state.size * 0.2f), length = (6 * state.speed + 10) * 0.05625f;
        // additive like 1.7.10 (GL_ONE, GL_ONE): the texture's black background adds nothing
        collector.submitCustomGeometry(pose, RenderType.energySwirl(TEXTURE, 0, 0), (p, vc) -> {
            for (int i = 0; i < 2; i++) {
                float a = i == 0 ? s : 0, b = i == 0 ? 0 : s;
                quad(vc, p, color, -length / 2, a, b, length / 2, -a, -b);
            }
        });
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int color, float x0, float y0, float z0, float x1, float y1, float z1) {
        vertex(vc, p, color, x0, -y0, -z0, 0, 0);
        vertex(vc, p, color, x1, -y0, -z0, 1, 0);
        vertex(vc, p, color, x1, y0, z0, 1, 1);
        vertex(vc, p, color, x0, y0, z0, 0, 1);
        // back face
        vertex(vc, p, color, x0, y0, z0, 0, 1);
        vertex(vc, p, color, x1, y0, z0, 1, 1);
        vertex(vc, p, color, x1, -y0, -z0, 1, 0);
        vertex(vc, p, color, x0, -y0, -z0, 0, 0);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, int color, float x, float y, float z, float u, float v) {
        vc.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
