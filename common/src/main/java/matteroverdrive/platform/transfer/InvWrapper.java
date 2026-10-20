package matteroverdrive.platform.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A vanilla {@link Container} as item slots */
public class InvWrapper implements IItemHandlerModifiable {
    private final Container container;

    public InvWrapper(Container container) {
        this.container = container;
    }

    public Container getInv() {
        return this.container;
    }

    @Override
    public int getSlots() {
        return this.container.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.container.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return ItemHandlers.insertIntoContainerSlot(this.container, slot, stack, simulate, this.getSlotLimit(slot), this.isItemValid(slot, stack));
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemHandlers.extractFromContainerSlot(this.container, slot, amount, simulate);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        this.container.setItem(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.container.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.container.canPlaceItem(slot, stack);
    }
}
