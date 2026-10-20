package matteroverdrive.client.screen;

import matteroverdrive.block.entity.SolarPanelBlockEntity;
import matteroverdrive.menu.SolarPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.7.10 GuiSolarPanel: an energy bar at (117, 35) showing the current generation rate. */
public class SolarPanelScreen extends MachineScreen<SolarPanelMenu> {
    private static final int ENERGY_X = 117, ENERGY_Y = 35;

    public SolarPanelScreen(SolarPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        drawEnergy(g, x + ENERGY_X, y + ENERGY_Y);
        int rate = Math.round(menu.getProgress() * SolarPanelBlockEntity.CHARGE_AMOUNT);
        Component text = Component.translatable("gui.matteroverdrive.generating", rate);
        g.drawString(font, text, x + ENERGY_X + 8 - font.width(text) / 2, y + ENERGY_Y + 46, COLOR_TEXT, false);
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        energyTooltip(g, mx, my, ENERGY_X, ENERGY_Y, mouseX, mouseY);
    }
}
