package matteroverdrive.client.starmap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.GalaxyClient;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.Star;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.util.Mth;

/**
 * 1.7.10 GuiStarMap.drawWorldBackground: the star map's hologram behind the whole screen, in a 75 degree perspective
 * tilted 15 degrees, slowly turning on the galaxy / quadrant / star pages, pulled back to fit the zoomed body.
 */
public class StarMapPipRenderer extends PictureInPictureRenderer<StarMapPipRenderer.State> {
    private final PerspectiveProjectionMatrixBuffer projection = new PerspectiveProjectionMatrixBuffer("star map");

    public record State(StarMapBlockEntity starMap, float partialTick, int x0, int y0, int x1, int y1, float scale,
                        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        public State(StarMapBlockEntity starMap, float partialTick, int x0, int y0, int x1, int y1) {
            this(starMap, partialTick, x0, y0, x1, y1, 1, null, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null));
        }
    }

    public StarMapPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "star_map";
    }

    @Override
    protected void renderToTexture(State state, PoseStack ignored) {
        var mc = Minecraft.getInstance();
        Galaxy galaxy = GalaxyClient.getGalaxy();
        if (galaxy == null) return;
        StarMapBlockEntity starMap = state.starMap();
        float aspect = (float) (state.x1() - state.x0()) / Math.max(1, state.y1() - state.y0());
        RenderSystem.setProjectionMatrix(projection.getBuffer(new Matrix4f().setPerspective(75 * Mth.DEG_TO_RAD, aspect, 0.05f, 20)),
                ProjectionType.PERSPECTIVE);
        StarMapRenderer.State view = StarMapRenderer.stateOf(starMap, state.partialTick(), mc.player);
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
        var ctx = new StarMapRenderer.Ctx(view, pose, HoloSink.of(bufferSource, mc.font), mc.font, yaw, pitch, orientation, mc.player, galaxy);
        StarMapRenderer.renderHologram(ctx);
    }
}
