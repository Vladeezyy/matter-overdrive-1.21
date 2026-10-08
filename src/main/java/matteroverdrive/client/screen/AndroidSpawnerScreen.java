package matteroverdrive.client.screen;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.AndroidSpawnerBlockEntity;
import matteroverdrive.menu.AndroidSpawnerMenu;
import matteroverdrive.menu.MachineMenu;
import matteroverdrive.network.AndroidSpawnerPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 1.7.10 GuiAndroidSpawner: flash drive slots for the path, "Kill All", spawned/max and the time to the next spawn; the
 * Config page has the spawn amount / range / delay and the team (1.7.10 config properties).
 */
public class AndroidSpawnerScreen extends MachineScreen<AndroidSpawnerMenu> {
    private static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "button_normal");
    private static final ResourceLocation BUTTON_OVER = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "button_over");
    private static final ResourceLocation BUTTON_DARK = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "button_over_dark");
    private static final int[] KILL_ALL = {64, 60, 60, 20};
    private static final String[] FIELDS = {"spawn_amount", "spawn_range", "spawn_delay", "team"};
    private static final int FIELD_X = 130, FIELD_Y = 80, FIELD_W = 60, ROW = 18;

    private final EditBox[] fields = new EditBox[4];
    private boolean updating;
    private String shown = "";

    public AndroidSpawnerScreen(AndroidSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private AndroidSpawnerBlockEntity machine() {
        return menu.getMachine();
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < fields.length; i++) {
            EditBox box = new EditBox(font, leftPos + FIELD_X + 4, topPos + FIELD_Y + i * ROW + 3, FIELD_W - 8, 10, Component.literal(FIELDS[i]));
            box.setBordered(false);
            box.setTextColor(COLOR_TEXT);
            if (i < 3) {
                box.setMaxLength(6);
                box.setFilter(s -> s.matches("\\d*"));
            } else {
                box.setMaxLength(AndroidSpawnerBlockEntity.MAX_TEAM_LENGTH);
            }
            box.setResponder(s -> edited());
            addRenderableWidget(box);
            fields[i] = box;
        }
        shown = "";
    }

    private String serverValues() {
        AndroidSpawnerBlockEntity m = machine();
        return m.getMaxSpawnAmount() + "|" + m.getSpawnRange() + "|" + m.getSpawnDelay() + "|" + m.getTeamName();
    }

    private void syncFields() {
        String now = serverValues();
        if (now.equals(shown)) return;
        shown = now;
        AndroidSpawnerBlockEntity m = machine();
        String[] values = {Integer.toString(m.getMaxSpawnAmount()), Integer.toString(m.getSpawnRange()), Integer.toString(m.getSpawnDelay()),
                m.getTeamName()};
        updating = true;
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].isFocused()) fields[i].setValue(values[i]);
        }
        updating = false;
    }

    private void edited() {
        if (updating) return;
        try {
            ClientPacketDistributor.sendToServer(new AndroidSpawnerPayload(menu.containerId, Integer.parseInt(fields[0].getValue()),
                    Integer.parseInt(fields[1].getValue()), Integer.parseInt(fields[2].getValue()), fields[3].getValue()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        boolean config = menu.page == MachineMenu.Page.CONFIG;
        for (EditBox box : fields) box.visible = config;
        syncFields();
        super.renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        boolean over = in(mx, my, KILL_ALL[0], KILL_ALL[1], KILL_ALL[2], KILL_ALL[3]);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, over ? BUTTON_OVER : BUTTON, x + KILL_ALL[0], y + KILL_ALL[1], KILL_ALL[2], KILL_ALL[3]);
        Component kill = Component.translatable("gui.matteroverdrive.android_spawner.kill_all");
        g.drawString(font, kill, x + KILL_ALL[0] + (KILL_ALL[2] - font.width(kill)) / 2, y + KILL_ALL[1] + 6, 0xFFFFFFFF, false);
        g.drawString(font, menu.getSpawnedCount() + "/" + machine().getMaxSpawnAmount(), x + 130, y + 66, COLOR_TEXT, false);
        int delay = machine().getSpawnDelay();
        if (delay > 0 && minecraft.level != null) {
            int ticks = delay - (int) (minecraft.level.getGameTime() % delay);
            g.drawString(font, Component.translatable("gui.matteroverdrive.android_spawner.next_spawn", formatRemainingTime(ticks / 20f)),
                    x + 54, y + 84, COLOR_TEXT, false);
        }
    }

    /** 1.7.10 MOStringHelper.formatRemainingTime. */
    static String formatRemainingTime(float seconds) {
        if (seconds > 3600) return Math.round(seconds / 3600) + " hr";
        if (seconds > 60) return Math.round(seconds / 60) + " min";
        return Math.round(seconds) + " sec";
    }

    @Override
    protected void renderConfigExtra(GuiGraphics g, int x, int y, int mx, int my) {
        for (int i = 0; i < fields.length; i++) {
            int fy = y + FIELD_Y + i * ROW;
            g.drawString(font, Component.translatable("gui.matteroverdrive.config." + FIELDS[i]), x + 50, fy + 4, COLOR_TEXT, false);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON_DARK, x + FIELD_X, fy, FIELD_W, 16);
        }
        if (!machine().isTeamValid()) {
            g.drawString(font, Component.translatable("gui.matteroverdrive.android_spawner.no_team"), x + 50, y + FIELD_Y + 4 * ROW + 2,
                    0xFFE36B6B, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x() - leftPos, my = event.y() - topPos;
        if (menu.page == MachineMenu.Page.HOME && in(mx, my, KILL_ALL[0], KILL_ALL[1], KILL_ALL[2], KILL_ALL[3])) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AndroidSpawnerMenu.BUTTON_KILL_ALL);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (EditBox box : List.of(fields)) {
            if (box.isFocused() && event.key() != 256) {
                box.keyPressed(event);
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
