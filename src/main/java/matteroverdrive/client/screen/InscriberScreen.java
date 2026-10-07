package matteroverdrive.client.screen;

import matteroverdrive.menu.InscriberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.7.10 GuiInscriber: energy bar at (100, 39), progress arrow at (32, 55), big output slot at (129, 55). */
public class InscriberScreen extends MachineScreen<InscriberMenu> {
    private static final int ENERGY_X = 100, ENERGY_Y = 39;

    public InscriberScreen(InscriberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        drawEnergy(g, x + ENERGY_X, y + ENERGY_Y);
        drawArrow(g, x + 32, y + 58, menu.getProgress());
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        energyTooltip(g, mx, my, ENERGY_X, ENERGY_Y, mouseX, mouseY);
    }
}
