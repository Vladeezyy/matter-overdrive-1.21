package matteroverdrive.client.screen;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.UpgradeItem;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.MachineMenu;
import matteroverdrive.util.MOText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The 1.7.10 MOGuiMachine look: stretched base_gui_hotbar background (225x186), page buttons on the right
 * (Home / Upgrades / Configurations), machine slots on Home, an activity indicator and a close button.
 */
public abstract class MachineScreen<M extends MachineMenu<?>> extends AbstractContainerScreen<M> {
    protected static final int COLOR_TITLE = 0xFF2C3634;          // 1.7.10 Color(44, 54, 52)
    protected static final int COLOR_TEXT = 0xFFBFE4E6;           // COLOR_MATTER
    private static final ResourceLocation BACKGROUND = id("machine_background");

    protected static final ResourceLocation ENERGY = tex("energy");
    protected static final ResourceLocation ARROW = tex("progress_arrow_right");
    protected static final ResourceLocation MATTER = tex("matter");
    private static final ResourceLocation SLOT_BIG = tex("slot_big");
    private static final ResourceLocation SLOT_SMALL = tex("slot_small");
    private static final ResourceLocation INDICATOR = tex("indicator");
    private static final ResourceLocation CLOSE = tex("close_button");
    private static final ResourceLocation PAGE_BUTTON = tex("page_button");
    private static final ResourceLocation[] PAGE_ICONS = {tex("page_icon_home"), tex("page_icon_upgrades"), tex("page_icon_config")};
    private static final int[] PAGE_ICON_SIZES = {14, 12, 16};

    private static final int CLOSE_X = MachineMenu.WIDTH - 17, CLOSE_Y = 6;
    // The background's right 12 px are transparent (1.7.10 kept its side panel there); the tabs hang off the frame edge, below the title bar (which reaches y = 35 there).
    private static final int PAGES_X = MachineMenu.WIDTH - 14, PAGES_Y = 38;
    private static final int REDSTONE_X = 50, REDSTONE_Y = 50, REDSTONE_W = 140, REDSTONE_H = 20;

