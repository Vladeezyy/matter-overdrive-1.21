package matteroverdrive.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.util.FormattedCharSequence;

/**
 * Stand-in for the 1.21.9+ SubmitNodeCollector: the renderers "submit" their geometry, which is drawn right away
 * into the frame's MultiBufferSource.
 */
public final class SubmitNodeCollector {
    private final MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    /** No draw order within a frame before 1.21.9. */
    public SubmitNodeCollector order(int order) {
        return this;
    }

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }

    public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
        renderer.render(pose.last(), buffers.getBuffer(type));
    }

    public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int light,
                           int color, int backgroundColor, int outlineColor) {
        Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, pose.last().pose(), buffers, mode, backgroundColor, light);
    }

    public void submitBlockModel(PoseStack pose, RenderType type, BlockStateModel model, float r, float g, float b, int light, int overlay,
                                 int outlineColor) {
        ModelBlockRenderer.renderModel(pose.last(), buffers.getBuffer(type), model, r, g, b, light, overlay);
    }
}
