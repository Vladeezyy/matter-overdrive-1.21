package matteroverdrive.client.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.guide.GuideElement.Context;
import matteroverdrive.client.guide.GuideElement.Link;
import static matteroverdrive.client.guide.GuideElement.decodeShortcode;
import static matteroverdrive.client.guide.GuideElement.drawStack;
import static matteroverdrive.client.guide.GuideElement.font;
import static matteroverdrive.client.guide.GuideElement.shortcodeStack;
import static matteroverdrive.client.guide.GuideElement.uni;
import static matteroverdrive.client.guide.GuideElement.uniWidth;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The 1.7.10 guide element handlers (MatterOverdriveGuides.registerGuideElements). */
public final class GuideElements {
    static final int COLOR_MATTER = 0xFFBFE4E6, COLOR_HOLO_GREEN = 0xFF18CF00;

    static @Nullable GuideElement create(String tag) {
        return switch (tag) {
            case "text" -> new Text();
            case "title" -> new Title();
            case "image" -> new Image();
            case "preview" -> new Preview();
            case "recipe" -> new Recipe();
            case "details" -> new Details();
            case "tooltip" -> new Tooltip();
            case "depth" -> new Depth();
            case "creates" -> new Creates();
            default -> null;
        };
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    /** 1.7.10 GuideElementPage: children laid out top to bottom; float:left ones side by side, float:right from the right. */
    public static final class Page extends GuideElement {
        final List<GuideElement> elements = new ArrayList<>();

        @Override
        public void load(GuideEntry entry, Element element, Map<String, String> css, int width, int height) {
            super.load(entry, element, css, width, height);
            NodeList children = element.getChildNodes();
            for (int c = 0; c < children.getLength(); c++) {
                if (!(children.item(c) instanceof Element child)) continue;
                GuideElement e = create(child.getTagName());
                if (e == null) continue;
                try {
                    e.load(entry, child, css, width - marginLeft - marginRight, height - marginBottom - marginTop);
                    elements.add(e);
                } catch (RuntimeException ex) {
                    MatterOverdrive.LOGGER.error("Could not load guide element {} of {}", child.getTagName(), entry.name(), ex);
                }
            }
        }

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {}

        @Override
        public void draw(Context ctx, int px, int py, int width) {
            int ox = px + marginLeft, oy = py + marginTop;
            int x = 0, y = 0, floatRightX = width, maxHeight = 0;
            int inner = width - marginLeft - marginRight;
            for (GuideElement e : elements) {
                int ex = ox + x, ey = oy + y;
                if (e.getFloating() == 2) ex += floatRightX - e.getWidth() - marginLeft;
                e.draw(ctx, ex, ey, inner);
                if (e.getFloating() == 1) {
                    if (x + e.getWidth() > width) {
                        y += maxHeight;
                        x = 0;
                        floatRightX = width;
                        maxHeight = 0;
                    } else {
                        maxHeight = Math.max(e.getHeight(), maxHeight);
                        x += e.getWidth();
                    }
                } else if (e.getFloating() == 2) {
                    if (floatRightX - e.getWidth() < 0) {
                        maxHeight = Math.max(maxHeight, e.getHeight());
                        floatRightX = width;
                        y += maxHeight;
                        maxHeight = 0;
                    } else {
                        floatRightX -= e.getWidth();
                    }
                } else {
                    maxHeight = Math.max(maxHeight, e.getHeight());
                    y += maxHeight;
                    maxHeight = 0;
                    x = 0;
                    floatRightX = width;
                }
            }
        }
    }

    // --- text -------------------------------------------------------------------------------------

    /** 1.7.10 TextChunk / TextChunkLink: a word, or a link to a guide entry. */
    record Chunk(String text, int width, @Nullable GuideEntry link, int page) {}

    private static final Pattern SHORTCODE = Pattern.compile("\\[(.*?)\\]");

    /** 1.7.10 GuideElementTextAbstract.handleTextFormatting: words and shortcode chunks wrapped to the width. */
    static List<List<Chunk>> layoutText(GuideEntry entry, String text, int width, boolean aquaVariables) {
        List<Object> parts = new ArrayList<>();
        Matcher m = SHORTCODE.matcher(text);
        int last = 0;
        while (m.find()) {
            parts.add(text.substring(last, m.start()));
            Chunk chunk = shortcodeChunk(decodeShortcode(m.group()));
            if (chunk != null) parts.add(chunk);
            last = m.end();
        }
        parts.add(text.substring(last));
        List<Chunk> chunks = new ArrayList<>();
        for (Object o : parts) {
            if (o instanceof String s) {
                for (String word : s.split("\\s+")) {
                    if (word.isEmpty()) continue;
                    String w = variables(word.trim(), entry, aquaVariables);
                    chunks.add(new Chunk(w, uniWidth(w), null, 0));
                }
            } else {
                chunks.add((Chunk) o);
            }
        }
        List<List<Chunk>> lines = new ArrayList<>();
        List<Chunk> line = new ArrayList<>();
        lines.add(line);
        for (int i = 0; i < chunks.size(); i++) {
            int w = advance(chunks.get(i), i + 1 < chunks.size() ? chunks.get(i + 1) : null);
            if (lineWidth(line) + w > width) {
                line = new ArrayList<>();
                lines.add(line);
            }
            line.add(chunks.get(i));
        }
        return lines;
    }

    /** 1.7.10 handleVariables: $itemName = the entry's first icon's name (aqua in text). */
    static String variables(String text, GuideEntry entry, boolean aqua) {
        if (!text.contains("$itemName")) return text;
        String name = entry.icon().isEmpty() ? entry.getDisplayName() : entry.icon().getHoverName().getString();
        return text.replace("$itemName", aqua ? ChatFormatting.AQUA + name + ChatFormatting.RESET : name);
    }

    static int lineWidth(List<Chunk> line) {
        int w = 0;
        for (Chunk c : line) w += c.width() + uniWidth(" ");
        return w;
    }

    /** 1.7.10 calculateWidth: no space before punctuation or after an opening bracket. */
    static int advance(Chunk main, @Nullable Chunk after) {
        if (after != null && after.text().matches("[.,!\"')}]")) return main.width();
        if (main.text().matches("[({]")) return main.width();
        return main.width() + uniWidth(" ");
    }

    /** 1.7.10 handleShortCode: item/block names in green (links to their entries), RF capacity, guide links in yellow. */
    static @Nullable Chunk shortcodeChunk(Map<String, String> code) {
        String type = code.getOrDefault("type", "");
        int page = 0;
        try {
            page = Integer.parseInt(code.getOrDefault("page", "0"));
        } catch (NumberFormatException ignored) {
        }
        if (type.equalsIgnoreCase("block") || type.equalsIgnoreCase("item")) {
            ItemStack stack = shortcodeStack(code);
            if (stack == null) return null;
            GuideEntry target = code.containsKey("guide") ? Guides.find(code.get("guide")) : Guides.findByStack(stack);
            String text = ChatFormatting.GREEN + stack.getHoverName().getString() + ChatFormatting.RESET;
            return new Chunk(text, uniWidth(text), target, page);
        }
        if (type.equalsIgnoreCase("rf")) {
            ItemStack stack = shortcodeStack(code);
            if (stack == null) return null;
            var handler = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (handler == null) return null;
            String text = handler.getMaxEnergyStored() + " FE";
            return new Chunk(text, uniWidth(text), null, 0);
        }
        if (type.equalsIgnoreCase("guide")) {
            GuideEntry target = Guides.find(code.getOrDefault("name", ""));
            if (target == null) return null;
            String text = ChatFormatting.YELLOW + target.getDisplayName() + ChatFormatting.RESET;
            return new Chunk(text, uniWidth(text), target, page);
        }
        return null;
    }

    /** Draws laid-out lines; links underline on hover and register a click area. */
    static void drawLines(Context ctx, List<List<Chunk>> lines, int x0, int y0, int width, int align, int color) {
        int lh = font().lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            List<Chunk> line = lines.get(i);
            int x = align == 1 ? -lineWidth(line) / 2 + width / 2 : align == 2 ? -lineWidth(line) + width : 0;
            int y = y0 + i * lh;
            for (int c = 0; c < line.size(); c++) {
                Chunk chunk = line.get(c);
                int cx = x0 + x;
                boolean over = chunk.link() != null && ctx.mouseX > cx && ctx.mouseX < cx + chunk.width() && ctx.mouseY > y && ctx.mouseY < y + lh;
                Component text = over ? uni(chunk.text()).copy().withStyle(ChatFormatting.UNDERLINE) : uni(chunk.text());
                ctx.g.drawString(font(), text, cx, y, color, false);
                if (chunk.link() != null) {
                    GuideEntry target = chunk.link();
                    int page = chunk.page();
                    ctx.links.add(new Link(cx, y, chunk.width(), lh, () -> ctx.navigator.open(target, page)));
                }
                x += advance(chunk, c + 1 < line.size() ? line.get(c + 1) : null);
            }
        }
    }

