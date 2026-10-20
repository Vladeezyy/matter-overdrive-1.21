package matteroverdrive.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Items used on a block before the block itself reacts (opens its screen): NeoForge calls this as
 * {@code IItemExtension.onItemUseFirst}, Fabric from its block use callback.
 */
public interface UseFirst {
    /** {@code PASS} lets the block (and then the item's useOn) handle the click */
    InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context);
}
