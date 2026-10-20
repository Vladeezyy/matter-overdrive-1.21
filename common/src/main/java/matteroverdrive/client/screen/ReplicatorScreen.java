package matteroverdrive.client.screen;

import matteroverdrive.block.entity.ReplicatorBlockEntity;
import matteroverdrive.menu.ReplicatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 GuiReplicator: matter bar (141, 39), energy bar (167, 39), progress arrow (32, 52) and the current
 * task's pattern in a wide slot (slot_big_main) at the top of the slot list.
 */
public class ReplicatorScreen extends MachineScreen<ReplicatorMenu> {
    private static final ResourceLocation SLOT_MAIN = tex("slot_big_main");
    private static final int MATTER_X = 141, ENERGY_X = 167, BAR_Y = 39, PATTERN_X = 5, PATTERN_Y = 49;

    public ReplicatorScreen(ReplicatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderHome(GuiGraphics g, int x, int y, int mx, int my) {
        drawMatter(g, x + MATTER_X, y + BAR_Y);
        drawEnergy(g, x + ENERGY_X, y + BAR_Y);
        drawArrow(g, x + 32, y + 55, menu.getProgress());
        g.blit(RenderPipelines.GUI_TEXTURED, SLOT_MAIN, x + PATTERN_X, y + PATTERN_Y, 0, 0, 37, 22, 37, 22);
        menu.getMachine().clientTask().ifPresent(task -> {
            ItemStack stack = task.pattern().toStack().copyWithCount(task.count());
            g.renderItem(stack, x + PATTERN_X + 3, y + PATTERN_Y + 3);
            g.renderItemDecorations(font, stack, x + PATTERN_X + 3, y + PATTERN_Y + 3);
        });
    }

    @Override
    protected void homeTooltips(GuiGraphics g, int mx, int my, int mouseX, int mouseY) {
        matterTooltip(g, mx, my, MATTER_X, BAR_Y, mouseX, mouseY);
        energyTooltip(g, mx, my, ENERGY_X, BAR_Y, mouseX, mouseY);
        if (in(mx, my, PATTERN_X, PATTERN_Y, 37, 22)) {
            menu.getMachine().clientTask().ifPresent(task -> g.setTooltipForNextFrame(font, Component.translatable(
                    "gui.matteroverdrive.replicating", task.pattern().toStack().getHoverName(), task.count(), task.pattern().progress()),
                    mouseX, mouseY));
        }
    }
}
