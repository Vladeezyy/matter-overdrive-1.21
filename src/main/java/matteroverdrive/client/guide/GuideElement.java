package matteroverdrive.client.guide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 GuideElementAbstract: an element of a guide page with CSS-like styles (margins, width/height in px or %,
 * text-align, float, color from the entry's stylesheet classes and the style attribute), laid out by its page.
 * Guide text uses the small unicode font like 1.7.10 (setUnicodeFlag(true)).
 */
public abstract class GuideElement {
    /** 1.7.10 setUnicodeFlag(true) = the uniform font. */
    public static final FontDescription UNIFORM = new FontDescription.Resource(Minecraft.UNIFORM_FONT);

    /** A clickable area (link) found while drawing, in screen coordinates. */
    public record Link(int x, int y, int w, int h, Runnable action) {}

    /** Everything an element needs while drawing: links are collected for the screen's mouse clicks. */
    public static final class Context {
        public final GuiGraphics g;
        public final List<Link> links = new ArrayList<>();
        public final GuideNavigator navigator;
        public int mouseX, mouseY;
        @Nullable public ItemStack hovered;

        public Context(GuiGraphics g, GuideNavigator navigator) {
            this.g = g;
            this.navigator = navigator;
        }
    }

    /** Opens guide entries for links (the Data Pad screen). */
    public interface GuideNavigator {
        void open(GuideEntry entry, int page);
    }

    protected int width, height;
    protected int marginTop, marginBottom, marginLeft, marginRight;
    /** 0 left, 1 center, 2 right. */
    protected int textAlign;
    /** 0 none, 1 left, 2 right. */
    protected int floating;
    protected int color = 0xFFFFFFFF;

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    public static Component uni(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(UNIFORM));
    }

    public static int uniWidth(String text) {
        return font().width(uni(text));
    }

    public void load(GuideEntry entry, Element element, Map<String, String> css, int width, int height) {
        Map<String, String> style = styleMap(css, element);
        loadStyles(style, width, height);
        loadContent(entry, element, width, height);
        calculateDimensions(style, width, height);
    }

    protected void loadStyles(Map<String, String> style, int width, int height) {
        marginTop = intStyle("margin-top", style, height);
        marginBottom = intStyle("margin-bottom", style, height);
        marginLeft = intStyle("margin-left", style, width);
        marginRight = intStyle("margin-right", style, width);
        String align = style.getOrDefault("text-align", "");
        textAlign = align.equalsIgnoreCase("center") ? 1 : align.equalsIgnoreCase("right") ? 2 : 0;
        // 1.7.10 getFloatingFromStyle: a stray ';' after the right check made every float "right"... unless "left"
        floating = style.containsKey("float") ? style.get("float").equalsIgnoreCase("left") ? 1 : 2 : 0;
        color = colorStyle(style);
    }

    protected abstract void loadContent(GuideEntry entry, Element element, int width, int height);

    protected void calculateDimensions(Map<String, String> style, int parentWidth, int parentHeight) {
        this.width = calculateWidth(style, parentWidth);
        if (style.containsKey("height")) this.height = intStyle("height", style, parentHeight);
    }

    protected int calculateWidth(Map<String, String> style, int parentWidth) {
        int w = this.width;
        if (floating == 0) w = parentWidth;
        if (style.containsKey("width")) w = intStyle("width", style, parentWidth);
        return w;
    }

    /** Draws at the element's top-left (screen coordinates); width is the space the parent gives it. */
    public abstract void draw(Context ctx, int x, int y, int width);

    public int getFloating() {
        return floating;
    }

    public int getWidth() {
        return width + marginLeft + marginRight;
    }

    public int getHeight() {
        return height + marginTop + marginBottom;
    }

    // --- styles ------------------------------------------------------------------------------------

    static Map<String, String> decodeStyle(String raw) {
        Map<String, String> map = new HashMap<>();
        for (String line : raw.split(";")) {
            String[] kv = line.split(":");
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    static Map<String, String> styleMap(Map<String, String> css, Element element) {
        Map<String, String> map = new HashMap<>();
        if (element.hasAttribute("class")) {
            for (String cls : element.getAttribute("class").split(" ")) {
                String rule = css.get("." + cls);
                if (rule != null) map.putAll(decodeStyle(rule));
            }
        }
        if (element.hasAttribute("style")) map.putAll(decodeStyle(element.getAttribute("style")));
        return map;
    }

    static int intStyle(String key, Map<String, String> style, int max) {
        String v = style.get(key);
        if (v == null) return 0;
        try {
            if (v.endsWith("px")) return Integer.parseInt(v.substring(0, v.length() - 2));
            if (v.endsWith("%")) return (int) (Integer.parseInt(v.substring(0, v.length() - 1)) / 100f * max);
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    static int colorStyle(Map<String, String> style) {
        String c = style.get("color");
        if (c == null) return 0xFFFFFFFF;
        if (c.startsWith("#")) return 0xFF000000 | Integer.parseInt(c.substring(1), 16);
        if (c.startsWith("rgb(")) {
            String[] rgb = c.substring(4, c.length() - 1).split(",");
            int a = rgb.length == 4 ? Integer.parseInt(rgb[3].trim()) : 255;
            return a << 24 | Integer.parseInt(rgb[0].trim()) << 16 | Integer.parseInt(rgb[1].trim()) << 8 | Integer.parseInt(rgb[2].trim());
        }
        return 0xFFFFFFFF;
    }

    // --- shortcodes ("[item name=phaser damage=1 guide=x page=2]") ---------------------------------------

    static Map<String, String> decodeShortcode(String raw) {
        Map<String, String> map = new HashMap<>();
        String[] params = raw.substring(1, raw.length() - 1).split(" ");
        if (params.length > 0) map.put("type", params[0]);
        for (int i = 1; i < params.length; i++) {
            String[] kv = params[i].split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    static @Nullable ItemStack shortcodeStack(Map<String, String> code) {
        if (!code.containsKey("name")) return null;
        int damage = 0;
        try {
            damage = Integer.parseInt(code.getOrDefault("damage", "0"));
        } catch (NumberFormatException ignored) {
        }
        return LegacyNames.stack(code.getOrDefault("mod", "mo"), code.get("name"), damage);
    }

    /** Draws a stack at (x, y) scaled, noting it for the tooltip when hovered. */
    static void drawStack(Context ctx, ItemStack stack, int x, int y, float scale) {
        if (stack == null || stack.isEmpty()) return;
        ctx.g.pose().pushMatrix();
        ctx.g.pose().translate(x, y);
        ctx.g.pose().scale(scale, scale);
        ctx.g.renderItem(stack, 0, 0);
        ctx.g.pose().popMatrix();
        int size = (int) (16 * scale);
        if (ctx.mouseX >= x && ctx.mouseX < x + size && ctx.mouseY >= y && ctx.mouseY < y + size) ctx.hovered = stack;
    }
}
