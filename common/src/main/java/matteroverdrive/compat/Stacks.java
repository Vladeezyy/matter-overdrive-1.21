package matteroverdrive.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** 1.20.5+ ItemStack calls missing from 1.20.1 */
public final class Stacks {
    private Stacks() {}

    /** {@code stack.transmuteCopy(item, count)}: another item with the same tag */
    public static ItemStack transmuteCopy(ItemStack stack, ItemLike item, int count) {
        ItemStack copy = new ItemStack(item, count);
        if (stack.getTag() != null) copy.setTag(stack.getTag().copy());
        return copy;
    }
}
