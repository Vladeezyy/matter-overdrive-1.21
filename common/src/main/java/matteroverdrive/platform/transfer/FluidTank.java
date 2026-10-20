package matteroverdrive.platform.transfer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Predicate;

/** One tank; saves under "Fluid" like NeoForge's {@code FluidTank} */
public class FluidTank implements IFluidHandler {
    protected final Predicate<FluidStack> validator;
    protected FluidStack fluid = FluidStack.EMPTY;
    protected int capacity;

    public FluidTank(int capacity) {
        this(capacity, stack -> true);
    }

    public FluidTank(int capacity, Predicate<FluidStack> validator) {
        this.capacity = capacity;
        this.validator = validator;
    }

    public FluidTank readFromNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.fluid = FluidStack.parseOptional(provider, tag.getCompoundOrEmpty("Fluid"));
        return this;
    }

    public CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (!this.fluid.isEmpty()) {
            tag.put("Fluid", this.fluid.save(provider));
        }
        return tag;
    }

    public FluidStack getFluid() {
        return this.fluid;
    }

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid;
    }

    public int getFluidAmount() {
        return this.fluid.getAmount();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getSpace() {
        return Math.max(0, this.capacity - this.fluid.getAmount());
    }

    public boolean isEmpty() {
        return this.fluid.isEmpty();
    }

    public boolean isFluidValid(FluidStack stack) {
        return this.validator.test(stack);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.fluid;
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return this.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !this.isFluidValid(resource)) {
            return 0;
        }
        if (action.simulate()) {
            if (this.fluid.isEmpty()) {
                return Math.min(this.capacity, resource.getAmount());
            }
            return FluidStack.isSameFluid(this.fluid, resource) ? Math.min(this.capacity - this.fluid.getAmount(), resource.getAmount()) : 0;
        }
        if (this.fluid.isEmpty()) {
            this.fluid = resource.copyWithAmount(Math.min(this.capacity, resource.getAmount()));
            this.onContentsChanged();
            return this.fluid.getAmount();
        }
        if (!FluidStack.isSameFluid(this.fluid, resource)) {
            return 0;
        }
        int filled = Math.min(this.capacity - this.fluid.getAmount(), resource.getAmount());
        if (filled > 0) {
            this.fluid.grow(filled);
            this.onContentsChanged();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluid(resource, this.fluid)) {
            return FluidStack.EMPTY;
        }
        return this.drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = Math.min(maxDrain, this.fluid.getAmount());
        FluidStack stack = this.fluid.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            this.fluid.shrink(drained);
            if (this.fluid.getAmount() <= 0) {
                this.fluid = FluidStack.EMPTY;
            }
            this.onContentsChanged();
        }
        return stack;
    }

    protected void onContentsChanged() {
    }
}
