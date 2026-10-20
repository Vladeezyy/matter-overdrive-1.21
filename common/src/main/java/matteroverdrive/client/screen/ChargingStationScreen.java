package matteroverdrive.client.screen;

import matteroverdrive.menu.ChargingStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 1.7.10 GuiChargingStation: the stored energy. */
public class ChargingStationScreen extends MachineScreen<ChargingStationMenu> {
    private static final int ENERGY_X = 117, ENERGY_Y = 35;

    public ChargingStationScreen(ChargingStationMenu menu, Inventory playerInventory, Component title) {
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
