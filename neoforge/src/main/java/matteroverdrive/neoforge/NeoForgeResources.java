package matteroverdrive.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import matteroverdrive.platform.transfer.FluidStack;
import matteroverdrive.platform.transfer.IEnergyStorage;
import matteroverdrive.platform.transfer.IFluidHandler;
import matteroverdrive.platform.transfer.IFluidHandlerItem;
import matteroverdrive.platform.transfer.IItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The common storages as NeoForge 21.9+'s transactional handlers. Inside a transaction an adapter only simulates and
 * keeps what was moved as pending (a snapshot journal reverts it with the transaction); the common handler does the
 * real move when the root transaction commits. One adapter per block entity and kind, so two lookups within one
 * transaction share the pending amounts. Items with energy or matter are exchanged for their changed copy.
 */
final class NeoForgeResources {
    private NeoForgeResources() {}

    private static final Map<BlockEntity, Object[]> EXPOSED = new WeakHashMap<>();
    private static final Map<net.minecraft.world.item.Item, Function<ItemStack, IFluidHandlerItem>> ITEM_FLUIDS = new java.util.HashMap<>();

    /** The mod's item fluid handlers (registered with the capabilities), used directly by the mod's own code */
    static void registerItemFluid(Function<ItemStack, IFluidHandlerItem> handler, net.minecraft.world.item.Item... items) {
        for (var item : items) ITEM_FLUIDS.put(item, handler);
    }

    static IFluidHandlerItem ownItemFluid(ItemStack stack) {
        var handler = ITEM_FLUIDS.get(stack.getItem());
        return handler == null ? null : handler.apply(stack);
    }

    /** The adapter of kind 0 (energy), 1 (items) or 2 (fluids) of a block entity, made once */
    @SuppressWarnings("unchecked")
    static synchronized <A> A exposed(BlockEntity be, int kind, Supplier<A> factory) {
        Object[] adapters = EXPOSED.computeIfAbsent(be, key -> new Object[3]);
        if (adapters[kind] == null) adapters[kind] = factory.get();
        return (A) adapters[kind];
    }

