package matteroverdrive.client.screen;

import matteroverdrive.menu.PatternStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.7.10 GuiPatternStorage: energy bar at (176, 39). */
public class PatternStorageScreen extends MachineScreen<PatternStorageMenu> {
    private static final int ENERGY_X = 176, ENERGY_Y = 39;

    public PatternStorageScreen(PatternStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        drawEnergy(g, x + ENERGY_X, y + ENERGY_Y);
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        energyTooltip(g, mx, my, ENERGY_X, ENERGY_Y, mouseX, mouseY);
    }
}
