package matteroverdrive.client.screen;

import matteroverdrive.menu.ContractMarketMenu;
import matteroverdrive.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 1.7.10 GuiContractMarket: the contracts in small slots and "Next Quest in: ...". */
public class ContractMarketScreen extends MachineScreen<ContractMarketMenu> {
    private static final int COLOR_GUI_LIGHT = 0xFF8B9EA0;

    public ContractMarketScreen(ContractMarketMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected boolean drawSlotBackground(Slot slot) {
        if (slot instanceof MachineMenu<?>.PageSlot) {
            matteroverdrive.compat.Gui.blit(graphicsForSlots, tex("slot_small"), leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 18, 18, 18);
            return false;
        }
        return true;
    }

    private GuiGraphics graphicsForSlots;

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        graphicsForSlots = g;
        super.renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, Component.translatable("gui.matteroverdrive.contract_market.next_quest",
                AndroidSpawnerScreen.formatRemainingTime(menu.getSecondsUntilNextQuest())), x + 64, y + 30, COLOR_GUI_LIGHT, false);
    }
}
