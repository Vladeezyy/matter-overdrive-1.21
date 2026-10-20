package matteroverdrive.client.screen;

import matteroverdrive.menu.DecomposerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.7.10 GuiDecomposer: matter bar (74, 39), energy bar (100, 39), progress arrow (32, 55). */
public class DecomposerScreen extends MachineScreen<DecomposerMenu> {
    private static final int MATTER_X = 74, ENERGY_X = 100, BAR_Y = 39;

    public DecomposerScreen(DecomposerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        drawMatter(g, x + MATTER_X, y + BAR_Y);
        drawEnergy(g, x + ENERGY_X, y + BAR_Y);
        drawArrow(g, x + 32, y + 58, menu.getProgress());
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        matterTooltip(g, mx, my, MATTER_X, BAR_Y, mouseX, mouseY);
        energyTooltip(g, mx, my, ENERGY_X, BAR_Y, mouseX, mouseY);
    }
}
