package matteroverdrive.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A machine's slots (1.7.10 machines kept upgrades and the battery slot in the same inventory).
 * Slots are declared with a role; {@link #automation()} is the view exposed to hoppers and pipes.
 */
public class MachineInventory extends ItemStacksResourceHandler {
    public enum Role { INPUT, OUTPUT, ENERGY, UPGRADE, OTHER }

    public record SlotSpec(Role role, Predicate<ItemResource> filter, int limit) {}

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

    public ItemStack getStack(int index) {
        return stacks.get(index);
    }

    public void setStack(int index, ItemStack stack) {
        set(index, ItemResource.of(stack), stack.getCount());
    }

    /** Removes {@code amount} items from a slot outside of any transaction (machine logic on the server tick). */
    public void shrink(int index, int amount) {
        ItemStack stack = getStack(index).copy();
        stack.shrink(amount);
        setStack(index, stack);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || specs.get(index).filter().test(resource);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int limit = specs.get(index).limit();
        return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChanged.run();
    }

    /** Hoppers and pipes may insert into INPUT and ENERGY slots and extract from OUTPUT slots only. */
    public ResourceHandler<ItemResource> automation() {
        return new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                Role role = specs.get(index).role();
                return role == Role.INPUT || role == Role.ENERGY ? super.insert(index, resource, amount, transaction) : 0;
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return specs.get(index).role() == Role.OUTPUT ? super.extract(index, resource, amount, transaction) : 0;
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<SlotSpec> specs = new ArrayList<>();

        /** Adds a slot and returns its index. */
        public int add(Role role, Predicate<ItemResource> filter, int limit) {
            specs.add(new SlotSpec(role, filter, limit));
            return specs.size() - 1;
        }

        public int add(Role role, Predicate<ItemResource> filter) {
            return add(role, filter, 64);
        }

        public MachineInventory build(Runnable onChanged) {
            return new MachineInventory(specs, onChanged);
        }
    }
}
