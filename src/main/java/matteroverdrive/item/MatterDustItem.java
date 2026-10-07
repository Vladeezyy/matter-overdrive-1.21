package matteroverdrive.item;

import java.util.function.Consumer;

import matteroverdrive.init.MODataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * 1.7.10 MatterDust. A failed decomposition or replication leaves matter dust carrying the item's matter; the
 * Matter Recycler refines it, and refined dust decomposes into exactly that much matter.
 */
public class MatterDustItem extends Item {
    private final boolean refined;

    public MatterDustItem(boolean refined, Properties properties) {
        super(properties);
        this.refined = refined;
    }

    public boolean isRefined() {
        return refined;
    }

    public static int getMatter(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.MATTER.get(), 0);
    }

    public static ItemStack withMatter(Item dust, int matter) {
        ItemStack stack = new ItemStack(dust);
        stack.set(MODataComponents.MATTER.get(), matter);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int matter = getMatter(stack);
        if (matter > 0) {
            tooltip.accept(Component.translatable("tooltip.matteroverdrive.matter", matter).withStyle(ChatFormatting.BLUE));
        }
        if (!refined) {
            tooltip.accept(Component.translatable("item.matteroverdrive.matter_dust.details").withStyle(ChatFormatting.GRAY));
        }
    }
}