    /** 1.7.10 GuideElementText. */
    public static final class Text extends GuideElement {
        List<List<Chunk>> lines = List.of();

        @Override
        public void load(GuideEntry entry, Element element, Map<String, String> css, int width, int height) {
            Map<String, String> style = styleMap(css, element);
            loadStyles(style, width, height);
            this.width = calculateWidth(style, width);
            loadContent(entry, element, width, height);
            calculateDimensions(style, width, height);
        }

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            lines = layoutText(entry, element.getTextContent(), this.width, true);
            this.height = font().lineHeight * lines.size();
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            drawLines(ctx, lines, x + marginLeft, y + marginTop, width - marginLeft - marginRight, textAlign, color);
        }
    }

    /** 1.7.10 GuideElementTitle: scaled text (size 3 by default). */
    public static final class Title extends GuideElement {
        String title = "";
        float size = 3;

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            title = variables(element.getTextContent().trim(), entry, false);
            if (element.hasAttribute("size")) size = Float.parseFloat(element.getAttribute("size"));
            this.width = width;
            this.height = (int) (font().lineHeight * size);
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            int titleWidth = (int) (uniWidth(title) * size);
            int tx = textAlign == 1 ? (width - marginLeft - marginRight) / 2 - titleWidth / 2 : textAlign == 2 ? width - marginLeft - marginRight - titleWidth : 0;
            ctx.g.pose().pushPose();
            ctx.g.pose().translate(x + tx + marginLeft, y + marginTop, 0);
            ctx.g.pose().scale(size, size, 1);
            ctx.g.drawString(font(), uni(title), 0, 0, color, false);
            ctx.g.pose().popPose();
        }
    }

    /** 1.7.10 GuideElementImage: textures/<src>, scaled down to fit the width. */
    public static final class Image extends GuideElement {
        ResourceLocation location;
        int imageWidth, imageHeight;

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            if (element.hasAttribute("src")) location = tex("textures/" + element.getAttribute("src"));
            if (element.hasAttribute("width")) imageWidth = Integer.parseInt(element.getAttribute("width"));
            if (element.hasAttribute("height")) imageHeight = Integer.parseInt(element.getAttribute("height"));
            this.width = imageWidth;
            this.height = imageHeight;
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            if (location == null) return;
            int maxWidth = width - marginLeft - marginRight;
            float scale = (float) maxWidth / Math.max(imageWidth, maxWidth);
            int w = (int) (imageWidth * scale), h = (int) (imageHeight * scale);
            int ix = textAlign == 1 ? width / 2 - w / 2 : 0;
            matteroverdrive.compat.Gui.blit(ctx.g, location, x + ix + marginLeft, y + marginTop, 0, 0, w, h, w, h, color);
        }
    }

    /** 1.7.10 GuideElementPreview: an item (the "item" shortcode, or one of the entry's icons) at a size. */
    public static final class Preview extends GuideElement {
        ItemStack stack;
        float size = 1;

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            if (element.hasAttribute("item")) {
                stack = shortcodeStack(decodeShortcode(element.getAttribute("item")));
            } else if (!entry.icons().isEmpty()) {
                int index = element.hasAttribute("index") ? Integer.parseInt(element.getAttribute("index"))
                        : (int) (Math.random() * entry.icons().size());
                stack = entry.icons().get(Mth.clamp(index, 0, entry.icons().size() - 1));
            }
            if (element.hasAttribute("size")) size = Float.parseFloat(element.getAttribute("size"));
            this.width = this.height = (int) (16 * size);
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            int px = textAlign == 1 ? (int) (width / 2 - 8 * size) : textAlign == 2 ? (int) (width - 16 * size) : 0;
            drawStack(ctx, stack, x + px + marginLeft, y + marginTop, size);
        }
    }

    /** 1.7.10 GuideElementRecipe: the crafting grid (asked from the server) on guide_recipe.png. */
    public static final class Recipe extends GuideElement {
        private static final ResourceLocation BACKGROUND = tex("textures/gui/elements/guide_recipe.png");
        ItemStack output;

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            output = element.hasAttribute("item") ? shortcodeStack(decodeShortcode(element.getAttribute("item"))) : entry.icon();
            if (output != null) GuideRecipes.request(output.getItem());
            this.width = this.height = 100;
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            int rx = textAlign == 1 ? x + marginLeft + this.width / 2 - 110 / 2 : x + marginLeft;
            int ry = y + marginTop;
            matteroverdrive.compat.Gui.blit(ctx.g, BACKGROUND, rx + 8, ry + 8, 0, 0, 96, 96, 48, 48, 48, 48, COLOR_MATTER);
            List<List<ItemStack>> grid = output == null ? null : GuideRecipes.get(output.getItem());
            if (grid == null) return;
            long time = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            for (int i = 0; i < Math.min(9, grid.size()); i++) {
                List<ItemStack> options = grid.get(i);
                if (options.isEmpty()) continue;
                ItemStack stack = options.get((int) (time / 100 % options.size()));
                drawStack(ctx, stack, rx + 10 + i % 3 * 33, ry + 9 + i / 3 * 33, 1.5f);
            }
        }
    }

    /** 1.7.10 GuideElementDetails: the item's details lines (item.*.details). */
    public static final class Details extends GuideElement {
        List<String> details = List.of();

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            ItemStack stack = element.hasAttribute("item") ? shortcodeStack(decodeShortcode(element.getAttribute("item"))) : null;
            if (stack == null) return;
            String key = stack.getItem().getDescriptionId() + ".details";
            if (net.minecraft.client.resources.language.I18n.exists(key)) {
                details = List.of(net.minecraft.client.resources.language.I18n.get(key).split("\\\\n|\n"));
            }
            this.height = details.size() * font().lineHeight;
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            for (int i = 0; i < details.size(); i++) {
                ctx.g.drawString(font(), uni(details.get(i)), x + marginLeft, y + marginTop + i * font().lineHeight, color, false);
            }
        }
    }

    /** 1.7.10 GuideElementTooltip: the item's tooltip lines (without its name). */
    public static final class Tooltip extends GuideElement {
        List<Component> lines = List.of();

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            ItemStack stack = element.hasAttribute("item") ? shortcodeStack(decodeShortcode(element.getAttribute("item"))) : entry.icon();
            var mc = Minecraft.getInstance();
            if (stack != null && mc.level != null) {
                List<Component> all = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
                lines = all.size() > 1 ? all.subList(1, all.size()) : List.of();
            }
            this.height = lines.size() * font().lineHeight;
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            for (int i = 0; i < lines.size(); i++) {
                ctx.g.drawString(font(), lines.get(i).copy().withStyle(s -> s.withFont(UNIFORM)), x, y + i * font().lineHeight, color, false);
            }
        }
    }

    /** 1.7.10 InfogramDepth: the ore's depth band on the terrain picture and the ore under the lens. */
    public static final class Depth extends GuideElement {
        private static final ResourceLocation TERRAIN = tex("textures/gui/elements/guide_info_depth_terrain.png");
        private static final ResourceLocation STRIPES = tex("textures/gui/elements/guide_info_depth_terrain_stripes.png");
        private static final ResourceLocation LENS = tex("textures/gui/elements/guide_info_depth_ore_lense.png");
        ItemStack stack;
        int minDepth = 0, maxDepth = 64;

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            if (element.hasAttribute("min")) minDepth = Integer.parseInt(element.getAttribute("min"));
            if (element.hasAttribute("max")) maxDepth = Integer.parseInt(element.getAttribute("max"));
            stack = element.hasAttribute("item") ? shortcodeStack(decodeShortcode(element.getAttribute("item"))) : entry.icon();
            this.width = 100;
            this.height = 53 + 16;
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            float minPercent = 1 - Math.min(minDepth, 64) / 64f, maxPercent = 1 - Math.min(maxDepth, 64) / 64f;
            var g = ctx.g;
            if (maxDepth >= 0) g.drawString(font(), uni(maxDepth + "-"), x + 8, y + 8 + (int) (46 * maxPercent), COLOR_HOLO_GREEN, false);
            if (minDepth > 0) g.drawString(font(), uni(minDepth + "-"), x + 8, y + 8 + (int) (46 * minPercent), COLOR_HOLO_GREEN, false);
            int tx = x + marginLeft + 20, ty = y + marginTop + 8;
            matteroverdrive.compat.Gui.blit(g, TERRAIN, tx, ty, 0, 0, 73, 53, 144, 105, 144, 105);
            // 1.7.10 used a stencil: the stripes only between the depths
            g.enableScissor(x + marginLeft + 24, ty + (int) (53 * maxPercent), x + marginLeft + 24 + 69, ty + (int) (53 * minPercent));
            matteroverdrive.compat.Gui.blit(g, STRIPES, x + marginLeft + 24, ty, 0, 0, 69, 53, 137, 105, 137, 105);
            g.disableScissor();
            matteroverdrive.compat.Gui.blit(g, LENS, x + marginLeft + 69, y + marginTop + 16, 0, 0, 84, 41, 168, 82, 168, 82);
            drawStack(ctx, stack, x + marginLeft + 123, y + marginTop + 21, 1.5f);
        }
    }

    /** 1.7.10 InfogramCreates: "from" turns into "to". */
    public static final class Creates extends GuideElement {
        private static final ResourceLocation BACKGROUND = tex("textures/gui/elements/guide_info_creates.png");
        ItemStack from, to;

        @Override
        protected void loadContent(GuideEntry entry, Element element, int width, int height) {
            if (element.hasAttribute("to")) to = shortcodeStack(decodeShortcode(element.getAttribute("to")));
            from = element.hasAttribute("from") ? shortcodeStack(decodeShortcode(element.getAttribute("from"))) : entry.icon();
            this.width = 100;
            this.height = 36 + 16;
        }

        @Override
        public void draw(Context ctx, int x, int y, int width) {
            matteroverdrive.compat.Gui.blit(ctx.g, BACKGROUND, x + marginLeft, y + marginTop, 0, 0, 115, 36, 230, 71, 230, 71);
            drawStack(ctx, from, x + marginLeft + 5, y + marginTop + 5, 1.5f);
            drawStack(ctx, to, x + marginLeft + 86, y + marginTop + 5, 1.5f);
        }
    }


    private GuideElements() {}
}
