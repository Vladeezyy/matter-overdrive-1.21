package matteroverdrive.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.matternet.MatterNetworkBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 1.7.10 NetworkFlashDrive: use it on a matter network block to add it to (or remove it from) the drive's
 * connections; in a machine's destination filter slot it limits that machine's network traffic to them.
 */
public class NetworkFlashDriveItem extends Item {
    public NetworkFlashDriveItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static List<BlockPos> getConnections(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.NETWORK_FILTER.get(), List.of());
    }

    /** Before the machine opens its screen (1.7.10 onItemUse on an IMatterNetworkConnection). */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        if (!(context.getLevel().getBlockState(pos).getBlock() instanceof MatterNetworkBlock)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            List<BlockPos> connections = new ArrayList<>(getConnections(stack));
            if (!connections.remove(pos)) connections.add(pos.immutable());
            stack.set(MODataComponents.NETWORK_FILTER.get(), List.copyOf(connections));
        }
        return InteractionResult.SUCCESS;
    }

    /** 1.7.10: "[x,y,z] Block name" per connection. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        var level = context.level();
        for (BlockPos pos : getConnections(stack)) {
            Component name = level != null && level.isLoaded(pos) && !level.getBlockState(pos).isAir() ? level.getBlockState(pos).getBlock().getName()
                    : Component.literal("Unknown");
            tooltip.accept(Component.literal("[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "] ").append(name).withStyle(ChatFormatting.GRAY));
        }
    }
}