    static int clamp(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    /** A common energy handler */
    static final class Energy extends SnapshotJournal<long[]> implements EnergyHandler {
        private final IEnergyStorage handler;
        private long in;
        private long out;

        Energy(IEnergyStorage handler) {
            this.handler = handler;
        }

        @Override
        public long getAmountAsLong() {
            return Math.max(0, this.handler.getEnergyStored() + this.in - this.out);
        }

        @Override
        public long getCapacityAsLong() {
            return this.handler.getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            if (this.out > 0 || !this.handler.canReceive()) return 0;
            int accepted = clamp(Math.min(amount, this.handler.receiveEnergy(clamp(this.in + amount), true) - this.in));
            if (accepted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.in += accepted;
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            if (this.in > 0 || !this.handler.canExtract()) return 0;
            int extracted = clamp(Math.min(amount, this.handler.extractEnergy(clamp(this.out + amount), true) - this.out));
            if (extracted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.out += extracted;
            return extracted;
        }

        @Override
        protected long[] createSnapshot() {
            return new long[] {this.in, this.out};
        }

        @Override
        protected void revertToSnapshot(long[] snapshot) {
            this.in = snapshot[0];
            this.out = snapshot[1];
        }

        @Override
        protected void onRootCommit(long[] original) {
            long received = this.in;
            long extracted = this.out;
            this.in = 0;
            this.out = 0;
            if (received > 0) this.handler.receiveEnergy(clamp(received), false);
            if (extracted > 0) this.handler.extractEnergy(clamp(extracted), false);
        }
    }

    private record SlotState(ItemResource resource, int in, int out) {}

    /** Common item slots */
    static final class Items implements ResourceHandler<ItemResource> {
        private final IItemHandler handler;
        private final List<Slot> slots = new ArrayList<>();

        Items(IItemHandler handler) {
            this.handler = handler;
            for (int i = 0; i < handler.getSlots(); i++) this.slots.add(new Slot(handler, i));
        }

        @Override
        public int size() {
            return this.slots.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return this.slots.get(index).resource();
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.slots.get(index).amount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            int limit = this.handler.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return this.handler.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.slots.get(index).insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.slots.get(index).extract(resource, amount, transaction);
        }
    }

    private static final class Slot extends SnapshotJournal<SlotState> {
        private final IItemHandler handler;
        private final int slot;
        private ItemResource pending = ItemResource.EMPTY;
        private int in;
        private int out;

        Slot(IItemHandler handler, int slot) {
            this.handler = handler;
            this.slot = slot;
        }

        ItemResource resource() {
            ItemStack current = this.handler.getStackInSlot(this.slot);
            if (current.isEmpty()) return this.in > 0 ? this.pending : ItemResource.EMPTY;
            return current.getCount() - this.out <= 0 ? ItemResource.EMPTY : ItemResource.of(current);
        }

        int amount() {
            return Math.max(0, this.handler.getStackInSlot(this.slot).getCount() + this.in - this.out);
        }

        int insert(ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (this.out > 0 || this.in > 0 && !resource.equals(this.pending)) return 0;
            int wanted = clamp((long) this.in + amount);
            ItemStack rest = this.handler.insertItem(this.slot, resource.toStack(wanted), true);
            int accepted = Math.min(amount, wanted - rest.getCount() - this.in);
            if (accepted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.pending = resource;
            this.in += accepted;
            return accepted;
        }

        int extract(ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (this.in > 0) return 0;
            ItemStack current = this.handler.getStackInSlot(this.slot);
            if (current.isEmpty() || !resource.matches(current)) return 0;
            ItemStack taken = this.handler.extractItem(this.slot, clamp((long) this.out + amount), true);
            int extracted = Math.min(amount, taken.getCount() - this.out);
            if (extracted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.out += extracted;
            return extracted;
        }

        @Override
        protected SlotState createSnapshot() {
            return new SlotState(this.pending, this.in, this.out);
        }

        @Override
        protected void revertToSnapshot(SlotState snapshot) {
            this.pending = snapshot.resource();
            this.in = snapshot.in();
            this.out = snapshot.out();
        }

        @Override
        protected void onRootCommit(SlotState original) {
            int inserted = this.in;
            int extracted = this.out;
            ItemResource resource = this.pending;
            this.in = 0;
            this.out = 0;
            this.pending = ItemResource.EMPTY;
            if (inserted > 0) this.handler.insertItem(this.slot, resource.toStack(inserted), false);
            if (extracted > 0) this.handler.extractItem(this.slot, extracted, false);
        }
    }

    private record TankState(FluidResource resource, int in, int out) {}

    /** A common fluid handler's first tank (the mod's tanks have one), in millibuckets */
    static final class Fluids extends SnapshotJournal<TankState> implements ResourceHandler<FluidResource> {
        private final IFluidHandler handler;
        private FluidResource pending = FluidResource.EMPTY;
        private int in;
        private int out;

        Fluids(IFluidHandler handler) {
            this.handler = handler;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            FluidStack current = this.handler.getFluidInTank(0);
            if (current.isEmpty()) return this.in > 0 ? this.pending : FluidResource.EMPTY;
            return current.getAmount() - this.out <= 0 ? FluidResource.EMPTY : FluidResource.of(current.getFluid());
        }

        @Override
        public long getAmountAsLong(int index) {
            return Math.max(0, this.handler.getFluidInTank(0).getAmount() + this.in - this.out);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return this.handler.getTankCapacity(0);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isComponentsPatchEmpty() && this.handler.isFluidValid(0, new FluidStack(resource.getFluid(), 1));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!resource.isComponentsPatchEmpty() || this.out > 0 || this.in > 0 && !resource.equals(this.pending)) return 0;
            int filled = this.handler.fill(new FluidStack(resource.getFluid(), clamp((long) this.in + amount)), IFluidHandler.FluidAction.SIMULATE);
            int accepted = Math.min(amount, filled - this.in);
            if (accepted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.pending = resource;
            this.in += accepted;
            return accepted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (this.in > 0) return 0;
            FluidStack current = this.handler.getFluidInTank(0);
            if (current.isEmpty() || current.getFluid() != resource.getFluid() || !resource.isComponentsPatchEmpty()) return 0;
            FluidStack drained = this.handler.drain(new FluidStack(resource.getFluid(), clamp((long) this.out + amount)), IFluidHandler.FluidAction.SIMULATE);
            int extracted = Math.min(amount, drained.getAmount() - this.out);
            if (extracted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.out += extracted;
            return extracted;
        }

        @Override
        protected TankState createSnapshot() {
            return new TankState(this.pending, this.in, this.out);
        }

        @Override
        protected void revertToSnapshot(TankState snapshot) {
            this.pending = snapshot.resource();
            this.in = snapshot.in();
            this.out = snapshot.out();
        }

        @Override
        protected void onRootCommit(TankState original) {
            int filled = this.in;
            int drained = this.out;
            FluidResource resource = this.pending;
            this.in = 0;
            this.out = 0;
            this.pending = FluidResource.EMPTY;
            if (filled > 0) this.handler.fill(new FluidStack(resource.getFluid(), filled), IFluidHandler.FluidAction.EXECUTE);
            if (drained > 0) this.handler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** The energy of one of the mod's items: the item is exchanged for its charged copy */
    record ItemEnergy(ItemAccess access, Function<ItemStack, IEnergyStorage> factory) implements EnergyHandler {
        private IEnergyStorage handler(ItemStack copy) {
            return this.factory.apply(copy);
        }

        private ItemStack copy() {
            return this.access.getResource().toStack(1);
        }

        @Override
        public long getAmountAsLong() {
            return this.handler(this.copy()).getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return this.handler(this.copy()).getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            ItemStack copy = this.copy();
            int received = this.handler(copy).receiveEnergy(amount, false);
            return received > 0 && this.access.exchange(ItemResource.of(copy), 1, transaction) == 1 ? received : 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            ItemStack copy = this.copy();
            int extracted = this.handler(copy).extractEnergy(amount, false);
            return extracted > 0 && this.access.exchange(ItemResource.of(copy), 1, transaction) == 1 ? extracted : 0;
        }
    }

    /** The fluid of one of the mod's items (matter containers) */
    record ItemFluids(ItemAccess access, Function<ItemStack, IFluidHandlerItem> factory) implements ResourceHandler<FluidResource> {
        private IFluidHandlerItem handler() {
            return this.factory.apply(this.access.getResource().toStack(1));
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            FluidStack fluid = this.handler().getFluidInTank(0);
            return fluid.isEmpty() ? FluidResource.EMPTY : FluidResource.of(fluid.getFluid());
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.handler().getFluidInTank(0).getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return this.handler().getTankCapacity(0);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isComponentsPatchEmpty() && this.handler().isFluidValid(0, new FluidStack(resource.getFluid(), 1));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!resource.isComponentsPatchEmpty()) return 0;
            IFluidHandlerItem handler = this.handler();
            int filled = handler.fill(new FluidStack(resource.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
            return filled > 0 && this.access.exchange(ItemResource.of(handler.getContainer()), 1, transaction) == 1 ? filled : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!resource.isComponentsPatchEmpty()) return 0;
            IFluidHandlerItem handler = this.handler();
            FluidStack drained = handler.drain(new FluidStack(resource.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
            return !drained.isEmpty() && this.access.exchange(ItemResource.of(handler.getContainer()), 1, transaction) == 1 ? drained.getAmount() : 0;
        }
    }
}
