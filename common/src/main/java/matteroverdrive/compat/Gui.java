package matteroverdrive.compat;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Stand-ins for the 1.21.6+ GuiGraphics calls the screens use: {@code blit(RenderPipelines.GUI_TEXTURED, ...)} (here
 * the render type function) and the deferred {@code setTooltipForNextFrame}.
 */
public final class Gui {
    private Gui() {}

    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        g.blit(RenderType::guiTextured, tex, x, y, u, v, w, h, texW, texH);
    }

    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int texW, int texH, int color) {
        g.blit(RenderType::guiTextured, tex, x, y, u, v, w, h, texW, texH, color);
    }

    /** w x h on screen from the regionW x regionH texture region. */
    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
                            int texW, int texH) {
        g.blit(RenderType::guiTextured, tex, x, y, u, v, w, h, regionW, regionH, texW, texH);
    }

    public static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int regionW, int regionH,
                            int texW, int texH, int color) {
        g.blit(RenderType::guiTextured, tex, x, y, u, v, w, h, regionW, regionH, texW, texH, color);
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        g.blitSprite(RenderType::guiTextured, sprite, x, y, w, h);
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, int color) {
        g.blitSprite(RenderType::guiTextured, sprite, x, y, w, h, color);
    }

    /** Deferred to the end of the screen's render like 1.21.6+ (drawn at the mouse); drawn now outside a screen. */
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
