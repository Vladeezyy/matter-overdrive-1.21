package matteroverdrive.client.quest;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.guide.GuideDocument;
import matteroverdrive.client.guide.GuideElement;
import matteroverdrive.client.guide.GuideElements;
import matteroverdrive.client.guide.GuideEntry;
import matteroverdrive.client.guide.Guides;
import matteroverdrive.item.DataPadItem;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestPayloads;
import matteroverdrive.quest.QuestStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import matteroverdrive.compat.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 1.7.10 GuiDataPad (300 wide; 240 high here, 1.7.10's 260 doesn't fit a 240-high scaled screen): the guide entries
 * (PageGuideEntries: category buttons, search, list / grid / grouped ordering, drag to scroll), the open entry
 * (PageGuideDescription: pages, links, back) and the active quests (PageActiveQuests: list, info, objectives with a
 * filled square when done, rewards, Complete / Abandon).
 */
public class DataPadScreen extends Screen {
    public static final int WIDTH = 300, HEIGHT = 240;
    private static final int HOLO = 0xFFA9E2FB, HOLO_DIM = ARGB.color(255, ARGB.scaleRGB(0xA9E2FB, 0.5f)), HOLO_GREEN = 0xFF18CF00,
            HOLO_RED = 0xFFE65014;
    private static final ResourceLocation BACKGROUND = id("data_pad");
    private static final ResourceLocation BUTTON = id("button_normal"), BUTTON_OVER = id("button_over"), BUTTON_DARK = id("button_over_dark");
    private static final ResourceLocation CLOSE = tex("textures/gui/elements/close_button.png");
    private static final ResourceLocation BAND = tex("textures/gui/elements/android_bg_element.png");
    private static final int LIST_X = 22, LIST_Y = 28, LIST_W = WIDTH - 28 - 44, ROW = 12, ROWS = 6;
    private static final int INFO_X = 22, INFO_Y = 116, INFO_W = WIDTH - 28 - 15, INFO_H = 80;
    private static final int BUTTONS_Y = HEIGHT - 28;
    private static final int[] QUESTS_BT = {WIDTH - 96, BUTTONS_Y, 22, 22}, COMPLETE_BT = {WIDTH - 72, BUTTONS_Y, 22, 22},
            ABANDON_BT = {WIDTH - 48, BUTTONS_Y + 4, 16, 16};

    private static final int PAGE_X = 14, PAGE_Y = 14, PAGE_W = WIDTH - 28, PAGE_H = HEIGHT - 14 - 49;
    private static final ResourceLocation ENTRY_BG = tex("textures/gui/elements/quide_element_bg.png");
    private static final ResourceLocation CIRCUIT = tex("textures/gui/elements/guide_cuircit_background.png");
    private static final ResourceLocation GROUP_BG = id("guide_group");
    private static final ResourceLocation SCROLL_LEFT = tex("textures/gui/elements/scroll_left.png");
    private static final ResourceLocation SCROLL_RIGHT = tex("textures/gui/elements/scroll_right.png");
    private static final ResourceLocation RETURN = tex("textures/gui/elements/return_arrow.png");
    private static final String[] ORDER_ICONS = {"list", "grid", "sort_random"};
    /** 1.7.10 PageGuideEntries: static scroll and search, kept between openings; PageGuideDescription history. */
    private static int entriesScrollX, entriesScrollY;
    private static String searchFilter = "";
    private static final java.util.ArrayDeque<String[]> HISTORY = new java.util.ArrayDeque<>();

    private EditBox search;
    private List<GuideElements.Page> pages = List.of();
    private String loadedGuide;
    private final List<GuideElement.Link> links = new ArrayList<>();
    private double lastDragX, lastDragY;
    private boolean dragging;
    private final InteractionHand hand;
    private DataPadItem.State state;
    private int left, top;
    private int listScroll;

