package matteroverdrive.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import matteroverdrive.block.entity.ReplicatorBlockEntity.Task;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.menu.MachineMenu;
import matteroverdrive.menu.PatternMonitorMenu;
import matteroverdrive.network.PatternRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import matteroverdrive.compat.KeyEvent;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 GuiPatternMonitor: a searchable grid of every pattern on the network (48, 40, 160x110). Left click adds
 * one copy to the order (shift: 16, max 64), right click removes; Request sends the order, Refresh reloads.
 */
public class PatternMonitorScreen extends MachineScreen<PatternMonitorMenu> {
    private static final ResourceLocation SLOT = tex("slot_big");
    private static final ResourceLocation REFRESH = tex("refresh");
    private static final ResourceLocation REQUEST = tex("request");
    private static final ResourceLocation SEARCH_FIELD = tex("search_field");
    private static final int SEARCH_X = 41, SEARCH_Y = 24;
    private static final int GRID_X = 48, GRID_Y = 40, CELL = 22, COLS = 7, ROWS = 5;
    private static final int REFRESH_X = 6, REFRESH_Y = 45, REQUEST_X = 6, REQUEST_Y = 75, BUTTON = 22;

    private final Map<Item, Integer> order = new HashMap<>();
    private EditBox search;
    private int scroll;

    public PatternMonitorScreen(PatternMonitorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // 1.7.10 MOElementTextField: text over the search_field texture at (41, 26, 167x14)
        search = new EditBox(font, leftPos + SEARCH_X + 16, topPos + SEARCH_Y + 3, 145, 10, Component.translatable("gui.matteroverdrive.search"));
        search.setHint(Component.translatable("gui.matteroverdrive.search").withColor(0xFF8B9EA0));
        search.setBordered(false);
        search.setTextColor(0xFFBFE4E6);
        addRenderableWidget(search);
        // ask the server for the network's patterns (also how Refresh works)
        PacketDistributor.sendToServer(new PatternRequestPayload(menu.containerId, List.of()));
    }

    private List<ItemPattern> visible() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        List<ItemPattern> out = new ArrayList<>();
        for (ItemPattern p : menu.getClientPatterns()) {
            if (q.isEmpty() || p.toStack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) out.add(p);
        }
        return out;
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        search.visible = true;
        matteroverdrive.compat.Gui.blit(g, SEARCH_FIELD, x + SEARCH_X, y + SEARCH_Y, 0, 0, 166, 14, 166, 14);
        List<ItemPattern> patterns = visible();
        int first = scroll * COLS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int cx = x + GRID_X + (i % COLS) * CELL, cy = y + GRID_Y + (i / COLS) * CELL;
            matteroverdrive.compat.Gui.blit(g, SLOT, cx, cy, 0, 0, 22, 22, 22, 22);
            if (first + i >= patterns.size()) continue;
            ItemPattern p = patterns.get(first + i);
            g.renderItem(p.toStack(), cx + 3, cy + 3);
            int amount = order.getOrDefault(p.item().value(), 0);
            if (amount > 0) {
                String s = Integer.toString(amount);
                g.drawString(font, s, cx + 20 - font.width(s), cy + 13, 0xFFFFFFFF, true);
            }
            // pattern completeness as a thin bar under the item
            int w = Math.round(16 * p.progress() / (float) ItemPattern.MAX_PROGRESS);
            g.fill(cx + 3, cy + 19, cx + 3 + w, cy + 20, p.isComplete() ? 0xFF6BE36B : 0xFFE3C46B);
        }
        boolean overRefresh = in(mx, my, REFRESH_X, REFRESH_Y, BUTTON, BUTTON);
        boolean overRequest = in(mx, my, REQUEST_X, REQUEST_Y, BUTTON, BUTTON);
        matteroverdrive.compat.Gui.blit(g, REFRESH, x + REFRESH_X, y + REFRESH_Y, overRefresh ? 22 : 0, 0, 22, 22, 44, 22);
        matteroverdrive.compat.Gui.blit(g, REQUEST, x + REQUEST_X, y + REQUEST_Y, overRequest ? 22 : 0, 0, 22, 22, 44, 22);
        int queued = menu.getClientQueue().stream().mapToInt(Task::count).sum();
        g.drawString(font, Component.translatable("gui.matteroverdrive.queue", menu.getClientQueue().size(), queued),
                x + GRID_X, y + GRID_Y + ROWS * CELL + 2, COLOR_TEXT, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (search != null) search.visible = menu.page == MachineMenu.Page.HOME;
        super.renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        ItemPattern p = patternAt(mx, my);
        if (p != null) {
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.pattern",
                    p.toStack().getHoverName(), p.progress()), mouseX, mouseY);
        } else if (in(mx, my, REFRESH_X, REFRESH_Y, BUTTON, BUTTON)) {
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.refresh"), mouseX, mouseY);
        } else if (in(mx, my, REQUEST_X, REQUEST_Y, BUTTON, BUTTON)) {
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.request"), mouseX, mouseY);
        }
    }

    private ItemPattern patternAt(double mx, double my) {
        if (!in(mx, my, GRID_X, GRID_Y, COLS * CELL, ROWS * CELL)) return null;
        int index = scroll * COLS + (int) ((my - GRID_Y) / CELL) * COLS + (int) ((mx - GRID_X) / CELL);
        List<ItemPattern> patterns = visible();
        return index < patterns.size() ? patterns.get(index) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        if (menu.page == MachineMenu.Page.HOME) {
            double mx = event.x() - leftPos, my = event.y() - topPos;
            ItemPattern p = patternAt(mx, my);
            if (p != null) {
                int step = event.hasShiftDown() ? 16 : 1;
                int delta = event.button() == 1 ? -step : step;
                order.merge(p.item().value(), delta, (a, b) -> Math.max(0, Math.min(64, a + b)));
                return true;
            }
            if (in(mx, my, REQUEST_X, REQUEST_Y, BUTTON, BUTTON)) {
                List<Task> requests = new ArrayList<>();
                for (ItemPattern pattern : menu.getClientPatterns()) {
                    int amount = order.getOrDefault(pattern.item().value(), 0);
                    if (amount > 0) requests.add(new Task(pattern, amount));
                }
                order.clear();
                PacketDistributor.sendToServer(new PatternRequestPayload(menu.containerId, requests));
                return true;
            }
            if (in(mx, my, REFRESH_X, REFRESH_Y, BUTTON, BUTTON)) {
                PacketDistributor.sendToServer(new PatternRequestPayload(menu.containerId, List.of()));
                return true;
            }
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rows = (visible().size() + COLS - 1) / COLS;
        scroll = Math.max(0, Math.min(Math.max(0, rows - ROWS), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        if (search.isFocused()) {
            if (event.key() == 256) {          // escape still closes
                return super.keyPressed(event.key(), event.scancode(), event.modifiers());
            }
            search.keyPressed(event.key(), event.scancode(), event.modifiers());
            scroll = 0;
            return true;
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
}
