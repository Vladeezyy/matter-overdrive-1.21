package matteroverdrive.platform.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Helpers over {@link IItemHandler} (the calls of NeoForge's {@code ItemHandlerHelper} the game code uses) */
public final class ItemHandlers {
    private ItemHandlers() {}

    /** Inserts into the first slots that take it; returns the rest */
    public static ItemStack insertItem(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            stack = handler.insertItem(i, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /** Inserts into slots holding the same item first, then into empty ones; returns the rest */
    public static ItemStack insertItemStacked(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertItem(handler, stack, simulate);
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                stack = handler.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                stack = handler.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    /** Puts the stack into the player's inventory, drops what does not fit */
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            ItemEntity drop = player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            if (drop != null) {
                drop.setNoPickUpDelay();
                drop.setTarget(player.getUUID());
            }
        }
    }

    static ItemStack insertIntoContainerSlot(Container container, int slot, ItemStack stack, boolean simulate, int slotLimit, boolean valid) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!valid) {
            return stack;
        }
        ItemStack existing = container.getItem(slot);
        int limit = Math.min(slotLimit, stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }
        int moved = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                container.setItem(slot, stack.copyWithCount(moved));
            } else {
                ItemStack grown = existing.copy();
                grown.grow(moved);
                container.setItem(slot, grown);
            }
            container.setChanged();
        }
        return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    static ItemStack extractFromContainerSlot(Container container, int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = container.getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int moved = Math.min(amount, existing.getCount());
        if (simulate) {
            return existing.copyWithCount(moved);
        }
        ItemStack taken = container.removeItem(slot, moved);
        container.setChanged();
        return taken;
    }
}
