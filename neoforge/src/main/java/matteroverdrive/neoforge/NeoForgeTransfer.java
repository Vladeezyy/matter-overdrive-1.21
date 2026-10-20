package matteroverdrive.neoforge;

import matteroverdrive.platform.transfer.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' NeoForge handlers as the common item, fluid and energy handlers of Matter Overdrive. NeoForge 26.3 removed
 * the legacy IItemHandler / IFluidHandler / IEnergyStorage wrappers the earlier versions went through, so these adapt
 * the transfer API directly: a simulated move is a transaction that is not committed, a real one commits it (nested in
 * the thread's open transaction, if there is one). The mod's own storages go the other way in {@link NeoForgeResources}.
 */
public final class NeoForgeTransfer {
    private NeoForgeTransfer() {}

    @SuppressWarnings("deprecation")
    private static Transaction tx() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    // --- fluids ---

    static FluidResource resource(FluidStack stack) {
        return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack.getFluid());
    }

    static FluidStack stack(FluidResource resource, int amount) {
        return resource.isEmpty() || amount <= 0 ? FluidStack.EMPTY : new FluidStack(resource.getFluid(), amount);
    }

    public static @Nullable matteroverdrive.platform.transfer.IFluidHandler fluids(@Nullable ResourceHandler<FluidResource> handler) {
        return handler == null ? null : new Fluids(handler);
    }

    /** A container's fluids through a one-slot item handler: the container after a move is the slot's stack */
    public static matteroverdrive.platform.transfer.IFluidHandlerItem itemFluids(ResourceHandler<FluidResource> handler, ItemStacksResourceHandler slot) {
        return new ItemFluids(handler, slot);
    }

    static class Fluids implements matteroverdrive.platform.transfer.IFluidHandler {
        final ResourceHandler<FluidResource> neo;

        Fluids(ResourceHandler<FluidResource> neo) {
            this.neo = neo;
        }

        @Override
        public int getTanks() {
            return neo.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return stack(neo.getResource(tank), neo.getAmountAsInt(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return neo.getCapacityAsInt(tank, neo.getResource(tank));
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return !stack.isEmpty() && neo.isValid(tank, resource(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            try (Transaction tx = tx()) {
                int filled = neo.insert(resource(resource), resource.getAmount(), tx);
                if (action.execute()) tx.commit();
                return filled;
            }
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            FluidResource type = resource(resource);
            try (Transaction tx = tx()) {
                int drained = neo.extract(type, resource.getAmount(), tx);
                if (action.execute()) tx.commit();
                return stack(type, drained);
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            for (int tank = 0; tank < neo.size(); tank++) {
                FluidResource type = neo.getResource(tank);
                if (type.isEmpty() || neo.getAmountAsLong(tank) <= 0) continue;
                try (Transaction tx = tx()) {
                    int drained = neo.extract(type, maxDrain, tx);
                    if (drained <= 0) continue;
                    if (action.execute()) tx.commit();
                    return stack(type, drained);
                }
            }
            return FluidStack.EMPTY;
        }
    }

    static final class ItemFluids extends Fluids implements matteroverdrive.platform.transfer.IFluidHandlerItem {
        private final ItemStacksResourceHandler slot;

        ItemFluids(ResourceHandler<FluidResource> neo, ItemStacksResourceHandler slot) {
            super(neo);
            this.slot = slot;
        }

        @Override
        public ItemStack getContainer() {
            return slot.getResource(0).toStack(slot.getAmountAsInt(0));
        }
    }

    // --- items ---

    public static @Nullable matteroverdrive.platform.transfer.IItemHandler items(@Nullable ResourceHandler<ItemResource> handler) {
        return handler == null ? null : new Items(handler);
    }

    record Items(ResourceHandler<ItemResource> neo) implements matteroverdrive.platform.transfer.IItemHandler {
        @Override
        public int getSlots() {
            return neo.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemResource resource = neo.getResource(slot);
            return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(neo.getAmountAsInt(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = tx()) {
                int inserted = neo.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemResource resource = neo.getResource(slot);
            if (amount <= 0 || resource.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = tx()) {
                int extracted = neo.extract(slot, resource, amount, tx);
                if (!simulate) tx.commit();
                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return neo.getCapacityAsInt(slot, neo.getResource(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && neo.isValid(slot, ItemResource.of(stack));
        }
    }

    // --- energy ---

    public static @Nullable matteroverdrive.platform.transfer.IEnergyStorage energy(@Nullable EnergyHandler handler) {
        return handler == null ? null : new Energy(handler);
    }

    record Energy(EnergyHandler neo) implements matteroverdrive.platform.transfer.IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) return 0;
            try (Transaction tx = tx()) {
                int received = neo.insert(toReceive, tx);
                if (!simulate) tx.commit();
                return received;
            }
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) return 0;
            try (Transaction tx = tx()) {
                int extracted = neo.extract(toExtract, tx);
                if (!simulate) tx.commit();
                return extracted;
            }
        }

        @Override
        public int getEnergyStored() {
            return neo.getAmountAsInt();
        }

        @Override
        public int getMaxEnergyStored() {
            return neo.getCapacityAsInt();
        }

        @Override
        public boolean canExtract() {
            // the transfer API has no flags: a full or empty handler is asked with a 1 FE move it doesn't commit
            if (neo.getAmountAsLong() <= 0) return true;
            try (Transaction tx = tx()) {
                return neo.extract(1, tx) > 0;
            }
        }

        @Override
        public boolean canReceive() {
            if (neo.getAmountAsLong() >= neo.getCapacityAsLong()) return true;
            try (Transaction tx = tx()) {
                return neo.insert(1, tx) > 0;
            }
        }
    }
}