    public DataPadScreen(InteractionHand hand) {
        super(Component.translatable("item.matteroverdrive.data_pad"));
        this.hand = hand;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private static ResourceLocation holo(String name) {
        return tex("textures/gui/holo/" + name + ".png");
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        state = DataPadItem.getState(pad());
        search = new EditBox(font, left + PAGE_X + 28 + 12, top + PAGE_Y + 5, 116, 10, Component.empty());
        search.setBordered(false);
        search.setTextColor(0xFFBFE4E6);
        search.setValue(searchFilter);
        search.setResponder(text -> searchFilter = text);
        addRenderableWidget(search);
        loadedGuide = null;
    }

    private ItemStack pad() {
        return minecraft.player.getItemInHand(hand);
    }

    private List<QuestStack> quests() {
        return PlayerQuests.get(minecraft.player).getActiveQuests();
    }

    private QuestStack selected() {
        List<QuestStack> quests = quests();
        return quests.isEmpty() ? null : quests.get(Math.clamp(state.selectedQuest(), 0, quests.size() - 1));
    }

    private void setState(DataPadItem.State next) {
        state = next;
        ClientPacketDistributor.sendToServer(new QuestPayloads.DataPadState(hand == InteractionHand.OFF_HAND, next));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private boolean canComplete(QuestStack stack) {
        if (stack == null) return false;
        for (int i = 0; i < stack.getObjectivesCount(minecraft.player); i++) {
            if (!stack.isObjectiveCompleted(minecraft.player, i)) return false;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!(pad().getItem() instanceof DataPadItem)) {
            onClose();
            return;
        }
        int mx = mouseX - left, my = mouseY - top;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, WIDTH, HEIGHT);
        boolean overClose = in(mx, my, WIDTH - 32, 20, 9, 9);
        g.blit(RenderPipelines.GUI_TEXTURED, CLOSE, left + WIDTH - 32, top + 20, overClose ? 9 : 0, 0, 9, 9, 18, 9);
        search.visible = state.page() == DataPadItem.State.PAGE_ENTRIES;
        links.clear();
        switch (state.page()) {
            case DataPadItem.State.PAGE_QUESTS -> renderQuests(g, mx, my, mouseX, mouseY);
            case DataPadItem.State.PAGE_DESCRIPTION -> renderDescription(g, mouseX, mouseY);
            default -> renderEntries(g, mx, my, mouseX, mouseY);
        }
        // 1.7.10 updateElementInformation: category buttons (the active one disabled on the guide pages), quest buttons
        int i = 0;
        for (Guides.Category category : Guides.categories().values()) {
            int[] b = {16 + 32 * i++, BUTTONS_Y, 22, 22};
            boolean enabled = !(category.name().equals(state.category()) && state.page() <= 1);
            int iconSize = switch (category.icon()) { case "home_icon" -> 14; case "ammo" -> 18; default -> 16; };
            button(g, mx, my, b, category.icon(), iconSize, enabled, HOLO, mouseX, mouseY,
                    "guide.matteroverdrive.category." + category.name());
        }
        boolean quests = state.page() == DataPadItem.State.PAGE_QUESTS;
        button(g, mx, my, QUESTS_BT, "question_mark", 20, !quests, HOLO, mouseX, mouseY, "gui.matteroverdrive.quest.active_quests");
        if (quests) {
            QuestStack selected = selected();
            button(g, mx, my, COMPLETE_BT, "tick", 16, canComplete(selected), HOLO_GREEN, mouseX, mouseY, "gui.matteroverdrive.quest.complete");
            button(g, mx, my, ABANDON_BT, "mini_quit", 16, selected != null, HOLO_RED, mouseX, mouseY, "gui.matteroverdrive.quest.abandon");
        }
    }

    // --- guide entries (1.7.10 PageGuideEntries) ---------------------------------------------------

    private Guides.Category activeCategory() {
        Guides.Category c = Guides.categories().get(state.category());
        return c != null ? c : Guides.categories().get("general");
    }

    /** Entry positions inside the page for the current ordering; groups' bounds for the grouped one. */
    private record Placed(GuideEntry entry, int x, int y) {}

    private List<Placed> placeEntries(java.util.Map<String, int[]> groups, int[] inner) {
        List<Placed> placed = new ArrayList<>();
        int pad = 6, x = 8 + entriesScrollX, y = 22 + entriesScrollY, heightCount = 0, widthCount = 0;
        for (GuideEntry entry : activeCategory().entries()) {
            if (!entry.getDisplayName().toLowerCase().contains(searchFilter.toLowerCase())) continue;
            switch (state.ordering()) {
                case 0 -> {
                    placed.add(new Placed(entry, x + 16, y));
                    y += 26;
                    heightCount += 26;
                }
                case 1 -> {
                    placed.add(new Placed(entry, x, y));
                    x += 26;
                    if (x > PAGE_W - 22 - 4) {
                        x = 8;
                        y += 26;
                        heightCount += 26;
                    }
                }
                default -> {
                    int ex = x + entry.guiX(), ey = y + entry.guiY();
                    placed.add(new Placed(entry, ex, ey));
                    widthCount = Math.max(widthCount, entry.guiX() + 22 + pad + 4);
                    heightCount = Math.max(heightCount, entry.guiY() + 22 + pad + 4);
                    if (entry.group() != null) {
                        int[] b = groups.get(entry.group());
                        if (b == null) groups.put(entry.group(), new int[] {ex - pad, ey - pad, ex + 22 + pad, ey + 22 + pad});
                        else {
                            b[0] = Math.min(b[0], ex - pad);
                            b[1] = Math.min(b[1], ey - pad);
                            b[2] = Math.max(b[2], ex + 22 + pad);
                            b[3] = Math.max(b[3], ey + 22 + pad);
                        }
                    }
                }
            }
        }
        inner[0] = Math.max(widthCount + 8, PAGE_W);
        inner[1] = Math.max(heightCount + 22, PAGE_H);
        return placed;
    }

    private void clampEntriesScroll(int[] inner) {
        entriesScrollX = Math.max(Math.min(entriesScrollX, 0), PAGE_W - inner[0]);
        entriesScrollY = Math.max(Math.min(entriesScrollY, 0), PAGE_H - inner[1]);
    }

    private void renderEntries(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        java.util.Map<String, int[]> groups = new java.util.LinkedHashMap<>();
        int[] inner = new int[2];
        List<Placed> placed = placeEntries(groups, inner);
        int px = left + PAGE_X, py = top + PAGE_Y;
        g.enableScissor(px, py, px + PAGE_W, py + PAGE_H);
        // 1.7.10: two parallax layers of the circuit background at 10%
        float aspect = (float) PAGE_H / PAGE_W;
        circuit(g, px, py, 0.5f - entriesScrollX * 0.001f, 0.5f - entriesScrollY * 0.0003f, 0.5f, 0.5f * aspect);
        circuit(g, px, py, 0.2f - entriesScrollX * 0.001f, 0.2f - entriesScrollY * 0.0005f, 0.3f, 0.3f * aspect);
        if (state.ordering() > 1) {
            for (var group : groups.entrySet()) {
                int[] b = group.getValue();
                g.blitSprite(RenderPipelines.GUI_TEXTURED, GROUP_BG, px + b[0], py + b[1], b[2] - b[0], b[3] - b[1], 0xFFBFE4E6);
                Component name = GuideElement.uni(net.minecraft.client.resources.language.I18n.get("guide.matteroverdrive.group." + group.getKey()));
                g.drawString(font, name, px + (b[0] + b[2]) / 2 - font.width(name) / 2, py + b[1] - 4, 0xFFBFE4E6, false);
            }
        }
        long time = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        GuideEntry hoveredEntry = null;
        for (Placed p : placed) {
            int ex = px + p.x(), ey = py + p.y();
            g.blit(RenderPipelines.GUI_TEXTURED, ENTRY_BG, ex, ey, 0, 0, 22, 22, 22, 22);
            List<ItemStack> icons = p.entry().icons();
            if (!icons.isEmpty()) g.renderItem(icons.get((int) (time / 20 % icons.size())), ex + 3, ey + 3);
            if (state.ordering() == 0) {
                g.drawString(font, GuideElement.uni(p.entry().getDisplayName()), ex + 26, ey + 6, 0xFFBFE4E6, false);
            }
            if (mx >= p.x() + PAGE_X && my >= p.y() + PAGE_Y && mx < p.x() + PAGE_X + 22 && my < p.y() + PAGE_Y + 22
                    && in(mx, my, PAGE_X, PAGE_Y, PAGE_W, PAGE_H)) {
                hoveredEntry = p.entry();
            }
        }
        g.disableScissor();
        // search icon and the ordering toggle (1.7.10 ElementStatesHoloIcons)
        g.blit(RenderPipelines.GUI_TEXTURED, holo("page_icon_search"), px + 28, py + 3, 0, 0, 10, 10, 16, 16, 16, 16, 0xFFBFE4E6);
        int ordering = Math.clamp(state.ordering(), 0, 2);
        g.blit(RenderPipelines.GUI_TEXTURED, holo(ORDER_ICONS[ordering]), px + PAGE_W - 38, py + 2, 0, 0, 16, 16, 16, 16, 16, 16, 0xFFBFE4E6);
        if (hoveredEntry != null) g.setTooltipForNextFrame(font, Component.literal(hoveredEntry.getDisplayName()), mouseX, mouseY);
    }

    private void circuit(GuiGraphics g, int x, int y, float u, float v, float uw, float vh) {
        int size = 1600;
        g.blit(RenderPipelines.GUI_TEXTURED, CIRCUIT, x, y, u * size, v * size, PAGE_W, PAGE_H, (int) (uw * size), (int) (vh * size),
                size, size, ARGB.color(26, 0xFFFFFF));
    }

    private GuideEntry entryAt(double mx, double my) {
        java.util.Map<String, int[]> groups = new java.util.HashMap<>();
        int[] inner = new int[2];
        if (!in(mx, my, PAGE_X, PAGE_Y, PAGE_W, PAGE_H)) return null;
        for (Placed p : placeEntries(groups, inner)) {
            if (in(mx, my, PAGE_X + p.x(), PAGE_Y + p.y(), 22, 22)) return p.entry();
        }
        return null;
    }

    // --- guide description (1.7.10 PageGuideDescription) ------------------------------------------

    private void ensureLoaded() {
        if (state.guide().equals(loadedGuide)) return;
        loadedGuide = state.guide();
        GuideEntry entry = Guides.find(state.guide());
        pages = entry == null ? List.of() : GuideDocument.load(entry, PAGE_W, PAGE_H);
    }

    /** 1.7.10 OpenGuide: remembers where we were when following a link. */
    private void openGuide(GuideEntry entry, int page, boolean history) {
        if (!entry.name().equals(state.guide())) {
            if (history) HISTORY.push(new String[] {state.guide(), Integer.toString(state.guidePage())});
            setState(state.withGuide(entry.name(), page).withPage(DataPadItem.State.PAGE_DESCRIPTION));
        } else {
            setState(state.withPage(DataPadItem.State.PAGE_DESCRIPTION));
        }
    }

    private void renderDescription(GuiGraphics g, int mouseX, int mouseY) {
        ensureLoaded();
        int px = left + PAGE_X, py = top + PAGE_Y;
        int page = state.guidePage();
        if (page >= 0 && page < pages.size()) {
            GuideElement.Context ctx = new GuideElement.Context(g, (entry, p) -> openGuide(entry, p, true));
            ctx.mouseX = mouseX;
            ctx.mouseY = mouseY;
            pages.get(page).draw(ctx, px, py, PAGE_W);
            links.addAll(ctx.links);
            if (ctx.hovered != null) g.setTooltipForNextFrame(font, ctx.hovered, mouseX, mouseY);
        } else {
            Component none = GuideElement.uni("No Info...");
            g.drawString(font, none, px + PAGE_W / 2 - font.width(none) / 2, py + PAGE_H / 2, HOLO_RED, false);
        }
        int by = py + PAGE_H - 16;
        int mx = mouseX - left, my = mouseY - top;
        if (page > 0) arrow(g, SCROLL_LEFT, px + 10, by, 10, 10, mx, my);
        if (page < pages.size() - 1) arrow(g, SCROLL_RIGHT, px + PAGE_W - 20, by, 10, 10, mx, my);
        arrow(g, RETURN, px + PAGE_W / 2 - 5, by, 11, 11, mx, my);
    }

    private void arrow(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int mx, int my) {
        boolean over = in(mx + left, my + top, x, y, w, h);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, over ? w : 0, 0, w, h, w * 2, h);
    }

