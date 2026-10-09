package matteroverdrive.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A machine's slots (1.7.10 machines kept upgrades and the battery slot in the same inventory).
 * Slots are declared with a role; {@link #automation()} is the view exposed to hoppers and pipes.
 */
public class MachineInventory extends ItemStackHandler {
    /** FILTER: the network destination filter (1.7.10 DestinationFilterSlot), shown on the Config page. */
    public enum Role { INPUT, OUTPUT, ENERGY, UPGRADE, OTHER, FILTER }

    public record SlotSpec(Role role, Predicate<ItemStack> filter, int limit) {}

    private final List<SlotSpec> specs;
    private final Runnable onChanged;

    private MachineInventory(List<SlotSpec> specs, Runnable onChanged) {
        super(specs.size());
        this.specs = List.copyOf(specs);
        this.onChanged = onChanged;
    }

    public SlotSpec spec(int index) {
        return specs.get(index);
    }

    public int size() {
        return getSlots();
    }

    public ItemStack getStack(int index) {
        return stacks.get(index);
    }

    public void setStack(int index, ItemStack stack) {
        setStackInSlot(index, stack);
    }

    /** Removes {@code amount} items from a slot (machine logic on the server tick). */
    public void shrink(int index, int amount) {
        ItemStack stack = getStack(index).copy();
        stack.shrink(amount);
        setStack(index, stack);
    }

    public boolean isValid(int index, ItemStack stack) {
        return stack.isEmpty() || specs.get(index).filter().test(stack);
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        return isValid(index, stack);
    }

    @Override
    public int getSlotLimit(int index) {
        return specs.get(index).limit();
    }

    @Override
    protected void onContentsChanged(int index) {
        onChanged.run();
    }

    private record SlotStack(int slot, ItemStack stack) {
        static final com.mojang.serialization.Codec<SlotStack> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.INT.fieldOf("Slot").forGetter(SlotStack::slot),
                ItemStack.CODEC.fieldOf("Item").forGetter(SlotStack::stack)).apply(i, SlotStack::new));
    }

    public void serialize(ValueOutput output) {
        var items = output.list("Items", SlotStack.CODEC);
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) items.add(new SlotStack(i, stacks.get(i)));
        }
    }

    public void deserialize(ValueInput input) {
        java.util.Collections.fill(stacks, ItemStack.EMPTY);
        input.listOrEmpty("Items", SlotStack.CODEC).forEach(slot -> {
            if (slot.slot() >= 0 && slot.slot() < stacks.size()) stacks.set(slot.slot(), slot.stack());
        });
        onLoad();
    }

    /** Hoppers and pipes may insert into INPUT and ENERGY slots and extract from OUTPUT slots only. */
    public IItemHandler automation() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return MachineInventory.this.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int index) {
                return MachineInventory.this.getStackInSlot(index);
            }

            @Override
            public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
                Role role = specs.get(index).role();
                return role == Role.INPUT || role == Role.ENERGY ? MachineInventory.this.insertItem(index, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int index, int amount, boolean simulate) {
                return specs.get(index).role() == Role.OUTPUT ? MachineInventory.this.extractItem(index, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int index) {
                return MachineInventory.this.getSlotLimit(index);
            }

            @Override
            public boolean isItemValid(int index, ItemStack stack) {
                return MachineInventory.this.isItemValid(index, stack);
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<SlotSpec> specs = new ArrayList<>();

        /** Adds a slot and returns its index. */
        public int add(Role role, Predicate<ItemStack> filter, int limit) {
            specs.add(new SlotSpec(role, filter, limit));
            return specs.size() - 1;
        }

        public int add(Role role, Predicate<ItemStack> filter) {
            return add(role, filter, 64);
        }

        public MachineInventory build(Runnable onChanged) {
            return new MachineInventory(specs, onChanged);
        }
    }
}
