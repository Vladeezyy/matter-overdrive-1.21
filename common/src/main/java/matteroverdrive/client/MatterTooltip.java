package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** 1.7.10 TooltipHandler: "Matter: N kM" under every item that holds matter. */
public final class MatterTooltip {
    public static void onTooltip(net.minecraft.world.item.ItemStack stack, java.util.List<Component> tooltip) {
        int matter = MatterRegistry.getClient(stack.getItem());
        if (matter > 0) {
            // estimated values (items the 1.7.10 rules can't price) are marked with "~"
            String key = MatterRegistry.isEstimatedClient(stack.getItem()) ? "tooltip.matteroverdrive.matter_estimated" : "tooltip.matteroverdrive.matter";
            tooltip.add(Component.translatable(key, matter).withStyle(ChatFormatting.BLUE));
        }
    }

    private MatterTooltip() {}
}