    protected MachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = MachineMenu.WIDTH;
        imageHeight = MachineMenu.HEIGHT;
    }

    protected static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/elements/" + name + ".png");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private List<MachineMenu.Page> pages() {
        return hasUpgradeSlots()
                ? List.of(MachineMenu.Page.HOME, MachineMenu.Page.UPGRADES, MachineMenu.Page.CONFIG)
                : List.of(MachineMenu.Page.HOME, MachineMenu.Page.CONFIG);
    }

    private boolean hasUpgradeSlots() {
        for (Slot slot : menu.slots) {
            if (slot instanceof MachineMenu<?>.PageSlot ps && ps.role() == matteroverdrive.machine.MachineInventory.Role.UPGRADE) return true;
        }
        return false;
    }

    // --- rendering -------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        renderExtraTooltips(g, mouseX - leftPos, mouseY - topPos, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, imageWidth, imageHeight);

        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot instanceof MachineMenu<?>.PageSlot) {
                g.blit(RenderPipelines.GUI_TEXTURED, SLOT_BIG, x + slot.x - 3, y + slot.y - 3, 0, 0, 22, 22, 22, 22);
            } else {
                g.blit(RenderPipelines.GUI_TEXTURED, SLOT_SMALL, x + slot.x - 1, y + slot.y - 1, 0, 0, 18, 18, 18, 18);
            }
        }

        // page buttons
        List<MachineMenu.Page> pages = pages();
        for (int i = 0; i < pages.size(); i++) {
            MachineMenu.Page page = pages.get(i);
            int bx = x + PAGES_X, by = y + PAGES_Y + i * 26;
            boolean selected = menu.page == page;
            g.blit(RenderPipelines.GUI_TEXTURED, PAGE_BUTTON, bx, by, selected ? 24 : 0, 0, 24, 24, 48, 24);
            int icon = page.ordinal();
            int size = PAGE_ICON_SIZES[icon];
            g.blit(RenderPipelines.GUI_TEXTURED, PAGE_ICONS[icon], bx + (24 - size) / 2, by + (24 - size) / 2, 0, 0, size, size, size, size,
                    selected ? COLOR_TEXT : 0xFF8B9EA0);
        }

        // indicator (1.7.10 ElementIndicator: 21x5 frames, 0 idle / 1 active)
        g.blit(RenderPipelines.GUI_TEXTURED, INDICATOR, x + 6, y + imageHeight - 18, 0, menu.isActive() ? 5 : 0, 21, 5, 21, 15);

        // close button
        boolean overClose = in(mouseX - x, mouseY - y, CLOSE_X, CLOSE_Y, 9, 9);
        g.blit(RenderPipelines.GUI_TEXTURED, CLOSE, x + CLOSE_X, y + CLOSE_Y, overClose ? 9 : 0, 0, 9, 9, 18, 9);

        switch (menu.page) {
            case HOME -> renderHome(g, x, y, mouseX - x, mouseY - y);
            case UPGRADES -> renderUpgrades(g, x, y);
            case CONFIG -> renderConfig(g, x, y, mouseX - x, mouseY - y);
        }
    }

    /** Draws the machine's Home page; x/y are the GUI origin, mx/my the mouse relative to it. */
    protected abstract void renderHome(GuiGraphics g, int x, int y, int mx, int my);

    /** Extra tooltips for the Home page; mx/my relative to the GUI origin. */
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {}

    private void renderExtraTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        List<MachineMenu.Page> pages = pages();
        for (int i = 0; i < pages.size(); i++) {
            if (in(mx, my, PAGES_X, PAGES_Y + i * 26, 24, 24)) {
                g.setTooltipForNextFrame(font, Component.translatable("gui.matteroverdrive.page." + pages.get(i).name().toLowerCase(java.util.Locale.ROOT)), mouseX, mouseY);
            }
        }
        if (menu.page == MachineMenu.Page.HOME) {
            homeTooltips(g, mx, my, mouseX, mouseY);
        }
    }

    private void renderUpgrades(GuiGraphics g, int x, int y) {
        int line = 0;
        for (UpgradeType type : UpgradeType.values()) {
            if (!menu.getMachine().getAffectedBy().contains(type)) continue;
            double m = menu.getMachine().getUpgradeMultiplier(type);
            Component text = Math.abs(m - 1) < 1e-6
                    ? Component.literal("0% ").append(Component.translatable(type.translationKey())).withColor(0xFF8B9EA0)
                    : UpgradeItem.describe(type, m);
            g.drawString(font, text, x + 48, y + 84 + line * 11, 0xFFFFFFFF, false);
            line++;
        }
    }

    private void renderConfig(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, Component.translatable("gui.matteroverdrive.config.redstone"), x + REDSTONE_X, y + REDSTONE_Y - 12, COLOR_TEXT, false);
        boolean over = in(mx, my, REDSTONE_X, REDSTONE_Y, REDSTONE_W, REDSTONE_H);
        g.fill(x + REDSTONE_X, y + REDSTONE_Y, x + REDSTONE_X + REDSTONE_W, y + REDSTONE_Y + REDSTONE_H, over ? 0xFF4F6669 : 0xFF3E5154);
        g.submitOutline(x + REDSTONE_X, y + REDSTONE_Y, REDSTONE_W, REDSTONE_H, 0xFF22282A);
        Component mode = Component.translatable(menu.getRedstoneMode().translationKey());
        g.drawString(font, mode, x + REDSTONE_X + (REDSTONE_W - font.width(mode)) / 2, y + REDSTONE_Y + 6, 0xFFFFFFFF, false);
    }

    /** 1.7.10 MOElementEnergy: 16x42 bar from energy.png (left half empty, right half full). */
    protected void drawEnergy(GuiGraphics g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, ENERGY, x, y, 0, 0, 16, 42, 32, 64);
        int capacity = menu.getCapacity();
        int h = capacity > 0 ? (int) ((long) menu.getEnergy() * 42 / capacity) : 0;
        if (h > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY, x, y + 42 - h, 16, 42 - h, 16, h, 32, 64);
        }
    }

    protected void energyTooltip(GuiGraphics g, int mx, int my, int barX, int barY, int mouseX, int mouseY) {
        if (in(mx, my, barX, barY, 16, 42)) {
            g.setTooltipForNextFrame(font, Component.translatable("tooltip.matteroverdrive.energy_stored",
                    MOText.energy(menu.getEnergy()), MOText.energy(menu.getCapacity())), mouseX, mouseY);
        }
    }

    /** 1.7.10 ElementMatterStored: same 16x42 layout as the energy bar, from matter.png. */
    protected void drawMatter(GuiGraphics g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, MATTER, x, y, 0, 0, 16, 42, 32, 64);
        int capacity = menu.getMatterCapacity();
        int h = capacity > 0 ? (int) ((long) menu.getMatter() * 42 / capacity) : 0;
        if (h > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, MATTER, x, y + 42 - h, 16, 42 - h, 16, h, 32, 64);
        }
    }

    protected void matterTooltip(GuiGraphics g, int mx, int my, int barX, int barY, int mouseX, int mouseY) {
        if (in(mx, my, barX, barY, 16, 42)) {
            g.setTooltipForNextFrame(font, Component.translatable("tooltip.matteroverdrive.matter_stored",
                    menu.getMatter(), menu.getMatterCapacity()), mouseX, mouseY);
        }
    }

    /** 1.7.10 ElementDualScaled with Progress_Arrow_Right: 24x16, empty frame then full frame. */
    protected void drawArrow(GuiGraphics g, int x, int y, float progress) {
        g.blit(RenderPipelines.GUI_TEXTURED, ARROW, x, y, 0, 0, 24, 16, 48, 16);
        int w = Math.round(progress * 24);
        if (w > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, ARROW, x, y, 24, 0, w, 16, 48, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 125 - font.width(title) / 2, 7, COLOR_TITLE, false);
    }

    protected static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // --- input -----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x() - leftPos, my = event.y() - topPos;
        if (in(mx, my, CLOSE_X, CLOSE_Y, 9, 9)) {
            onClose();
            return true;
        }
        List<MachineMenu.Page> pages = pages();
        for (int i = 0; i < pages.size(); i++) {
            if (in(mx, my, PAGES_X, PAGES_Y + i * 26, 24, 24)) {
                menu.page = pages.get(i);
                return true;
            }
        }
        if (menu.page == MachineMenu.Page.CONFIG && in(mx, my, REDSTONE_X, REDSTONE_Y, REDSTONE_W, REDSTONE_H)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.BUTTON_REDSTONE);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