    /** 1.7.10 undo: back through the link history, else to the entries. */
    private void goBack() {
        if (!HISTORY.isEmpty()) {
            String[] last = HISTORY.pop();
            setState(state.withGuide(last[0], Integer.parseInt(last[1])));
        } else {
            setState(state.withPage(DataPadItem.State.PAGE_ENTRIES));
        }
    }

    private void button(GuiGraphics g, int mx, int my, int[] b, String icon, int iconSize, boolean enabled, int color, int mouseX, int mouseY,
                        String tooltip) {
        boolean over = enabled && in(mx, my, b[0], b[1], b[2], b[3]);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, !enabled ? BUTTON_DARK : over ? BUTTON_OVER : BUTTON, left + b[0], top + b[1], b[2], b[3]);
        int size = Math.min(iconSize, b[2] - 4);
        g.blit(RenderPipelines.GUI_TEXTURED, holo(icon), left + b[0] + (b[2] - size) / 2, top + b[1] + (b[3] - size) / 2, 0, 0, size, size,
                iconSize, iconSize, iconSize, iconSize, enabled ? color : HOLO_DIM);
        if (in(mx, my, b[0], b[1], b[2], b[3])) g.setTooltipForNextFrame(font, Component.translatable(tooltip), mouseX, mouseY);
    }

    private void renderQuests(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        List<QuestStack> quests = quests();
        QuestStack selected = selected();
        // the list (1.7.10 MOElementListBox of ListElementQuest: centred titles, "‣ " on the selected one)
        for (int i = 0; i < ROWS && listScroll + i < quests.size(); i++) {
            QuestStack stack = quests.get(listScroll + i);
            Quest quest = stack.getQuest();
            String title = quest == null ? stack.getQuestId() : quest.getTitle(stack, minecraft.player);
            boolean isSelected = stack == selected;
            int ty = top + LIST_Y + i * ROW;
            int tw = font.width(title);
            if (isSelected) g.drawString(font, "‣ " + title, left + LIST_X + LIST_W / 2 - tw / 2 - 8, ty, HOLO, false);
            else g.drawString(font, title, left + LIST_X + LIST_W / 2 - tw / 2, ty, HOLO_DIM, false);
        }
        if (quests.isEmpty()) {
            Component none = Component.translatable("gui.matteroverdrive.quest.none");
            g.drawString(font, none, left + LIST_X + LIST_W / 2 - font.width(none) / 2, top + LIST_Y, HOLO_DIM, false);
        }
        // the band between list and info (1.7.10 android_bg_element at 20% holo)
        g.blit(RenderPipelines.GUI_TEXTURED, BAND, left + 60, top + 102, 0, 0, 174, 11, 174, 11, ARGB.color(51, 0xA9E2FB));
        if (selected == null || selected.getQuest() == null) return;
        // info, objectives, rewards
        Quest quest = selected.getQuest();
        List<FormattedCharSequence> lines = new ArrayList<>();
        String info = quest.getInfo(selected, minecraft.player).replace("/n/", "\n");
        for (String part : info.split("\n")) lines.addAll(font.split(Component.literal(part), INFO_W));
        lines.add(FormattedCharSequence.EMPTY);
        for (int i = 0; i < selected.getObjectivesCount(minecraft.player); i++) {
            boolean done = selected.isObjectiveCompleted(minecraft.player, i);
            // 1.7.10 QuestFactory.getFormattedQuestObjective: filled / empty square, green / dark green
            Component objective = Component.literal((done ? "■ " : "□ ") + quest.getObjective(selected, minecraft.player, i))
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN);
            lines.addAll(font.split(objective, INFO_W));
        }
        lines.add(FormattedCharSequence.EMPTY);
        lines.add(Component.translatable("gui.matteroverdrive.quest.rewards", quest.getXpReward(selected, minecraft.player))
                .withStyle(ChatFormatting.GOLD).getVisualOrderText());
        List<ItemStack> rewards = quest.getRewards(selected, minecraft.player);
        int contentHeight = lines.size() * font.lineHeight + (rewards.isEmpty() ? 0 : 20);
        int maxScroll = Math.max(0, contentHeight - INFO_H);
        int scroll = Math.clamp(state.scroll(), 0, maxScroll);
        g.enableScissor(left + INFO_X, top + INFO_Y, left + INFO_X + INFO_W, top + INFO_Y + INFO_H);
        int y = top + INFO_Y - scroll;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, left + INFO_X, y, HOLO, false);
            y += font.lineHeight;
        }
        for (int i = 0; i < rewards.size(); i++) {
            int rx = left + INFO_X + 8 + i * 20, ry = y + 2;
            g.renderItem(rewards.get(i), rx, ry);
            g.renderItemDecorations(font, rewards.get(i), rx, ry);
            if (mouseX >= rx && mouseX < rx + 16 && mouseY >= ry && mouseY < ry + 16 && mouseY >= top + INFO_Y && mouseY < top + INFO_Y + INFO_H) {
                g.setTooltipForNextFrame(font, rewards.get(i), mouseX, mouseY);
            }
        }
        g.disableScissor();
        // 1.7.10 ElementScrollGroup scroller in holo colour
        if (maxScroll > 0) {
            int bar = Math.max(8, INFO_H * INFO_H / contentHeight);
            int by = top + INFO_Y + (INFO_H - bar) * scroll / maxScroll;
            g.fill(left + INFO_X + INFO_W + 4, by, left + INFO_X + INFO_W + 6, by + bar, HOLO);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        double mx = event.x() - left, my = event.y() - top;
        if (in(mx, my, WIDTH - 32, 20, 9, 9)) {
            onClose();
            return true;
        }
        int i = 0;
        for (Guides.Category category : Guides.categories().values()) {
            if (in(mx, my, 16 + 32 * i++, BUTTONS_Y, 22, 22)) {
                setState(state.withCategory(category.name()).withPage(DataPadItem.State.PAGE_ENTRIES));
                return true;
            }
        }
        if (in(mx, my, QUESTS_BT[0], QUESTS_BT[1], QUESTS_BT[2], QUESTS_BT[3])) {
            setState(state.withPage(DataPadItem.State.PAGE_QUESTS));
            return true;
        }
        if (state.page() == DataPadItem.State.PAGE_ENTRIES) {
            if (in(mx, my, PAGE_X + PAGE_W - 38, PAGE_Y + 2, 16, 16)) {
                setState(state.withOrdering((Math.clamp(state.ordering(), 0, 2) + 1) % 3));
                return true;
            }
            GuideEntry entry = entryAt(mx, my);
            if (entry != null) {
                openGuide(entry, 0, false);
                return true;
            }
            if (in(mx, my, PAGE_X, PAGE_Y, PAGE_W, PAGE_H) && !search.isMouseOver(event.x(), event.y())) {
                dragging = true;
                lastDragX = event.x();
                lastDragY = event.y();
                search.setFocused(false);
            }
            return super.mouseClicked(event.x(), event.y(), event.button());
        }
        if (state.page() == DataPadItem.State.PAGE_DESCRIPTION) {
            for (GuideElement.Link link : List.copyOf(links)) {
                if (in(event.x(), event.y(), link.x(), link.y(), link.w(), link.h())) {
                    link.action().run();
                    return true;
                }
            }
            int by = PAGE_Y + PAGE_H - 16;
            ensureLoaded();
            if (state.guidePage() > 0 && in(mx, my, PAGE_X + 10, by, 10, 10)) {
                setState(state.withGuide(state.guide(), state.guidePage() - 1));
                return true;
            }
            if (state.guidePage() < pages.size() - 1 && in(mx, my, PAGE_X + PAGE_W - 20, by, 10, 10)) {
                setState(state.withGuide(state.guide(), state.guidePage() + 1));
                return true;
            }
            if (in(mx, my, PAGE_X + PAGE_W / 2 - 5, by, 11, 11)) {
                goBack();
                return true;
            }
            return super.mouseClicked(event.x(), event.y(), event.button());
        }
        List<QuestStack> quests = quests();
        if (in(mx, my, LIST_X, LIST_Y, LIST_W, ROWS * ROW)) {
            int index = listScroll + (int) ((my - LIST_Y) / ROW);
            if (index < quests.size()) setState(state.withQuest(index, 0));
            return true;
        }
        QuestStack selected = selected();
        if (in(mx, my, COMPLETE_BT[0], COMPLETE_BT[1], COMPLETE_BT[2], COMPLETE_BT[3]) && canComplete(selected)) {
            ClientPacketDistributor.sendToServer(new QuestPayloads.QuestAction(QuestPayloads.Action.COMPLETE, quests.indexOf(selected)));
            return true;
        }
        if (in(mx, my, ABANDON_BT[0], ABANDON_BT[1], ABANDON_BT[2], ABANDON_BT[3]) && selected != null) {
            ClientPacketDistributor.sendToServer(new QuestPayloads.QuestAction(QuestPayloads.Action.ABANDON, quests.indexOf(selected)));
            setState(state.withQuest(0, 0));
            return true;
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        if (dragging && state.page() == DataPadItem.State.PAGE_ENTRIES) {
            entriesScrollX += (int) (event.x() - lastDragX);
            entriesScrollY += (int) (event.y() - lastDragY);
            lastDragX = event.x();
            lastDragY = event.y();
            int[] inner = new int[2];
            placeEntries(new java.util.HashMap<>(), inner);
            clampEntriesScroll(inner);
            return true;
        }
        return super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        dragging = false;
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        if (search.isFocused() && event.key() != 256) {
            search.keyPressed(event.key(), event.scancode(), event.modifiers());
            return true;
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = mouseX - left, my = mouseY - top;
        if (state.page() == DataPadItem.State.PAGE_ENTRIES) {
            // 1.7.10 onMouseWheel: scrollY += Lerp(scrollX, scrollX + movement, 0.1) (the wheel moved 120 per notch)
            entriesScrollY += (int) (entriesScrollX + 12 * Math.signum(scrollY));
            int[] inner = new int[2];
            placeEntries(new java.util.HashMap<>(), inner);
            clampEntriesScroll(inner);
            return true;
        }
        if (state.page() != DataPadItem.State.PAGE_QUESTS) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (in(mx, my, INFO_X, INFO_Y, INFO_W + 8, INFO_H)) {
            state = state.withQuest(state.selectedQuest(), Math.max(0, state.scroll() - (int) Math.signum(scrollY) * 9));
            return true;
        }
        if (in(mx, my, LIST_X, LIST_Y, LIST_W, ROWS * ROW)) {
            listScroll = Math.clamp(listScroll - (int) Math.signum(scrollY), 0, Math.max(0, quests().size() - ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 1.7.10 PageActiveQuests.onGuiClose: keeps the info scroll on the pad. */
    @Override
    public void removed() {
        setState(state);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
