package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 1.7.10 TooltipHandler: "Matter: N kM" under every item that holds matter. */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class MatterTooltip {
    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        int matter = MatterRegistry.getClient(event.getItemStack().getItem());
        if (matter > 0) {
            event.getToolTip().add(Component.translatable("tooltip.matteroverdrive.matter", matter).withStyle(ChatFormatting.BLUE));
        }
    }

    private MatterTooltip() {}
}
