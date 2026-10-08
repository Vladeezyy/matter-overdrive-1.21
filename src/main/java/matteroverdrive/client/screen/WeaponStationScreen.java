package matteroverdrive.client.screen;

import java.util.Map;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOItems;
import matteroverdrive.menu.WeaponStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 GuiWeaponStation: a large preview of the weapon in the middle, its module slots around it with lines to
 * where each module sits on the weapon. Slots the weapon doesn't have are greyed out; empty slots show a holo icon.
 */
public class WeaponStationScreen extends MachineScreen<WeaponStationMenu> {
    private static final String[] MODULE_NAMES = {"battery", "color", "barrel", "sights", "other"};
    private static final ResourceLocation[] HOLO = {tex("holo_battery"), tex("holo_color"), tex("holo_barrel"), tex("holo_sights"), tex("holo_module")};
    private static final int PREVIEW_X = 92, PREVIEW_Y = 24, PREVIEW_SIZE = 64;
    private static final int GREY = 0xB01E1E1E, HOLO_COLOR = ARGB.color(78 * 2, 0xBFE4E6);

    /**
     * 1.7.10 getModuleScreenPosition per weapon (battery, colour, barrel, sights), scaled like the slots. Slots without
     * an entry (and "other") get no line.
     */
    private static Map<Item, int[][]> modulePoints() {
        int[][] rifle = {{152, 52}, {100, 52}, {92, 58}, {132, 47}};
        return Map.of(MOItems.PHASER.get(), new int[][] {{152, 58}, {100, 52}, {88, 58}, null},
                MOItems.PHASER_RIFLE.get(), rifle, MOItems.PLASMA_SHOTGUN.get(), rifle,
                MOItems.ION_SNIPER.get(), new int[][] {{152, 52}, {108, 58}, {92, 62}, {140, 47}},
                MOItems.OMNI_TOOL.get(), new int[][] {{158, 58}, {120, 47}, {88, 67}, null});
    }

    public WeaponStationScreen(WeaponStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        ItemStack weapon = menu.getWeapon();
        if (!weapon.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(x + PREVIEW_X, y + PREVIEW_Y);
            g.pose().scale(PREVIEW_SIZE / 16f, PREVIEW_SIZE / 16f);
            g.renderItem(weapon, 0, 0);
            g.pose().popMatrix();
            int[][] points = modulePoints().get(weapon.getItem());
            for (int i = 0; points != null && i < points.length; i++) {
                if (points[i] == null || !menu.supportsSlot(i)) continue;
                int[] slot = WeaponStationMenu.MODULE_POS[i];
                line(g, x + slot[0] + 8, y + slot[1] + 8, x + points[i][0], y + points[i][1], 0xFFBFE4E6);
            }
        }
        for (Slot slot : menu.slots) {
            if (!(slot instanceof WeaponStationMenu.ModuleSlot module) || !slot.isActive()) continue;
            int sx = x + slot.x, sy = y + slot.y;
            if (!menu.supportsSlot(module.moduleSlot)) {
                g.fill(sx, sy, sx + 16, sy + 16, GREY);
            } else if (!slot.hasItem()) {
                g.blit(RenderPipelines.GUI_TEXTURED, HOLO[module.moduleSlot], sx, sy, 0, 0, 16, 16, 16, 16, HOLO_COLOR);
            }
        }
    }

    /** A 1 px line from slot to module, plotted point by point (GuiGraphics has no line primitive). */
    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + Math.round((x1 - x0) * (i / (float) Math.max(steps, 1)));
            int py = y0 + Math.round((y1 - y0) * (i / (float) Math.max(steps, 1)));
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        for (Slot slot : menu.slots) {
            if (slot instanceof WeaponStationMenu.ModuleSlot module && slot.isActive() && !slot.hasItem()
                    && in(mx, my, slot.x, slot.y, 16, 16)) {
                g.setTooltipForNextFrame(font, Component.translatable("gui." + MatterOverdrive.MODID + ".module." + MODULE_NAMES[module.moduleSlot]),
                        mouseX, mouseY);
            }
        }
    }
}
