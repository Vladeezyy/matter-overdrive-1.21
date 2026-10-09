package matteroverdrive.compat;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.1 stand-ins for the 1.21.6+ GuiGraphics calls the screens use: {@code blit(RenderPipelines.GUI_TEXTURED, ...)}
 * (translucent, optionally tinted with an ARGB colour) and the deferred {@code setTooltipForNextFrame}.
 */
public final class Gui {
    private Gui() {}

    private static void begin(int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(ARGB.red(color) / 255f, ARGB.green(color) / 255f, ARGB.blue(color) / 255f, ARGB.alpha(color) / 255f);
    }

    private static void end() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /** blit(GUI_TEXTURED, tex, x, y, u, v, w, h, texW, texH). */
    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        blit(g, tex, x, y, u, v, w, h, texW, texH, -1);
    }

    /** blit(GUI_TEXTURED, tex, x, y, u, v, w, h, texW, texH, color). */
    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int texW, int texH, int color) {
        begin(color);
        g.blit(tex, x, y, u, v, w, h, texW, texH);
        end();
    }

    /** blit(GUI_TEXTURED, tex, x, y, u, v, w, h, regionW, regionH, texW, texH): the region is scaled to w x h. */
    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
                            int texW, int texH) {
        blit(g, tex, x, y, u, v, w, h, regionW, regionH, texW, texH, -1);
    }

    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
                            int texW, int texH, int color) {
        begin(color);
        g.blit(tex, x, y, w, h, u, v, regionW, regionH, texW, texH);
        end();
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        blitSprite(g, sprite, x, y, w, h, -1);
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, int color) {
        begin(color);
        TextureAtlasSprite atlasSprite = Minecraft.getInstance().getGuiSprites().getSprite(sprite);
        if (Minecraft.getInstance().getGuiSprites().getSpriteScaling(atlasSprite) instanceof GuiSpriteScaling.NineSlice nine
                && nine.border().left() != nine.border().right()) {
            nineSlice(g, atlasSprite, nine, x, y, w, h);
        } else {
            g.blitSprite(sprite, x, y, w, h);
        }
        end();
    }

    /**
     * 1.21.1's GuiGraphics.blitNineSlicedSprite draws the right edge as wide as the left border,
     * so sprites with different left / right borders get a stray column. This one draws the nine parts (edges and centre
     * tiled, 1 px rows / columns as one stretched quad) into a single buffer.
     */
    private static void nineSlice(GuiGraphics g, TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice nine, int x, int y, int w, int h) {
        var border = nine.border();
        int sw = nine.width(), sh = nine.height();
        int l = Math.min(border.left(), w / 2), r = Math.min(border.right(), w / 2);
        int t = Math.min(border.top(), h / 2), b = Math.min(border.bottom(), h / 2);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = g.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int[][] cols = {{x, l, 0, l}, {x + l, w - l - r, l, sw - l - r}, {x + w - r, r, sw - r, r}};
        int[][] rows = {{y, t, 0, t}, {y + t, h - t - b, t, sh - t - b}, {y + h - b, b, sh - b, b}};
        for (int[] c : cols) {
            for (int[] rw : rows) {
                tile(buffer, matrix, sprite, sw, sh, c[0], rw[0], c[1], rw[1], c[2], rw[2], c[3], rw[3]);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /** Fills w x h at (x, y) with the sprite region (u, v, uw, vh), tiled 1:1 (a 1 px region is stretched). */
    private static void tile(BufferBuilder buffer, Matrix4f matrix, TextureAtlasSprite sprite, int sw, int sh, int x, int y, int w, int h,
                             int u, int v, int uw, int vh) {
        if (w <= 0 || h <= 0 || uw <= 0 || vh <= 0) return;
        int stepX = uw == 1 ? w : uw, stepY = vh == 1 ? h : vh;
        for (int i = 0; i < w; i += stepX) {
            int cw = Math.min(stepX, w - i);
            float u0 = sprite.getU((float) u / sw), u1 = sprite.getU((float) (u + (uw == 1 ? 1 : cw)) / sw);
            for (int k = 0; k < h; k += stepY) {
                int ch = Math.min(stepY, h - k);
                float v0 = sprite.getV((float) v / sh), v1 = sprite.getV((float) (v + (vh == 1 ? 1 : ch)) / sh);
                float x0 = x + i, x1 = x + i + cw, y0 = y + k, y1 = y + k + ch;
                buffer.addVertex(matrix, x0, y0, 0).setUv(u0, v0);
                buffer.addVertex(matrix, x0, y1, 0).setUv(u0, v1);
                buffer.addVertex(matrix, x1, y1, 0).setUv(u1, v1);
                buffer.addVertex(matrix, x1, y0, 0).setUv(u1, v0);
            }
        }
    }

    /** Deferred to the end of the screen's render like 1.21.10 (drawn at the mouse); drawn now outside a screen. */
    public static void setTooltipForNextFrame(GuiGraphics g, Font font, Component text, int x, int y) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.setTooltipForNextRenderPass(font.split(text, Math.max(screen.width / 2, 170)));
        } else {
            g.renderTooltip(font, text, x, y);
        }
    }

    public static void setTooltipForNextFrame(GuiGraphics g, Font font, List<FormattedCharSequence> lines, int x, int y) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.setTooltipForNextRenderPass(lines);
        } else {
            g.renderTooltip(font, lines, x, y);
        }
    }

    public static void setTooltipForNextFrame(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        List<FormattedCharSequence> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), stack).stream()
                .map(Component::getVisualOrderText).toList();
        setTooltipForNextFrame(g, font, lines, x, y);
    }

    public static void setComponentTooltipForNextFrame(GuiGraphics g, Font font, List<Component> lines, int x, int y) {
        setTooltipForNextFrame(g, font, lines.stream().map(Component::getVisualOrderText).toList(), x, y);
    }
}
