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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 stand-ins for the 1.21.6+ GuiGraphics calls the screens use: {@code blit(RenderPipelines.GUI_TEXTURED, ...)}
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

    /**
     * 1.20.1 has no GUI sprite atlas (1.20.2+): a sprite is its own texture {@code textures/gui/sprites/<path>.png}, and
     * its {@code gui.scaling} (stretch / tile / nine_slice) is read from the .mcmeta here, once per sprite.
     */
    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, int color) {
        SpriteInfo info = SPRITES.computeIfAbsent(sprite, Gui::loadSprite);
        begin(color);
        RenderSystem.setShaderTexture(0, info.texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = g.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int sw = info.width, sh = info.height;
        if (info.border != null) {
            int[] border = info.border;
            int l = Math.min(border[0], w / 2), t = Math.min(border[1], h / 2);
            int r = Math.min(border[2], w / 2), b = Math.min(border[3], h / 2);
            int[][] cols = {{x, l, 0, l}, {x + l, w - l - r, l, sw - l - r}, {x + w - r, r, sw - r, r}};
            int[][] rows = {{y, t, 0, t}, {y + t, h - t - b, t, sh - t - b}, {y + h - b, b, sh - b, b}};
            for (int[] c : cols) {
                for (int[] rw : rows) {
                    tile(buffer, matrix, sw, sh, c[0], rw[0], c[1], rw[1], c[2], rw[2], c[3], rw[3]);
                }
            }
        } else if (info.tile) {
            tile(buffer, matrix, sw, sh, x, y, w, h, 0, 0, sw, sh);
        } else {
            quad(buffer, matrix, x, y, x + w, y + h, 0, 0, 1, 1);
        }
        BufferUploader.drawWithShader(buffer.end());
        end();
    }

    private static final java.util.Map<ResourceLocation, SpriteInfo> SPRITES = new java.util.concurrent.ConcurrentHashMap<>();

    /** Forgets the sprites' sizes and scaling (resource reload) */
    public static void clearSprites() {
        SPRITES.clear();
    }

    private record SpriteInfo(ResourceLocation texture, int width, int height, int @org.jetbrains.annotations.Nullable [] border, boolean tile) {}

    private static SpriteInfo loadSprite(ResourceLocation sprite) {
        ResourceLocation texture = new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
        int width = 16, height = 16;
        int[] border = null;
        boolean tile = false;
        var manager = Minecraft.getInstance().getResourceManager();
        try (var in = manager.open(texture)) {
            byte[] header = in.readNBytes(24);
            width = java.nio.ByteBuffer.wrap(header, 16, 4).getInt();
            height = java.nio.ByteBuffer.wrap(header, 20, 4).getInt();
        } catch (java.io.IOException | RuntimeException e) {
            matteroverdrive.MatterOverdrive.LOGGER.warn("Missing GUI sprite {}", texture);
        }
        var meta = manager.getResource(new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta"));
        if (meta.isPresent()) {
            try (var reader = meta.get().openAsReader()) {
                var scaling = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling");
                String type = scaling.get("type").getAsString();
                if (type.equals("nine_slice")) {
                    width = scaling.get("width").getAsInt();
                    height = scaling.get("height").getAsInt();
                    var b = scaling.get("border");
                    if (b.isJsonPrimitive()) {
                        int all = b.getAsInt();
                        border = new int[]{all, all, all, all};
                    } else {
                        var o = b.getAsJsonObject();
                        border = new int[]{o.get("left").getAsInt(), o.get("top").getAsInt(), o.get("right").getAsInt(), o.get("bottom").getAsInt()};
                    }
                } else if (type.equals("tile")) {
                    width = scaling.get("width").getAsInt();
                    height = scaling.get("height").getAsInt();
                    tile = true;
                }
            } catch (Exception e) {
                matteroverdrive.MatterOverdrive.LOGGER.warn("Bad GUI sprite metadata {}: {}", texture, e.toString());
            }
        }
        return new SpriteInfo(texture, width, height, border, tile);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
        buffer.vertex(matrix, x0, y0, 0).uv(u0, v0).endVertex();
        buffer.vertex(matrix, x0, y1, 0).uv(u0, v1).endVertex();
        buffer.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
        buffer.vertex(matrix, x1, y0, 0).uv(u1, v0).endVertex();
    }

    /** Fills w x h at (x, y) with the sprite region (u, v, uw, vh), tiled 1:1 (a 1 px region is stretched). */
    private static void tile(BufferBuilder buffer, Matrix4f matrix, int sw, int sh, int x, int y, int w, int h,
                             int u, int v, int uw, int vh) {
        if (w <= 0 || h <= 0 || uw <= 0 || vh <= 0) return;
        int stepX = uw == 1 ? w : uw, stepY = vh == 1 ? h : vh;
        for (int i = 0; i < w; i += stepX) {
            int cw = Math.min(stepX, w - i);
            float u0 = (float) u / sw, u1 = (float) (u + (uw == 1 ? 1 : cw)) / sw;
            for (int k = 0; k < h; k += stepY) {
                int ch = Math.min(stepY, h - k);
                float v0 = (float) v / sh, v1 = (float) (v + (vh == 1 ? 1 : ch)) / sh;
                quad(buffer, matrix, x + i, y + k, x + i + cw, y + k + ch, u0, v0, u1, v1);
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
