package matteroverdrive.client.screen;

import java.util.Locale;

import matteroverdrive.block.entity.FusionReactorControllerBlockEntity;
import matteroverdrive.menu.FusionReactorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7.10 GuiFusionReactor drew round energy/matter gauges; this port shows the same readouts with the standard
 * bars: status, efficiency, +FE/t generated and kM/t of matter burned.
 */
public class FusionReactorScreen extends MachineScreen<FusionReactorMenu> {
    private static final int MATTER_X = 150, ENERGY_X = 176, BAR_Y = 39;
    private static final int COLOR_ENERGY = 0xFFE65014;      // COLOR_HOLO_RED

    public FusionReactorScreen(FusionReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        drawMatter(g, x + MATTER_X, y + BAR_Y);
        drawEnergy(g, x + ENERGY_X, y + BAR_Y);
        FusionReactorControllerBlockEntity reactor = menu.getMachine();
        int line = 0;
        for (String s : reactor.getStatus().split("\n")) {
            g.drawString(font, s, x + 46, y + 38 + line++ * 10, COLOR_TEXT, false);
        }
        int ty = y + 68;
        g.drawString(font, Component.translatable("gui.matteroverdrive.efficiency", Math.round(reactor.getEnergyEfficiency() * 100)),
                x + 46, ty, COLOR_TEXT, false);
        if (reactor.isValidStructure()) {
            g.drawString(font, "+" + reactor.getEnergyPerTick() + " FE/t", x + 46, ty + 10, COLOR_ENERGY, true);
            g.drawString(font, String.format(Locale.ROOT, "-%.4f kM/t", reactor.getMatterPerTick()), x + 46, ty + 20, COLOR_TEXT, true);
        }
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        matterTooltip(g, mx, my, MATTER_X, BAR_Y, mouseX, mouseY);
        energyTooltip(g, mx, my, ENERGY_X, BAR_Y, mouseX, mouseY);
    }
}
