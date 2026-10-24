package matteroverdrive.platform.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A menu slot backed by an {@link IItemHandler} slot (NeoForge's {@code SlotItemHandler}) */
public class SlotItemHandler extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final IItemHandler handler;
    private final int index;

    public SlotItemHandler(IItemHandler handler, int index, int x, int y) {
        super(EMPTY, index, x, y);
        this.handler = handler;
        this.index = index;
    }

    public IItemHandler getItemHandler() {
        return this.handler;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && this.handler.isItemValid(this.index, stack);
    }

    @Override
    public ItemStack getItem() {
        return this.handler.getStackInSlot(this.index);
    }

    @Override
    public void set(ItemStack stack) {
        ((IItemHandlerModifiable) this.handler).setStackInSlot(this.index, stack);
        this.setChanged();
    }

    @Override
    public void setByPlayer(ItemStack newStack) {
        ((IItemHandlerModifiable) this.handler).setStackInSlot(this.index, newStack);
        this.setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public int getMaxStackSize() {
        return this.handler.getSlotLimit(this.index);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        ItemStack max = stack.copyWithCount(stack.getMaxStackSize());
        ItemStack current = this.handler.getStackInSlot(this.index);
        if (this.handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(this.index, ItemStack.EMPTY);
            ItemStack rest = modifiable.insertItem(this.index, max, true);
            modifiable.setStackInSlot(this.index, current);
            return max.getCount() - rest.getCount();
        }
        ItemStack rest = this.handler.insertItem(this.index, max, true);
        return current.getCount() + max.getCount() - rest.getCount();
    }

    @Override
    public boolean mayPickup(Player player) {
        return !this.handler.extractItem(this.index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return this.handler.extractItem(this.index, amount, false);
    }
}
