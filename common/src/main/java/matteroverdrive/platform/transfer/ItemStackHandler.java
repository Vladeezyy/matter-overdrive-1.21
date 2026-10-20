package matteroverdrive.platform.transfer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Item slots stored in a list. Saves as {@code {Items: [{Slot, ...stack}], Size}}, the layout NeoForge's
 * {@code ItemStackHandler} used, so block entities saved by the NeoForge-only build load the same.
 */
public class ItemStackHandler implements IItemHandlerModifiable, matteroverdrive.platform.ValueIOSerializable {
    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler() {
        this(1);
    }

    public ItemStackHandler(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public void setSize(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        this.validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        this.onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return this.stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        this.validateSlotIndex(slot);
        return this.stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!this.isItemValid(slot, stack)) {
            return stack;
        }
        this.validateSlotIndex(slot);
        ItemStack existing = this.stacks.get(slot);
        int limit = this.getStackLimit(slot, stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            this.onContentsChanged(slot);
        }
        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }
        this.validateSlotIndex(slot);
        ItemStack existing = this.stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.stacks.set(slot, ItemStack.EMPTY);
                this.onContentsChanged(slot);
                return existing;
            }
            return existing.copy();
        }
        if (!simulate) {
            this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            this.onContentsChanged(slot);
        }
        return existing.copyWithCount(toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 99;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
        var items = output.list("Items", net.minecraft.world.ItemStackWithSlot.CODEC);
        for (int i = 0; i < this.stacks.size(); i++) {
            if (!this.stacks.get(i).isEmpty()) items.add(new net.minecraft.world.ItemStackWithSlot(i, this.stacks.get(i)));
        }
        output.putInt("Size", this.stacks.size());
    }

    @Override
    public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
        this.setSize(input.getIntOr("Size", this.stacks.size()));
        input.listOrEmpty("Items", net.minecraft.world.ItemStackWithSlot.CODEC).forEach(slot -> {
            if (slot.isValidInContainer(this.stacks.size())) this.stacks.set(slot.slot(), slot.stack());
        });
        this.onLoad();
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= this.stacks.size()) {
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
        }
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }
}
