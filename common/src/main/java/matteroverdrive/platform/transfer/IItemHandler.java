package matteroverdrive.platform.transfer;

import net.minecraft.world.item.ItemStack;

/**
 * Slots of items that rats and blocks move items in and out of: the methods of NeoForge 1.21.1's {@code IItemHandler}
 * (the original code is written against them), with no loader classes. Each loader adapts its own item storages to it
 * ({@code dev.vladeezyy.rats.neoforge}, {@code dev.vladeezyy.rats.fabric}); NeoForge 26.x replaced IItemHandler, so
 * this also keeps the game code the same across versions.
 */
public interface IItemHandler {
    int getSlots();

    /** The stack in the slot; do not modify it */
    ItemStack getStackInSlot(int slot);

    /** Inserts into the slot; returns what did not fit (the stack itself if nothing fit) */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** Extracts up to {@code amount} from the slot; returns what was (or would be) extracted */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);
}
