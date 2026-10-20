package matteroverdrive.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import matteroverdrive.platform.transfer.FluidStack;
import matteroverdrive.platform.transfer.IEnergyStorage;
import matteroverdrive.platform.transfer.IFluidHandler;
import matteroverdrive.platform.transfer.IFluidHandlerItem;
import matteroverdrive.platform.transfer.IItemHandler;
import matteroverdrive.platform.transfer.IItemHandlerModifiable;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

/**
 * The common storages (NeoForge-style handlers, fluids in millibuckets) and Fabric's transactional ones (fluids in
 * droplets, 81 per millibucket; energy through Team Reborn Energy, 1 E = 1 FE as Fabric tech mods count it).
 *
 * <p>Other mods' storages are used through a transaction per call. The mod's own handlers are offered to other mods
 * through adapters that only simulate inside a transaction and keep what was moved as pending; the handler does the
 * real move when the outermost transaction commits (aborted transactions just drop the pending amounts).
 */
final class FabricTransfer {
    private FabricTransfer() {}

    static final long DROPLETS_PER_MB = FluidConstants.BUCKET / FluidStack.BUCKET_VOLUME;

    static int clamp(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    static FluidStack fromFabric(FluidVariant variant, long droplets) {
        return variant.isBlank() || droplets < DROPLETS_PER_MB ? FluidStack.EMPTY : new FluidStack(variant.getFluid(), clamp(droplets / DROPLETS_PER_MB));
    }

    /** A one-slot storage holding a copy of the stack: what Fabric's item lookups need as the item's container */
    static ContainerItemContext stackContext(ItemStack stack) {
        SingleVariantStorage<ItemVariant> slot = new SingleVariantStorage<>() {
            @Override
            protected ItemVariant getBlankVariant() {
                return ItemVariant.blank();
            }

            @Override
            protected long getCapacity(ItemVariant variant) {
                return variant.getItem().getDefaultMaxStackSize();
            }
        };
        slot.variant = ItemVariant.of(stack);
        slot.amount = stack.getCount();
        return ContainerItemContext.ofSingleSlot(slot);
    }

    // --- other mods' storages as common handlers ---------------------------------------------------------------

    /** A Team Reborn Energy storage */
    static class Energy implements IEnergyStorage {
        final EnergyStorage storage;

        Energy(EnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0 || !this.storage.supportsInsertion()) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = this.storage.insert(toReceive, tx);
                if (!simulate) tx.commit();
                this.changed(simulate, inserted);
                return clamp(inserted);
            }
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0 || !this.storage.supportsExtraction()) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = this.storage.extract(toExtract, tx);
                if (!simulate) tx.commit();
                this.changed(simulate, extracted);
                return clamp(extracted);
            }
        }

        void changed(boolean simulate, long moved) {
        }

        @Override
        public int getEnergyStored() {
            return clamp(this.storage.getAmount());
        }

        @Override
        public int getMaxEnergyStored() {
            return clamp(this.storage.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return this.storage.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return this.storage.supportsInsertion();
        }
    }

    /** The energy of another mod's item: the stack takes the item's new data after each executed move, as on NeoForge */
    static final class ItemEnergy extends Energy {
        private final ContainerItemContext context;
        private final ItemStack stack;

        ItemEnergy(EnergyStorage storage, ContainerItemContext context, ItemStack stack) {
            super(storage);
            this.context = context;
            this.stack = stack;
        }

        @Override
        void changed(boolean simulate, long moved) {
            ItemVariant variant = this.context.getItemVariant();
            if (!simulate && moved > 0 && variant.getItem() == this.stack.getItem()) {
                this.stack.applyComponents(variant.getComponents());
            }
        }
    }

    /** A slotted item storage (other mods' inventories, vanilla containers) as item slots */
    static final class SlottedItems implements IItemHandlerModifiable {
        private final SlottedStorage<ItemVariant> storage;

        SlottedItems(SlottedStorage<ItemVariant> storage) {
            this.storage = storage;
        }

        @Override
        public int getSlots() {
            return this.storage.getSlotCount();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            SingleSlotStorage<ItemVariant> view = this.storage.getSlot(slot);
            return view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack(clamp(view.getAmount()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = this.storage.getSlot(slot).insert(ItemVariant.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) inserted);
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            SingleSlotStorage<ItemVariant> view = this.storage.getSlot(slot);
            if (amount <= 0 || view.isResourceBlank()) return ItemStack.EMPTY;
            ItemVariant variant = view.getResource();
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = view.extract(variant, amount, tx);
                if (!simulate) tx.commit();
                return extracted <= 0 ? ItemStack.EMPTY : variant.toStack((int) extracted);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return clamp(this.storage.getSlot(slot).getCapacity());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            try (Transaction tx = Transaction.openOuter()) {
                return this.storage.getSlot(slot).insert(ItemVariant.of(stack), 1, tx) > 0;
            }
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            try (Transaction tx = Transaction.openOuter()) {
                SingleSlotStorage<ItemVariant> view = this.storage.getSlot(slot);
                if (!view.isResourceBlank()) view.extract(view.getResource(), view.getAmount(), tx);
                if (!stack.isEmpty()) view.insert(ItemVariant.of(stack), stack.getCount(), tx);
                tx.commit();
            }
        }
    }

    /** A fluid storage as tanks (one tank per view) */
    static class Fluids implements IFluidHandler {
        final Storage<FluidVariant> storage;

        Fluids(Storage<FluidVariant> storage) {
            this.storage = storage;
        }

        private List<StorageView<FluidVariant>> views() {
            List<StorageView<FluidVariant>> views = new ArrayList<>();
            for (StorageView<FluidVariant> view : this.storage) views.add(view);
            return views;
        }

        @Override
        public int getTanks() {
            return this.views().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            List<StorageView<FluidVariant>> views = this.views();
            if (tank < 0 || tank >= views.size()) return FluidStack.EMPTY;
            return fromFabric(views.get(tank).getResource(), views.get(tank).getAmount());
        }

        @Override
        public int getTankCapacity(int tank) {
            List<StorageView<FluidVariant>> views = this.views();
            return tank < 0 || tank >= views.size() ? 0 : clamp(views.get(tank).getCapacity() / DROPLETS_PER_MB);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return this.storage.supportsInsertion();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = this.storage.insert(FluidVariant.of(resource.getFluid()), resource.getAmount() * DROPLETS_PER_MB, tx);
                if (action.execute()) tx.commit();
                return clamp(inserted / DROPLETS_PER_MB);
            }
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            try (Transaction tx = Transaction.openOuter()) {
                FluidVariant variant = FluidVariant.of(resource.getFluid());
                long extracted = this.storage.extract(variant, resource.getAmount() * DROPLETS_PER_MB, tx);
                if (action.execute()) tx.commit();
                return fromFabric(variant, extracted);
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            for (StorageView<FluidVariant> view : this.storage) {
                if (!view.isResourceBlank() && view.getAmount() > 0) {
                    return this.drain(new FluidStack(view.getResource().getFluid(), maxDrain), action);
                }
            }
            return FluidStack.EMPTY;
        }
    }

    /** The fluid of another mod's item; the item in the context changes when filled or drained (a bucket) */
    static final class ItemFluids extends Fluids implements IFluidHandlerItem {
        private final ContainerItemContext context;

        ItemFluids(Storage<FluidVariant> storage, ContainerItemContext context) {
            super(storage);
            this.context = context;
        }

        @Override
        public ItemStack getContainer() {
            ItemVariant variant = this.context.getItemVariant();
            return variant.isBlank() ? ItemStack.EMPTY : variant.toStack(clamp(this.context.getAmount()));
        }
    }

    // --- the mod's handlers for other mods ------------------------------------------------------------------------

    /** A common energy handler as a Team Reborn Energy storage */
    static final class ExposedEnergy extends SnapshotParticipant<long[]> implements EnergyStorage {
        private final IEnergyStorage handler;
        private long in;
        private long out;

        ExposedEnergy(IEnergyStorage handler) {
            this.handler = handler;
        }

        @Override
        public boolean supportsInsertion() {
            return this.handler.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return this.handler.canExtract();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            if (this.out > 0 || !this.handler.canReceive()) return 0;
            long accepted = Math.min(maxAmount, this.handler.receiveEnergy(clamp(this.in + maxAmount), true) - this.in);
            if (accepted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.in += accepted;
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            if (this.in > 0 || !this.handler.canExtract()) return 0;
            long extracted = Math.min(maxAmount, this.handler.extractEnergy(clamp(this.out + maxAmount), true) - this.out);
            if (extracted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.out += extracted;
            return extracted;
        }

        @Override
        public long getAmount() {
            return Math.max(0, this.handler.getEnergyStored() + this.in - this.out);
        }

        @Override
        public long getCapacity() {
            return this.handler.getMaxEnergyStored();
        }

        @Override
        protected long[] createSnapshot() {
            return new long[] {this.in, this.out};
        }

        @Override
        protected void readSnapshot(long[] snapshot) {
            this.in = snapshot[0];
            this.out = snapshot[1];
        }

        @Override
        protected void onFinalCommit() {
            long received = this.in;
            long extracted = this.out;
            this.in = 0;
            this.out = 0;
            if (received > 0) this.handler.receiveEnergy(clamp(received), false);
            if (extracted > 0) this.handler.extractEnergy(clamp(extracted), false);
        }
    }

    /** Common item slots as Fabric slots */
    static Storage<ItemVariant> exposeItems(IItemHandler handler) {
        List<ExposedSlot> slots = new ArrayList<>(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) slots.add(new ExposedSlot(handler, i));
        return new CombinedSlottedStorage<>(slots);
    }

    private record SlotState(ItemVariant variant, long in, long out) {}

    static final class ExposedSlot extends SnapshotParticipant<SlotState> implements SingleSlotStorage<ItemVariant> {
        private final IItemHandler handler;
        private final int slot;
        private ItemVariant variant = ItemVariant.blank();
        private long in;
        private long out;

        ExposedSlot(IItemHandler handler, int slot) {
            this.handler = handler;
            this.slot = slot;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (this.out > 0 || this.in > 0 && !resource.equals(this.variant)) return 0;
            int wanted = clamp(this.in + maxAmount);
            ItemStack rest = this.handler.insertItem(this.slot, resource.toStack(wanted), true);
            long accepted = Math.min(maxAmount, wanted - rest.getCount() - this.in);
            if (accepted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.variant = resource;
            this.in += accepted;
            return accepted;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (this.in > 0) return 0;
            ItemStack current = this.handler.getStackInSlot(this.slot);
            if (current.isEmpty() || !resource.matches(current)) return 0;
            ItemStack taken = this.handler.extractItem(this.slot, clamp(this.out + maxAmount), true);
            long extracted = Math.min(maxAmount, taken.getCount() - this.out);
            if (extracted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.out += extracted;
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return this.getResource().isBlank();
        }

        @Override
        public ItemVariant getResource() {
            ItemStack current = this.handler.getStackInSlot(this.slot);
            if (current.isEmpty()) return this.in > 0 ? this.variant : ItemVariant.blank();
            return current.getCount() - this.out <= 0 ? ItemVariant.blank() : ItemVariant.of(current);
        }

        @Override
        public long getAmount() {
            ItemStack current = this.handler.getStackInSlot(this.slot);
            return Math.max(0, current.getCount() + this.in - this.out);
        }

        @Override
        public long getCapacity() {
            ItemVariant resource = this.getResource();
            int limit = this.handler.getSlotLimit(this.slot);
            return resource.isBlank() ? limit : Math.min(limit, resource.getItem().getDefaultMaxStackSize());
        }

        @Override
        protected SlotState createSnapshot() {
            return new SlotState(this.variant, this.in, this.out);
        }

        @Override
        protected void readSnapshot(SlotState snapshot) {
            this.variant = snapshot.variant();
            this.in = snapshot.in();
            this.out = snapshot.out();
        }

        @Override
        protected void onFinalCommit() {
            long inserted = this.in;
            long extracted = this.out;
            ItemVariant resource = this.variant;
            this.in = 0;
            this.out = 0;
            this.variant = ItemVariant.blank();
            if (inserted > 0) this.handler.insertItem(this.slot, resource.toStack(clamp(inserted)), false);
            if (extracted > 0) this.handler.extractItem(this.slot, clamp(extracted), false);
        }
    }

    private record TankState(FluidVariant variant, long in, long out) {}

    /** A common fluid handler's first tank (the mod's tanks have one) as a Fabric storage */
    static final class ExposedFluid extends SnapshotParticipant<TankState> implements SingleSlotStorage<FluidVariant> {
        private final IFluidHandler handler;
        private FluidVariant variant = FluidVariant.blank();
        private long in;
        private long out;

        ExposedFluid(IFluidHandler handler) {
            this.handler = handler;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!resource.getComponents().isEmpty() || this.out > 0 || this.in > 0 && !resource.equals(this.variant)) return 0;
            long maxMb = maxAmount / DROPLETS_PER_MB;
            if (maxMb <= 0) return 0;
            int filled = this.handler.fill(new FluidStack(resource.getFluid(), clamp(this.in + maxMb)), IFluidHandler.FluidAction.SIMULATE);
            long accepted = Math.min(maxMb, filled - this.in);
            if (accepted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.variant = resource;
            this.in += accepted;
            return accepted * DROPLETS_PER_MB;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (this.in > 0) return 0;
            FluidStack current = this.handler.getFluidInTank(0);
            if (current.isEmpty() || current.getFluid() != resource.getFluid() || !resource.getComponents().isEmpty()) return 0;
            long maxMb = maxAmount / DROPLETS_PER_MB;
            if (maxMb <= 0) return 0;
            FluidStack drained = this.handler.drain(new FluidStack(resource.getFluid(), clamp(this.out + maxMb)), IFluidHandler.FluidAction.SIMULATE);
            long extracted = Math.min(maxMb, drained.getAmount() - this.out);
            if (extracted <= 0) return 0;
            this.updateSnapshots(transaction);
            this.out += extracted;
            return extracted * DROPLETS_PER_MB;
        }

        @Override
        public boolean isResourceBlank() {
            return this.getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            FluidStack current = this.handler.getFluidInTank(0);
            if (current.isEmpty()) return this.in > 0 ? this.variant : FluidVariant.blank();
            return current.getAmount() - this.out <= 0 ? FluidVariant.blank() : FluidVariant.of(current.getFluid());
        }

        @Override
        public long getAmount() {
            return Math.max(0, this.handler.getFluidInTank(0).getAmount() + this.in - this.out) * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return (long) this.handler.getTankCapacity(0) * DROPLETS_PER_MB;
        }

        @Override
        protected TankState createSnapshot() {
            return new TankState(this.variant, this.in, this.out);
        }

        @Override
        protected void readSnapshot(TankState snapshot) {
            this.variant = snapshot.variant();
            this.in = snapshot.in();
            this.out = snapshot.out();
        }

        @Override
        protected void onFinalCommit() {
            long filled = this.in;
            long drained = this.out;
            FluidVariant resource = this.variant;
            this.in = 0;
            this.out = 0;
            this.variant = FluidVariant.blank();
            if (filled > 0) this.handler.fill(new FluidStack(resource.getFluid(), clamp(filled)), IFluidHandler.FluidAction.EXECUTE);
            if (drained > 0) this.handler.drain(clamp(drained), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** The energy of one of the mod's items in another mod's container: the item is exchanged for its charged copy */
    static final class ExposedItemEnergy implements EnergyStorage {
        private final ContainerItemContext context;
        private final Function<ItemStack, IEnergyStorage> factory;

        ExposedItemEnergy(ContainerItemContext context, Function<ItemStack, IEnergyStorage> factory) {
            this.context = context;
            this.factory = factory;
        }

        private ItemStack copy() {
            return this.context.getItemVariant().toStack(1);
        }

        @Override
        public boolean supportsInsertion() {
            return this.factory.apply(this.copy()).canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return this.factory.apply(this.copy()).canExtract();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            ItemStack copy = this.copy();
            int received = this.factory.apply(copy).receiveEnergy(clamp(maxAmount), false);
            return received > 0 && this.context.exchange(ItemVariant.of(copy), 1, transaction) == 1 ? received : 0;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            ItemStack copy = this.copy();
            int extracted = this.factory.apply(copy).extractEnergy(clamp(maxAmount), false);
            return extracted > 0 && this.context.exchange(ItemVariant.of(copy), 1, transaction) == 1 ? extracted : 0;
        }

        @Override
        public long getAmount() {
            return this.factory.apply(this.copy()).getEnergyStored();
        }

        @Override
        public long getCapacity() {
            return this.factory.apply(this.copy()).getMaxEnergyStored();
        }
    }

    /** The fluid of one of the mod's items (matter containers) in another mod's container */
    static final class ExposedItemFluid implements SingleSlotStorage<FluidVariant> {
        private final ContainerItemContext context;
        private final Function<ItemStack, IFluidHandlerItem> factory;

        ExposedItemFluid(ContainerItemContext context, Function<ItemStack, IFluidHandlerItem> factory) {
            this.context = context;
            this.factory = factory;
        }

        private IFluidHandlerItem handler() {
            return this.factory.apply(this.context.getItemVariant().toStack(1));
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            long maxMb = maxAmount / DROPLETS_PER_MB;
            if (maxMb <= 0 || !resource.getComponents().isEmpty()) return 0;
            IFluidHandlerItem handler = this.handler();
            int filled = handler.fill(new FluidStack(resource.getFluid(), clamp(maxMb)), IFluidHandler.FluidAction.EXECUTE);
            return filled > 0 && this.context.exchange(ItemVariant.of(handler.getContainer()), 1, transaction) == 1 ? filled * DROPLETS_PER_MB : 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            long maxMb = maxAmount / DROPLETS_PER_MB;
            if (maxMb <= 0 || !resource.getComponents().isEmpty()) return 0;
            IFluidHandlerItem handler = this.handler();
            FluidStack drained = handler.drain(new FluidStack(resource.getFluid(), clamp(maxMb)), IFluidHandler.FluidAction.EXECUTE);
            return !drained.isEmpty() && this.context.exchange(ItemVariant.of(handler.getContainer()), 1, transaction) == 1
                    ? drained.getAmount() * DROPLETS_PER_MB : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return this.handler().getFluidInTank(0).isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            FluidStack fluid = this.handler().getFluidInTank(0);
            return fluid.isEmpty() ? FluidVariant.blank() : FluidVariant.of(fluid.getFluid());
        }

        @Override
        public long getAmount() {
            return this.handler().getFluidInTank(0).getAmount() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return this.handler().getTankCapacity(0) * DROPLETS_PER_MB;
        }
    }
}
