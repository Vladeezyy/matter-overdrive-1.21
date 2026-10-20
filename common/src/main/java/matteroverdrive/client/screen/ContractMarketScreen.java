package matteroverdrive.client.screen;

import matteroverdrive.menu.ContractMarketMenu;
import matteroverdrive.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
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
            graphicsForSlots.blit(RenderPipelines.GUI_TEXTURED, tex("slot_small"), leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 18, 18, 18);
            return false;
        }
        return true;
    }

    private GuiGraphicsExtractor graphicsForSlots;

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        graphicsForSlots = g;
        super.extractBackground(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderHome(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        g.text(font, Component.translatable("gui.matteroverdrive.contract_market.next_quest",
                AndroidSpawnerScreen.formatRemainingTime(menu.getSecondsUntilNextQuest())), x + 64, y + 30, COLOR_GUI_LIGHT, false);
    }
}
