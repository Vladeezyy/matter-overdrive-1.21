package matteroverdrive.item;

import matteroverdrive.init.MODataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** 1.7.10 TransportFlashDrive: use it on a block to mark it; a transporter imports the spot above it as a destination. */
public class TransportFlashDriveItem extends Item {
    public TransportFlashDriveItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static @Nullable BlockPos getTarget(ItemStack stack) {
        return stack.get(MODataComponents.TRANSPORT_TARGET.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).isAir()) {
            context.getItemInHand().remove(MODataComponents.TRANSPORT_TARGET.get());
            return InteractionResult.PASS;
        }
        context.getItemInHand().set(MODataComponents.TRANSPORT_TARGET.get(), context.getClickedPos().immutable());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.matteroverdrive.transport_flash_drive.details").withStyle(ChatFormatting.GRAY));
        BlockPos target = getTarget(stack);
        if (target != null) {
            tooltip.accept(Component.literal("[" + target.getX() + "," + target.getY() + "," + target.getZ() + "]").withStyle(ChatFormatting.YELLOW));
        }
    }
}
