package matteroverdrive.platform.transfer;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The slots of a {@link WorldlyContainer} that one side exposes, with its insert and extract rules */
public class SidedInvWrapper implements IItemHandlerModifiable {
    protected final WorldlyContainer container;
    @Nullable
    protected final Direction side;

    public SidedInvWrapper(WorldlyContainer container, @Nullable Direction side) {
        this.container = container;
        this.side = side;
    }

    private int[] slots() {
        return this.container.getSlotsForFace(this.side == null ? Direction.UP : this.side);
    }

    private int slot(int index) {
        int[] slots = this.slots();
        return index >= 0 && index < slots.length ? slots[index] : -1;
    }

    @Override
    public int getSlots() {
        return this.slots().length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        int slot = this.slot(index);
        return slot < 0 ? ItemStack.EMPTY : this.container.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        int slot = this.slot(index);
        if (slot < 0 || stack.isEmpty() || !this.container.canPlaceItemThroughFace(slot, stack, this.side)) {
            return stack;
        }
        return ItemHandlers.insertIntoContainerSlot(this.container, slot, stack, simulate, this.container.getMaxStackSize(), this.container.canPlaceItem(slot, stack));
    }

    @Override
    public ItemStack extractItem(int index, int amount, boolean simulate) {
        int slot = this.slot(index);
        if (slot < 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = this.container.getItem(slot);
        if (existing.isEmpty() || !this.container.canTakeItemThroughFace(slot, existing, this.side == null ? Direction.UP : this.side)) {
            return ItemStack.EMPTY;
        }
        return ItemHandlers.extractFromContainerSlot(this.container, slot, amount, simulate);
    }

    @Override
    public void setStackInSlot(int index, ItemStack stack) {
        int slot = this.slot(index);
        if (slot >= 0) {
            this.container.setItem(slot, stack);
        }
    }

    @Override
    public int getSlotLimit(int index) {
        return this.container.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        int slot = this.slot(index);
        return slot >= 0 && this.container.canPlaceItem(slot, stack);
    }
}
