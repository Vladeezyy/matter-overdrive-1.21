package matteroverdrive.compat;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
        g.blitSprite(sprite, x, y, w, h);
        end();
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
