package matteroverdrive.client.screen;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.TransporterBlockEntity;
import matteroverdrive.item.TransportFlashDriveItem;
import matteroverdrive.menu.MachineMenu;
import matteroverdrive.menu.TransporterMenu;
import matteroverdrive.network.TransporterPayload;
import matteroverdrive.transport.TransportLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import matteroverdrive.compat.KeyEvent;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 1.7.10 GuiTransporter: the destination list with New/Remove, and the selected destination's name and X/Y/Z (within
 * the transporter's range, offsets from it shown alongside) with Import (from the flash drive) and Reset.
 * 1.7.10 split list and editor over two pages; here both share the home page.
 */
public class TransporterScreen extends MachineScreen<TransporterMenu> {
    private static final ResourceLocation BUTTON = id("button_normal");
    private static final ResourceLocation BUTTON_OVER = id("button_over");
    private static final ResourceLocation BUTTON_DARK = id("button_over_dark");
    private static final int LIST_X = 46, LIST_Y = 26, LIST_W = 128, ROW = 11, ROWS = 7;
    private static final int[] NEW = {46, 106, 40, 14}, REMOVE = {90, 106, 50, 14}, IMPORT = {124, 140, 50, 14}, RESET = {124, 158, 50, 14};
    private static final int FIELD_X = 58, NAME_Y = 124, X_Y = 140, ENERGY_X = 184, ENERGY_Y = 36, MATTER_X = 184, MATTER_Y = 92;

    private EditBox name;
    private final EditBox[] coords = new EditBox[3];
    private int scroll;
    private int shownIndex = -1;
    private TransportLocation shown;
    private boolean updating;

