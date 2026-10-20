package matteroverdrive.client.screen;

import matteroverdrive.menu.AnalyzerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7.10 GuiMatterAnalyzer: the scanner screen (49, 36) fills with 26 noise bars as the scan progresses;
 * energy bar at (176, 39).
 */
public class AnalyzerScreen extends MachineScreen<AnalyzerMenu> {
    private static final ResourceLocation SCREEN = tex("screen");
    private static final int SCREEN_X = 49, SCREEN_Y = 36, ENERGY_X = 176, ENERGY_Y = 39;
    private static final int BARS = 26, MARGIN_LEFT = 7, MARGIN_TOP = 8, MAX_HEIGHT = 32;

    public AnalyzerScreen(AnalyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        g.blit(RenderPipelines.GUI_TEXTURED, SCREEN, x + SCREEN_X, y + SCREEN_Y, 0, 0, 117, 47, 117, 47);
        int filled = Mth.floor(menu.getProgress() * BARS);
        long seed = menu.getMachine().getBlockPos().asLong();
        for (int i = 0; i < filled; i++) {
            // 1.7.10 used smoothed noise per bar; a fixed per-machine hash keeps the bars steady between frames
            double n = (Mth.sin((float) (i * 0.7 + (seed % 97))) + Mth.sin((float) (i * 0.23 + (seed % 13)))) / 4 + 0.5;
            int h = Math.max(2, (int) (n * MAX_HEIGHT));
            int bx = x + SCREEN_X + MARGIN_LEFT + i * 4;
            int by = y + SCREEN_Y + MARGIN_TOP + MAX_HEIGHT - h;
            g.fill(bx, by, bx + 3, by + h, 0xFFBFE4E6);
        }
        drawEnergy(g, x + ENERGY_X, y + ENERGY_Y);
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        energyTooltip(g, mx, my, ENERGY_X, ENERGY_Y, mouseX, mouseY);
    }
}
