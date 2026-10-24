package matteroverdrive.forge;

import matteroverdrive.platform.transfer.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/** The common item, fluid and energy handlers of Matter Overdrive as Forge capabilities and back (wrappers unwrap each other) */
public final class ForgeTransfer {
    private ForgeTransfer() {}

    // --- fluids ---

    public static net.minecraftforge.fluids.FluidStack toForge(FluidStack stack) {
        return stack.isEmpty() ? net.minecraftforge.fluids.FluidStack.EMPTY : new net.minecraftforge.fluids.FluidStack(stack.getFluid(), stack.getAmount());
    }

    public static FluidStack fromForge(net.minecraftforge.fluids.FluidStack stack) {
        return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), stack.getAmount());
    }

    private static IFluidHandler.FluidAction toForge(matteroverdrive.platform.transfer.IFluidHandler.FluidAction action) {
        return action.execute() ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
    }

    private static matteroverdrive.platform.transfer.IFluidHandler.FluidAction fromForge(IFluidHandler.FluidAction action) {
        return action.execute() ? matteroverdrive.platform.transfer.IFluidHandler.FluidAction.EXECUTE : matteroverdrive.platform.transfer.IFluidHandler.FluidAction.SIMULATE;
    }

    @Nullable
    public static IFluidHandler toForge(@Nullable matteroverdrive.platform.transfer.IFluidHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromForgeFluids wrapped) {
            return wrapped.forge;
        }
        return new ToForgeFluids(handler);
    }

    @Nullable
    public static matteroverdrive.platform.transfer.IFluidHandler fromForge(@Nullable IFluidHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ToForgeFluids wrapped) {
            return wrapped.handler;
        }
        return handler instanceof IFluidHandlerItem item ? new FromForgeItemFluids(item) : new FromForgeFluids(handler);
    }

    /** An item's fluid handler as Forge's (the container may change) */
    @Nullable
    public static IFluidHandlerItem toForgeItem(@Nullable matteroverdrive.platform.transfer.IFluidHandlerItem handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromForgeItemFluids wrapped) {
            return (IFluidHandlerItem) wrapped.forge;
        }
        return new ToForgeItemFluids(handler);
    }

    record ToForgeItemFluids(matteroverdrive.platform.transfer.IFluidHandlerItem handler) implements IFluidHandlerItem {
        @Override public int getTanks() { return this.handler.getTanks(); }
        @Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) { return toForge(this.handler.getFluidInTank(tank)); }
        @Override public int getTankCapacity(int tank) { return this.handler.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack stack) { return this.handler.isFluidValid(tank, fromForge(stack)); }
        @Override public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) { return this.handler.fill(fromForge(resource), ForgeTransfer.fromForge(action)); }
        @Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) { return toForge(this.handler.drain(fromForge(resource), ForgeTransfer.fromForge(action))); }
        @Override public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) { return toForge(this.handler.drain(maxDrain, ForgeTransfer.fromForge(action))); }
        @Override public ItemStack getContainer() { return this.handler.getContainer(); }
    }

    record ToForgeFluids(matteroverdrive.platform.transfer.IFluidHandler handler) implements IFluidHandler {
        @Override public int getTanks() { return this.handler.getTanks(); }
        @Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) { return toForge(this.handler.getFluidInTank(tank)); }
        @Override public int getTankCapacity(int tank) { return this.handler.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, net.minecraftforge.fluids.FluidStack stack) { return this.handler.isFluidValid(tank, fromForge(stack)); }
        @Override public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) { return this.handler.fill(fromForge(resource), ForgeTransfer.fromForge(action)); }
        @Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) { return toForge(this.handler.drain(fromForge(resource), ForgeTransfer.fromForge(action))); }
        @Override public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) { return toForge(this.handler.drain(maxDrain, ForgeTransfer.fromForge(action))); }
    }

    static class FromForgeFluids implements matteroverdrive.platform.transfer.IFluidHandler {
        final IFluidHandler forge;

        FromForgeFluids(IFluidHandler forge) {
            this.forge = forge;
        }

        @Override public int getTanks() { return this.forge.getTanks(); }
        @Override public FluidStack getFluidInTank(int tank) { return fromForge(this.forge.getFluidInTank(tank)); }
        @Override public int getTankCapacity(int tank) { return this.forge.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return this.forge.isFluidValid(tank, toForge(stack)); }
        @Override public int fill(FluidStack resource, FluidAction action) { return this.forge.fill(toForge(resource), ForgeTransfer.toForge(action)); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return fromForge(this.forge.drain(toForge(resource), ForgeTransfer.toForge(action))); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return fromForge(this.forge.drain(maxDrain, ForgeTransfer.toForge(action))); }
    }

    static final class FromForgeItemFluids extends FromForgeFluids implements matteroverdrive.platform.transfer.IFluidHandlerItem {
        FromForgeItemFluids(IFluidHandlerItem forge) {
            super(forge);
        }

        @Override
        public ItemStack getContainer() {
            return ((IFluidHandlerItem) this.forge).getContainer();
        }
    }

    // --- items ---

    @Nullable
    public static IItemHandler toForge(@Nullable matteroverdrive.platform.transfer.IItemHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromForgeItems wrapped) {
            return wrapped.forge;
        }
        return handler instanceof matteroverdrive.platform.transfer.IItemHandlerModifiable modifiable ? new ToForgeModifiableItems(modifiable) : new ToForgeItems(handler);
    }

    @Nullable
    public static matteroverdrive.platform.transfer.IItemHandler fromForge(@Nullable IItemHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ToForgeItems wrapped) {
            return wrapped.handler;
        }
        return new FromForgeItems(handler);
    }

    static class ToForgeItems implements IItemHandler {
        final matteroverdrive.platform.transfer.IItemHandler handler;

        ToForgeItems(matteroverdrive.platform.transfer.IItemHandler handler) {
            this.handler = handler;
        }

        @Override public int getSlots() { return this.handler.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return this.handler.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return this.handler.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return this.handler.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return this.handler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return this.handler.isItemValid(slot, stack); }
    }

    static final class ToForgeModifiableItems extends ToForgeItems implements IItemHandlerModifiable {
        ToForgeModifiableItems(matteroverdrive.platform.transfer.IItemHandlerModifiable handler) {
            super(handler);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            ((matteroverdrive.platform.transfer.IItemHandlerModifiable) this.handler).setStackInSlot(slot, stack);
        }
    }

    static final class FromForgeItems implements matteroverdrive.platform.transfer.IItemHandlerModifiable {
        final IItemHandler forge;

        FromForgeItems(IItemHandler forge) {
            this.forge = forge;
        }

        @Override public int getSlots() { return this.forge.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return this.forge.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return this.forge.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return this.forge.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return this.forge.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return this.forge.isItemValid(slot, stack); }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (this.forge instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, stack);
            }
        }
    }

    // --- energy ---

    @Nullable
    public static IEnergyStorage toForge(@Nullable matteroverdrive.platform.transfer.IEnergyStorage handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FromForgeEnergy wrapped) {
            return wrapped.forge();
        }
        return new ToForgeEnergy(handler);
    }

    @Nullable
    public static matteroverdrive.platform.transfer.IEnergyStorage fromForge(@Nullable IEnergyStorage handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ToForgeEnergy wrapped) {
            return wrapped.handler();
        }
        return new FromForgeEnergy(handler);
    }

    record ToForgeEnergy(matteroverdrive.platform.transfer.IEnergyStorage handler) implements IEnergyStorage {
        @Override public int receiveEnergy(int toReceive, boolean simulate) { return this.handler.receiveEnergy(toReceive, simulate); }
        @Override public int extractEnergy(int toExtract, boolean simulate) { return this.handler.extractEnergy(toExtract, simulate); }
        @Override public int getEnergyStored() { return this.handler.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return this.handler.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return this.handler.canExtract(); }
        @Override public boolean canReceive() { return this.handler.canReceive(); }
    }

    record FromForgeEnergy(IEnergyStorage forge) implements matteroverdrive.platform.transfer.IEnergyStorage {
        @Override public int receiveEnergy(int toReceive, boolean simulate) { return this.forge.receiveEnergy(toReceive, simulate); }
        @Override public int extractEnergy(int toExtract, boolean simulate) { return this.forge.extractEnergy(toExtract, simulate); }
        @Override public int getEnergyStored() { return this.forge.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return this.forge.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return this.forge.canExtract(); }
        @Override public boolean canReceive() { return this.forge.canReceive(); }
    }
}
