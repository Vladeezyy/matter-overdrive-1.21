package matteroverdrive.neoforge;

import matteroverdrive.platform.transfer.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/** The common item, fluid and energy handlers of Matter Overdrive as NeoForge capabilities and back (wrappers unwrap each other) */
public final class NeoForgeTransfer {
    private NeoForgeTransfer() {}

    // --- fluids ---

    public static net.neoforged.neoforge.fluids.FluidStack toNeo(FluidStack stack) {
        return stack.isEmpty() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : new net.neoforged.neoforge.fluids.FluidStack(stack.getFluid(), stack.getAmount());
    }

    public static FluidStack fromNeo(net.neoforged.neoforge.fluids.FluidStack stack) {
        return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), stack.getAmount());
    }

    private static IFluidHandler.FluidAction toNeo(matteroverdrive.platform.transfer.IFluidHandler.FluidAction action) {
        return action.execute() ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
    }

    private static matteroverdrive.platform.transfer.IFluidHandler.FluidAction fromNeo(IFluidHandler.FluidAction action) {
        return action.execute() ? matteroverdrive.platform.transfer.IFluidHandler.FluidAction.EXECUTE : matteroverdrive.platform.transfer.IFluidHandler.FluidAction.SIMULATE;
    }

    @Nullable
    public static IFluidHandler toNeo(@Nullable matteroverdrive.platform.transfer.IFluidHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromNeoFluids wrapped) {
            return wrapped.neo;
        }
        return new ToNeoFluids(handler);
    }

    @Nullable
    public static matteroverdrive.platform.transfer.IFluidHandler fromNeo(@Nullable IFluidHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ToNeoFluids wrapped) {
            return wrapped.handler;
        }
        return handler instanceof IFluidHandlerItem item ? new FromNeoItemFluids(item) : new FromNeoFluids(handler);
    }

    /** An item's fluid handler as NeoForge's (the container may change) */
    @Nullable
    public static IFluidHandlerItem toNeoItem(@Nullable matteroverdrive.platform.transfer.IFluidHandlerItem handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromNeoItemFluids wrapped) {
            return (IFluidHandlerItem) wrapped.neo;
        }
        return new ToNeoItemFluids(handler);
    }

    record ToNeoItemFluids(matteroverdrive.platform.transfer.IFluidHandlerItem handler) implements IFluidHandlerItem {
        @Override public int getTanks() { return this.handler.getTanks(); }
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) { return toNeo(this.handler.getFluidInTank(tank)); }
        @Override public int getTankCapacity(int tank) { return this.handler.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) { return this.handler.isFluidValid(tank, fromNeo(stack)); }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) { return this.handler.fill(fromNeo(resource), NeoForgeTransfer.fromNeo(action)); }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) { return toNeo(this.handler.drain(fromNeo(resource), NeoForgeTransfer.fromNeo(action))); }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) { return toNeo(this.handler.drain(maxDrain, NeoForgeTransfer.fromNeo(action))); }
        @Override public ItemStack getContainer() { return this.handler.getContainer(); }
    }

    record ToNeoFluids(matteroverdrive.platform.transfer.IFluidHandler handler) implements IFluidHandler {
        @Override public int getTanks() { return this.handler.getTanks(); }
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) { return toNeo(this.handler.getFluidInTank(tank)); }
        @Override public int getTankCapacity(int tank) { return this.handler.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) { return this.handler.isFluidValid(tank, fromNeo(stack)); }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) { return this.handler.fill(fromNeo(resource), NeoForgeTransfer.fromNeo(action)); }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) { return toNeo(this.handler.drain(fromNeo(resource), NeoForgeTransfer.fromNeo(action))); }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) { return toNeo(this.handler.drain(maxDrain, NeoForgeTransfer.fromNeo(action))); }
    }

    static class FromNeoFluids implements matteroverdrive.platform.transfer.IFluidHandler {
        final IFluidHandler neo;

        FromNeoFluids(IFluidHandler neo) {
            this.neo = neo;
        }

        @Override public int getTanks() { return this.neo.getTanks(); }
        @Override public FluidStack getFluidInTank(int tank) { return fromNeo(this.neo.getFluidInTank(tank)); }
        @Override public int getTankCapacity(int tank) { return this.neo.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return this.neo.isFluidValid(tank, toNeo(stack)); }
        @Override public int fill(FluidStack resource, FluidAction action) { return this.neo.fill(toNeo(resource), NeoForgeTransfer.toNeo(action)); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return fromNeo(this.neo.drain(toNeo(resource), NeoForgeTransfer.toNeo(action))); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return fromNeo(this.neo.drain(maxDrain, NeoForgeTransfer.toNeo(action))); }
    }

    static final class FromNeoItemFluids extends FromNeoFluids implements matteroverdrive.platform.transfer.IFluidHandlerItem {
        FromNeoItemFluids(IFluidHandlerItem neo) {
            super(neo);
        }

        @Override
        public ItemStack getContainer() {
            return ((IFluidHandlerItem) this.neo).getContainer();
        }
    }

    // --- items ---

    @Nullable
    public static IItemHandler toNeo(@Nullable matteroverdrive.platform.transfer.IItemHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromNeoItems wrapped) {
            return wrapped.neo;
        }
        return handler instanceof matteroverdrive.platform.transfer.IItemHandlerModifiable modifiable ? new ToNeoModifiableItems(modifiable) : new ToNeoItems(handler);
    }

    @Nullable
    public static matteroverdrive.platform.transfer.IItemHandler fromNeo(@Nullable IItemHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ToNeoItems wrapped) {
            return wrapped.handler;
        }
        return new FromNeoItems(handler);
    }

    static class ToNeoItems implements IItemHandler {
        final matteroverdrive.platform.transfer.IItemHandler handler;

        ToNeoItems(matteroverdrive.platform.transfer.IItemHandler handler) {
            this.handler = handler;
        }

        @Override public int getSlots() { return this.handler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return this.handler.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return this.handler.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return this.handler.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return this.handler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return this.handler.isItemValid(slot, stack); }
    }

    static final class ToNeoModifiableItems extends ToNeoItems implements IItemHandlerModifiable {
        ToNeoModifiableItems(matteroverdrive.platform.transfer.IItemHandlerModifiable handler) {
            super(handler);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            ((matteroverdrive.platform.transfer.IItemHandlerModifiable) this.handler).setStackInSlot(slot, stack);
        }
    }

    static final class FromNeoItems implements matteroverdrive.platform.transfer.IItemHandlerModifiable {
        final IItemHandler neo;

        FromNeoItems(IItemHandler neo) {
            this.neo = neo;
        }

        @Override public int getSlots() { return this.neo.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return this.neo.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return this.neo.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return this.neo.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return this.neo.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return this.neo.isItemValid(slot, stack); }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (this.neo instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, stack);
            }
        }
    }

    // --- energy ---

    @Nullable
    public static IEnergyStorage toNeo(@Nullable matteroverdrive.platform.transfer.IEnergyStorage handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromNeoEnergy wrapped) {
            return wrapped.neo();
        }
        return new ToNeoEnergy(handler);
    }

    @Nullable
    public static matteroverdrive.platform.transfer.IEnergyStorage fromNeo(@Nullable IEnergyStorage handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ToNeoEnergy wrapped) {
            return wrapped.handler();
        }
        return new FromNeoEnergy(handler);
    }

    record ToNeoEnergy(matteroverdrive.platform.transfer.IEnergyStorage handler) implements IEnergyStorage {
        @Override public int receiveEnergy(int toReceive, boolean simulate) { return this.handler.receiveEnergy(toReceive, simulate); }
        @Override public int extractEnergy(int toExtract, boolean simulate) { return this.handler.extractEnergy(toExtract, simulate); }
        @Override public int getEnergyStored() { return this.handler.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return this.handler.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return this.handler.canExtract(); }
        @Override public boolean canReceive() { return this.handler.canReceive(); }
    }

    record FromNeoEnergy(IEnergyStorage neo) implements matteroverdrive.platform.transfer.IEnergyStorage {
        @Override public int receiveEnergy(int toReceive, boolean simulate) { return this.neo.receiveEnergy(toReceive, simulate); }
        @Override public int extractEnergy(int toExtract, boolean simulate) { return this.neo.extractEnergy(toExtract, simulate); }
        @Override public int getEnergyStored() { return this.neo.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return this.neo.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return this.neo.canExtract(); }
        @Override public boolean canReceive() { return this.neo.canReceive(); }
    }
}
