package matteroverdrive.client.starmap;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.math.Axis;

import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.compat.render.SubmitNodeCollector;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.GalaxyClient;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.Star;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * 1.7.10 GuiStarMap.drawWorldBackground: the star map's hologram behind the whole screen, in a 75 degree perspective
 * tilted 15 degrees, slowly turning on the galaxy / quadrant / star pages, pulled back to fit the zoomed body.
 * (Before 1.21.6: drawn straight into the screen with its own projection; 1.21.6+ renders a picture-in-picture.)
 */
public final class StarMapPipRenderer {
    private StarMapPipRenderer() {}

    public static void render(StarMapBlockEntity starMap, float partialTick, int width, int height) {
        var mc = Minecraft.getInstance();
        Galaxy galaxy = GalaxyClient.getGalaxy();
        if (galaxy == null) return;
        float aspect = (float) width / Math.max(1, height);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setPerspective(75 * Mth.DEG_TO_RAD, aspect, 0.05f, 20), ProjectionType.PERSPECTIVE);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        clearDepth(mc);

        StarMapRenderer.State view = StarMapRenderer.stateOf(starMap, partialTick, mc.player);
        float rotation = starMap.getZoomLevel() <= 2 ? (float) (view.time * 0.1) : 0;
        float yaw = 180 + rotation, pitch = 15;
        Quaternionf orientation = new Quaternionf().rotationYXZ((float) Math.PI - yaw * Mth.DEG_TO_RAD, -pitch * Mth.DEG_TO_RAD, 0);
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(15));
        pose.translate(0, -0.8f, 0);
        switch (starMap.getZoomLevel()) {
            case 0 -> pose.translate(0, -1.1, -4);
            case 1 -> pose.translate(0, -0.6, -4);
            case 2 -> {
                Star star = galaxy.getStar(starMap.getDestination());
                float maxDistance = 0;
                if (star != null) for (Planet planet : star.getPlanets()) maxDistance = Math.max(maxDistance, planet.getOrbit());
                pose.translate(0, 0, -maxDistance * 3 - 1.5f);
            }
            default -> pose.translate(0, 0.1f, -3);
        }
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.5f, -1.8f, -0.5f);
        var buffers = mc.renderBuffers().bufferSource();
        var ctx = new StarMapRenderer.Ctx(view, pose, HoloSink.of(new SubmitNodeCollector(buffers)), mc.font, yaw, pitch, orientation, mc.player, galaxy);
        StarMapRenderer.renderHologram(ctx);
        buffers.endBatch();

        modelView.popMatrix();
        RenderSystem.restoreProjectionMatrix();
        clearDepth(mc);
    }

    private static void clearDepth(Minecraft mc) {
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), 1.0);
    }
}
