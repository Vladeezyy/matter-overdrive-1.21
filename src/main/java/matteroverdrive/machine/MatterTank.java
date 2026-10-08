package matteroverdrive.machine;

import java.util.function.ToDoubleFunction;

import matteroverdrive.init.MOFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 1.7.10 MachineMatterStorage: a one-slot tank that only holds Matter Plasma. Capacity scales with MATTER_STORAGE
 * upgrades; maxInsert/maxExtract limit what pipes and neighbours may move (machine logic uses {@link #add}).
 */
public class MatterTank extends FluidStacksResourceHandler {
    private final int baseCapacity;
    private final int maxInsert;
    private final int maxExtract;
    private final ToDoubleFunction<UpgradeType> upgrades;
    private final Runnable onChanged;

    public MatterTank(int capacity, int maxInsert, int maxExtract, ToDoubleFunction<UpgradeType> upgrades, Runnable onChanged) {
        super(1, capacity);
        this.baseCapacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.upgrades = upgrades;
        this.onChanged = onChanged;
    }

    public int getMatter() {
        return stacks.get(0).getAmount();
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
        if (amount <= 0) {
            set(0, FluidResource.EMPTY, 0);
        } else {
            set(0, FluidResource.of(MOFluids.MATTER_PLASMA.get()), amount);
        }
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || resource.is(MOFluids.MATTER_PLASMA.get());
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return getCapacity();
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return super.insert(index, resource, Math.min(amount, maxInsert), transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return super.extract(index, resource, Math.min(amount, maxExtract), transaction);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChanged.run();
    }
}
