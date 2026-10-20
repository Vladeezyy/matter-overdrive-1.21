package matteroverdrive.client.starmap;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** Where the hologram draws: the world (block entity submit), a picture-in-picture texture (star map screen) or the GUI. */
public interface HoloSink {
    void geometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer);

    void text(PoseStack pose, float x, float y, FormattedCharSequence text, int argb);

    default void icon(PoseStack pose, ResourceLocation texture, float x, float y, float w, float h, int rgb) {
        geometry(pose, HoloRenderTypes.textured(texture), (p, vc) -> Holo.icon(p, vc, x, y, w, h, rgb));
    }

    default void fill(PoseStack pose, float x0, float y0, float x1, float y1, int rgb) {
        geometry(pose, HoloRenderTypes.COLOR_QUADS, (p, vc) -> {
            float r = Holo.r(rgb), g = Holo.g(rgb), b = Holo.b(rgb);
            vc.addVertex(p, x0, y0, 0).setColor(r, g, b, 1);
            vc.addVertex(p, x1, y0, 0).setColor(r, g, b, 1);
            vc.addVertex(p, x1, y1, 0).setColor(r, g, b, 1);
            vc.addVertex(p, x0, y1, 0).setColor(r, g, b, 1);
        });
    }

    /** A 16 px GUI item at (x, y) of the current (y down) panel frame. */
    void item(PoseStack pose, net.minecraft.world.item.ItemStack stack, float x, float y);

        static HoloSink of(SubmitNodeCollector collector) {
        return new HoloSink() {
            @Override
            public void geometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
                collector.submitCustomGeometry(pose, type, renderer);
            }

            @Override
            public void text(PoseStack pose, float x, float y, FormattedCharSequence text, int argb) {
                collector.submitText(pose, x, y, text, false, Font.DisplayMode.NORMAL, 0xF000F0, argb, 0, 0);
            }

            @Override
            public void item(PoseStack pose, net.minecraft.world.item.ItemStack stack, float x, float y) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                var state = new matteroverdrive.compat.render.ItemStackRenderState();
                matteroverdrive.compat.render.ItemModelResolver.INSTANCE.updateForTopItem(state, stack, net.minecraft.world.item.ItemDisplayContext.GUI, mc.level, null, 0);
                if (state.isEmpty()) return;
                pose.pushPose();
                pose.translate(x + 8, y + 8, 0);
                pose.scale(16, -16, 16);
                state.submit(pose, collector, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        };
    }

    /** 2D GUI drawing; the PoseStack is ignored (the GUI info panels are drawn untransformed). */
    static HoloSink of(GuiGraphics g, Font font) {
        return new HoloSink() {
            @Override
            public void geometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {}

            @Override
            public void text(PoseStack pose, float x, float y, FormattedCharSequence text, int argb) {
                g.drawString(font, text, Math.round(x), Math.round(y), argb, false);
            }

            @Override
            public void icon(PoseStack pose, ResourceLocation texture, float x, float y, float w, float h, int rgb) {
                matteroverdrive.compat.Gui.blit(g, texture, Math.round(x), Math.round(y), 0, 0, Math.round(w), Math.round(h),
                        Math.round(w), Math.round(h), 0xFF000000 | rgb);
            }

            @Override
            public void item(PoseStack pose, net.minecraft.world.item.ItemStack stack, float x, float y) {
                g.renderItem(stack, Math.round(x), Math.round(y));
            }

            @Override
            public void fill(PoseStack pose, float x0, float y0, float x1, float y1, int rgb) {
                g.fill(Math.round(Math.min(x0, x1)), Math.round(Math.min(y0, y1)), Math.round(Math.max(x0, x1)), Math.round(Math.max(y0, y1)),
                        0xFF000000 | rgb);
            }
        };
    }
}
