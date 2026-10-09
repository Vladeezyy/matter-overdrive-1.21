package matteroverdrive.client;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.machine.MachineBlockItem;
import matteroverdrive.machine.MachineStorage;
import matteroverdrive.util.MOText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 1.7.10 MOMachineBlockItem.addInformation: with Shift a machine item lists its details, stored energy (and transfer)
 * and stored matter (and transfer), otherwise "Hold Shift for Details.".
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class MachineTooltip {
    /** DevScene: acts as if Shift were held (a script can't press keys). */
    static boolean sceneShift;

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof MachineBlockItem)) return;
        List<Component> lines = new ArrayList<>();
        if (sceneShift || net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            String details = stack.getItem().getDescriptionId() + ".details";
            if (I18n.exists(details)) lines.add(Component.translatable(details).withStyle(ChatFormatting.GRAY));
            MachineStorage storage = stack.get(MODataComponents.MACHINE_STORAGE.get());
            Integer energy = stack.get(MODataComponents.ENERGY.get());
            if (storage != null && energy != null && storage.maxEnergy() > 0) {
                lines.add(Component.literal(MOText.compact(energy) + " / " + MOText.compact(storage.maxEnergy()) + " FE").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.translatable("tooltip.matteroverdrive.send_receive", MOText.compact(storage.energySend()),
                        MOText.compact(storage.energyReceive()), "FE").withStyle(ChatFormatting.GRAY));
            }
            if (storage != null && storage.maxMatter() > 0) {
                lines.add(Component.literal(MOText.compact(storage.matter()) + " / " + MOText.compact(storage.maxMatter()) + " kM").withStyle(ChatFormatting.BLUE));
                lines.add(Component.translatable("tooltip.matteroverdrive.send_receive", MOText.compact(storage.matterSend()),
                        MOText.compact(storage.matterReceive()), "kM").withStyle(ChatFormatting.DARK_BLUE));
            }
        } else {
            // 1.7.10 MOStringHelper.MORE_INFO
            lines.add(Component.translatable("tooltip.matteroverdrive.more_info",
                    Component.literal("Shift").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)).withStyle(ChatFormatting.GRAY));
        }
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }

    private MachineTooltip() {}
}