    public TransporterScreen(TransporterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private static ResourceLocation id(String sprite) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, sprite);
    }

    private TransporterBlockEntity machine() {
        return menu.getMachine();
    }

    @Override
    protected void init() {
        super.init();
        name = field(FIELD_X - 12, NAME_Y, 112, Component.translatable("gui.matteroverdrive.transporter.name"));
        name.setMaxLength(TransportLocation.MAX_NAME);
        for (int i = 0; i < 3; i++) {
            coords[i] = field(FIELD_X, X_Y + i * 16, 40, Component.literal("XYZ".substring(i, i + 1)));
            coords[i].setMaxLength(9);
            coords[i].setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d+"));
        }
        shownIndex = -1;
    }

    private EditBox field(int x, int y, int w, Component label) {
        EditBox box = new EditBox(font, leftPos + x + 4, topPos + y + 3, w - 8, 10, label);
        box.setBordered(false);
        box.setTextColor(0xFFBFE4E6);
        box.setResponder(s -> edited());
        addRenderableWidget(box);
        return box;
    }

    /** Typing into the name or a coordinate edits the selected destination (1.7.10 textChanged / field buttons). */
    private void edited() {
        if (updating) return;
        try {
            BlockPos pos = new BlockPos(Integer.parseInt(coords[0].getValue()), Integer.parseInt(coords[1].getValue()),
                    Integer.parseInt(coords[2].getValue()));
            send(TransporterPayload.Action.SET, 0, pos);
        } catch (NumberFormatException ignored) {
        }
    }

    private void send(TransporterPayload.Action action, int index, BlockPos pos) {
        PacketDistributor.sendToServer(new TransporterPayload(menu.containerId, action, index, name.getValue(), pos));
    }

    /** Refreshes the fields when the selection or the server's copy changed (and the user isn't typing in it). */
    private void syncFields() {
        TransportLocation selected = machine().getSelected();
        if (shownIndex == machine().getSelectedIndex() && selected.equals(shown)) return;
        boolean sameEntry = shownIndex == machine().getSelectedIndex();
        shownIndex = machine().getSelectedIndex();
        shown = selected;
        updating = true;
        if (!(sameEntry && name.isFocused())) name.setValue(selected.name());
        int[] v = {selected.pos().getX(), selected.pos().getY(), selected.pos().getZ()};
        for (int i = 0; i < 3; i++) {
            if (!(sameEntry && coords[i].isFocused())) coords[i].setValue(Integer.toString(v[i]));
        }
        updating = false;
    }

    private boolean canImport() {
        BlockPos target = TransportFlashDriveItem.getTarget(machine().getInventory().getStack(TransporterBlockEntity.FLASH_DRIVE));
        return target != null && machine().isInRange(target.above());
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        boolean home = menu.page == MachineMenu.Page.HOME;
        name.visible = home;
        for (EditBox box : coords) box.visible = home;
        syncFields();
        List<TransportLocation> locations = machine().getLocations();
        g.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + ROWS * ROW + 2, 0xFF1E2426);
        for (int i = 0; i < ROWS && scroll + i < locations.size(); i++) {
            int index = scroll + i;
            TransportLocation l = locations.get(index);
            int ry = y + LIST_Y + 1 + i * ROW;
            boolean valid = machine().isLocationValid(l);
            if (index == machine().getSelectedIndex()) g.fill(x + LIST_X + 1, ry, x + LIST_X + LIST_W - 1, ry + ROW, 0xFF3C5A60);
            String c = l.pos().getX() + " " + l.pos().getY() + " " + l.pos().getZ();
            String name = l.name().isEmpty() ? "-" : l.name();
            int room = LIST_W - 10 - font.width(c);
            String label = font.width(name) <= room ? name : font.plainSubstrByWidth(name, room - font.width("…")) + "…";
            g.drawString(font, label, x + LIST_X + 3, ry + 2, valid ? 0xFFFFFFFF : 0xFFE36B6B, false);
            g.drawString(font, c, x + LIST_X + LIST_W - 3 - font.width(c), ry + 2, 0xFF8B9EA0, false);
        }
        button(g, x, y, mx, my, NEW, "gui.matteroverdrive.transporter.new", true);
        button(g, x, y, mx, my, REMOVE, "gui.matteroverdrive.transporter.remove", !locations.isEmpty());
        button(g, x, y, mx, my, IMPORT, "gui.matteroverdrive.transporter.import", canImport());
        button(g, x, y, mx, my, RESET, "gui.matteroverdrive.transporter.reset", !locations.isEmpty());
        matteroverdrive.compat.Gui.blitSprite(g, BUTTON_DARK, x + FIELD_X - 12, y + NAME_Y, 112, 16);
        BlockPos me = machine().getBlockPos();
        int[] mine = {me.getX(), me.getY(), me.getZ()};
        for (int i = 0; i < 3; i++) {
            int fy = y + X_Y + i * 16;
            g.drawString(font, "XYZ".charAt(i) + ":", x + FIELD_X - 12, fy + 4, 0xFFFFFFFF, false);
            matteroverdrive.compat.Gui.blitSprite(g, BUTTON_DARK, x + FIELD_X, fy, 40, 16);
            try {
                int offset = Integer.parseInt(coords[i].getValue()) - mine[i];
                g.drawString(font, (offset > 0 ? "+" : "") + offset, x + FIELD_X + 44, fy + 4, 0xFF8B9EA0, false);
            } catch (NumberFormatException ignored) {
            }
        }
        drawEnergy(g, x + ENERGY_X, y + ENERGY_Y);
        drawMatter(g, x + MATTER_X, y + MATTER_Y);
        drawArrow(g, x + ENERGY_X - 4, y + 140, menu.getProgress());
    }

    private void button(GuiGraphics g, int x, int y, int mx, int my, int[] b, String key, boolean enabled) {
        boolean over = enabled && in(mx, my, b[0], b[1], b[2], b[3]);
        matteroverdrive.compat.Gui.blitSprite(g, !enabled ? BUTTON_DARK : over ? BUTTON_OVER : BUTTON, x + b[0], y + b[1], b[2], b[3]);
        Component text = Component.translatable(key);
        g.drawString(font, text, x + b[0] + (b[2] - font.width(text)) / 2, y + b[1] + (b[3] - 8) / 2, enabled ? 0xFFFFFFFF : 0xFF8B9EA0, false);
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        energyTooltip(g, mx, my, ENERGY_X, ENERGY_Y, mouseX, mouseY);
        matterTooltip(g, mx, my, MATTER_X, MATTER_Y, mouseX, mouseY);
        if (in(mx, my, IMPORT[0], IMPORT[1], IMPORT[2], IMPORT[3]) && !canImport()
                && TransportFlashDriveItem.getTarget(machine().getInventory().getStack(TransporterBlockEntity.FLASH_DRIVE)) != null) {
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.transporter.too_far"), mouseX, mouseY);
        }
        TransportLocation selected = machine().getSelected();
        if (in(mx, my, LIST_X, LIST_Y, LIST_W, ROWS * ROW) && !machine().getLocations().isEmpty()) {
            int index = scroll + (my - LIST_Y - 1) / ROW;
            if (index >= 0 && index < machine().getLocations().size() && !machine().isLocationValid(machine().getLocations().get(index))) {
                matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.transporter.invalid"), mouseX, mouseY);
            }
        } else if (in(mx, my, ENERGY_X - 4, 140, 24, 16)) {
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.transporter.cost", machine().getEnergyDrain(),
                    selected.name()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        if (menu.page == MachineMenu.Page.HOME) {
            double mx = event.x() - leftPos, my = event.y() - topPos;
            List<TransportLocation> locations = machine().getLocations();
            if (in(mx, my, LIST_X, LIST_Y, LIST_W, ROWS * ROW)) {
                int index = scroll + (int) ((my - LIST_Y - 1) / ROW);
                if (index >= 0 && index < locations.size()) send(TransporterPayload.Action.SELECT, index, BlockPos.ZERO);
                return true;
            }
            if (in(mx, my, NEW[0], NEW[1], NEW[2], NEW[3])) {
                send(TransporterPayload.Action.NEW, 0, BlockPos.ZERO);
                return true;
            }
            if (in(mx, my, REMOVE[0], REMOVE[1], REMOVE[2], REMOVE[3]) && !locations.isEmpty()) {
                send(TransporterPayload.Action.REMOVE, machine().getSelectedIndex(), BlockPos.ZERO);
                return true;
            }
            if (in(mx, my, IMPORT[0], IMPORT[1], IMPORT[2], IMPORT[3]) && canImport()) {
                send(TransporterPayload.Action.IMPORT, 0, BlockPos.ZERO);
                return true;
            }
            if (in(mx, my, RESET[0], RESET[1], RESET[2], RESET[3]) && !locations.isEmpty()) {
                send(TransporterPayload.Action.RESET, 0, BlockPos.ZERO);
                return true;
            }
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(Math.max(0, machine().getLocations().size() - ROWS), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
        for (EditBox box : List.of(name, coords[0], coords[1], coords[2])) {
            if (box.isFocused() && event.key() != 256) {
                box.keyPressed(event.key(), event.scancode(), event.modifiers());
                return true;
            }
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
}
