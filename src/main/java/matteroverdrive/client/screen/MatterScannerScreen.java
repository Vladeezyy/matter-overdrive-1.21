package matteroverdrive.client.screen;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.MatterScannerItem;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.matter.MatterRegistry;
import matteroverdrive.network.ScannerPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7.10 GuiMatterScanner (300x230, page "Scan Info"): the patterns of the linked pattern storage on the left, the
 * selected pattern on the right (item, name, matter, progress). Clicking a pattern selects it.
 */
public class MatterScannerScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "machine_background");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/elements/slot_big.png");
    private static final int WIDTH = 300, HEIGHT = 230, GRID_X = 34, GRID_Y = 34, CELL = 22, COLS = 5, ROWS = 8, INFO_X = 160;
    private static final int COLOR_TITLE = 0xFF2C3634, COLOR_TEXT = 0xFFBFE4E6, INFO_W = WIDTH - INFO_X - 24;

    private final int slot;
    private List<ItemPattern> patterns = new ArrayList<>();
    private boolean online;
    private boolean received;
    private int scroll;
    private int left, top;

    public MatterScannerScreen(int slot) {
        super(Component.translatable("item.matteroverdrive.matter_scanner"));
        this.slot = slot;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        ScannerPayloads.onPatterns = p -> {
            if (p.slot() == slot) {
                patterns = new ArrayList<>(p.patterns());
                online = p.online();
                received = true;
            }
        };
        ClientPacketDistributor.sendToServer(new ScannerPayloads.Request(slot));
    }

    @Override
    public void removed() {
        ScannerPayloads.onPatterns = p -> {};
    }

    private ItemStack scanner() {
        return minecraft.player.getInventory().getItem(slot);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ItemStack scanner = scanner();
        if (!(scanner.getItem() instanceof MatterScannerItem)) {
            onClose();
            return;
        }
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, WIDTH, HEIGHT);
        // the title bar like the machine screens
        g.drawString(font, title, left + WIDTH / 2 - font.width(title) / 2, top + 7, COLOR_TITLE, false);
        Component status = MatterScannerItem.getLink(scanner) == null ? Component.translatable("tooltip.matteroverdrive.scanner.offline")
                : !received ? Component.literal("...") : online ? Component.translatable("gui.matteroverdrive.scanner.online")
                : Component.translatable("gui.matteroverdrive.scanner.storage_offline");
        int statusColor = MatterScannerItem.getLink(scanner) != null && online ? 0xFF6BE36B : 0xFFE36B6B;
        g.drawString(font, status, left + INFO_X, top + GRID_Y - 2, statusColor, false);

        int first = scroll * COLS;
        ItemPattern hovered = null;
        for (int i = 0; i < COLS * ROWS; i++) {
            int cx = left + GRID_X + (i % COLS) * CELL, cy = top + GRID_Y + (i / COLS) * CELL;
            g.blit(RenderPipelines.GUI_TEXTURED, SLOT, cx, cy, 0, 0, 22, 22, 22, 22);
            if (first + i >= patterns.size()) continue;
            ItemPattern p = patterns.get(first + i);
            g.renderItem(p.toStack(), cx + 3, cy + 3);
            int w = Math.round(16 * p.progress() / (float) ItemPattern.MAX_PROGRESS);
            g.fill(cx + 3, cy + 19, cx + 3 + w, cy + 20, p.isComplete() ? 0xFF6BE36B : 0xFFE3C46B);
            if (mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL) hovered = p;
        }

        ItemPattern selected = MatterScannerItem.getSelected(scanner);
        int ix = left + INFO_X, iy = top + GRID_Y + 14;
        if (selected != null) {
            ItemStack stack = selected.toStack();
            g.pose().pushMatrix();
            g.pose().translate(ix, iy);
            g.pose().scale(3, 3);
            g.renderItem(stack, 0, 0);
            g.pose().popMatrix();
            int ty = iy + 56;
            for (var line : font.split(stack.getHoverName(), INFO_W)) {
                g.drawString(font, line, ix, ty, 0xFFFFFFFF, false);
                ty += 10;
            }
            ty += 4;
            g.drawString(font, Component.translatable("tooltip.matteroverdrive.matter", MatterRegistry.getAnySide(stack.getItem())), ix, ty, COLOR_TEXT, false);
            ty += 12;
            g.drawString(font, Component.translatable("tooltip.matteroverdrive.scanner.progress", selected.progress()), ix, ty, COLOR_TEXT, false);
            ty += 12;
            int barW = INFO_W;
            g.fill(ix, ty, ix + barW, ty + 4, 0xFF1E2426);
            g.fill(ix, ty, ix + Math.round(barW * selected.progress() / (float) ItemPattern.MAX_PROGRESS), ty + 4,
                    selected.isComplete() ? 0xFF6BE36B : 0xFFE3C46B);
        } else {
            g.drawWordWrap(font, Component.translatable("gui.matteroverdrive.scanner.nothing_selected"), ix, iy, INFO_W, COLOR_TEXT, false);
        }
        if (hovered != null) {
            g.setTooltipForNextFrame(font, Component.translatable("gui.matteroverdrive.pattern", hovered.toStack().getHoverName(), hovered.progress()),
                    mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private @Nullable ItemPattern patternAt(double mx, double my) {
        double x = mx - left - GRID_X, y = my - top - GRID_Y;
        if (x < 0 || y < 0 || x >= COLS * CELL || y >= ROWS * CELL) return null;
        int index = scroll * COLS + (int) (y / CELL) * COLS + (int) (x / CELL);
        return index < patterns.size() ? patterns.get(index) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        ItemPattern p = patternAt(event.x(), event.y());
        if (p != null) {
            ClientPacketDistributor.sendToServer(new ScannerPayloads.Select(slot, p.item()));
            return true;
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rows = (patterns.size() + COLS - 1) / COLS;
        scroll = Math.max(0, Math.min(Math.max(0, rows - ROWS), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
