package matteroverdrive.machine;

import java.util.function.ToDoubleFunction;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import matteroverdrive.init.MOFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 1.7.10 MachineMatterStorage: a one-slot tank that only holds Matter Plasma. Capacity scales with MATTER_STORAGE
 * upgrades; maxInsert/maxExtract limit what pipes and neighbours may move (machine logic uses {@link #add}).
 */
public class MatterTank implements IFluidHandler {
    private final int baseCapacity;
    private final int maxInsert;
    private final int maxExtract;
    private final ToDoubleFunction<UpgradeType> upgrades;
    private final Runnable onChanged;
    private int matter;

    public MatterTank(int capacity, int maxInsert, int maxExtract, ToDoubleFunction<UpgradeType> upgrades, Runnable onChanged) {
        this.baseCapacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.upgrades = upgrades;
        this.onChanged = onChanged;
    }

    public int getMatter() {
        return matter;
    }

    public int getCapacity() {
        return (int) Math.min(Integer.MAX_VALUE, baseCapacity * upgrades.applyAsDouble(UpgradeType.MATTER_STORAGE));
    }

    public int getMaxInsert() {
        return maxInsert;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public int getFreeSpace() {
        return Math.max(0, getCapacity() - getMatter());
    }

    /** Machine-side change ignoring the transfer limits; clamps to [0, capacity]. */
    public void add(int amount) {
        setMatter(Math.max(0, Math.min(getCapacity(), getMatter() + amount)));
    }

    public void setMatter(int amount) {
        amount = Math.max(0, amount);
        if (amount == matter) return;
        matter = amount;
        onChanged.run();
    }

    // --- IFluidHandler (pipes and neighbours) ------------------------------------------------------

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return matter <= 0 ? FluidStack.EMPTY : new FluidStack(MOFluids.MATTER_PLASMA.get(), matter);
    }

    @Override
    public int getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.isEmpty() || stack.is(MOFluids.MATTER_PLASMA.get());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        int filled = Math.min(Math.min(resource.getAmount(), maxInsert), getFreeSpace());
        if (filled > 0 && action.execute()) setMatter(matter + filled);
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.is(MOFluids.MATTER_PLASMA.get())) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = Math.min(Math.min(maxDrain, maxExtract), matter);
        if (drained <= 0) return FluidStack.EMPTY;
        if (action.execute()) setMatter(matter - drained);
        return new FluidStack(MOFluids.MATTER_PLASMA.get(), drained);
    }

    public void serialize(ValueOutput output) {
        output.putInt("matter", matter);
    }

    public void deserialize(ValueInput input) {
        matter = Math.max(0, input.getIntOr("matter", 0));
    }
}
