package matteroverdrive.platform.transfer;

import net.minecraft.world.item.ItemStack;

/** Several item handlers as one, their slots one after another */
public class CombinedInvWrapper implements IItemHandlerModifiable {
    protected final IItemHandlerModifiable[] handlers;
    protected final int[] baseIndex;
    protected final int slotCount;

    public CombinedInvWrapper(IItemHandlerModifiable... handlers) {
        this.handlers = handlers;
        this.baseIndex = new int[handlers.length];
        int index = 0;
        for (int i = 0; i < handlers.length; i++) {
            index += handlers[i].getSlots();
            this.baseIndex[i] = index;
        }
        this.slotCount = index;
    }

    protected int handlerIndex(int slot) {
        if (slot < 0) {
            return -1;
        }
        for (int i = 0; i < this.baseIndex.length; i++) {
            if (slot < this.baseIndex[i]) {
                return i;
            }
        }
        return -1;
    }

    protected int localSlot(int slot, int handler) {
        return handler <= 0 ? slot : slot - this.baseIndex[handler - 1];
    }

    @Override
    public int getSlots() {
        return this.slotCount;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int h = this.handlerIndex(slot);
        return h < 0 ? ItemStack.EMPTY : this.handlers[h].getStackInSlot(this.localSlot(slot, h));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int h = this.handlerIndex(slot);
        return h < 0 ? stack : this.handlers[h].insertItem(this.localSlot(slot, h), stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int h = this.handlerIndex(slot);
        return h < 0 ? ItemStack.EMPTY : this.handlers[h].extractItem(this.localSlot(slot, h), amount, simulate);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int h = this.handlerIndex(slot);
        if (h >= 0) {
            this.handlers[h].setStackInSlot(this.localSlot(slot, h), stack);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        int h = this.handlerIndex(slot);
        return h < 0 ? 0 : this.handlers[h].getSlotLimit(this.localSlot(slot, h));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int h = this.handlerIndex(slot);
        return h >= 0 && this.handlers[h].isItemValid(this.localSlot(slot, h), stack);
    }
}
