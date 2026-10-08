package matteroverdrive.machine;

import java.util.function.ToDoubleFunction;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * 1.7.10 MachineEnergyStorage: capacity scales with POWER_STORAGE upgrades, transfer rates with POWER_TRANSFER.
 * Call {@link #refresh()} whenever the upgrades change.
 */
public class MachineEnergy extends SimpleEnergyHandler {
    private final int baseCapacity;
    private final int baseInsert;
    private final int baseExtract;
    private final ToDoubleFunction<UpgradeType> upgrades;
    private final Runnable onChanged;

    public MachineEnergy(int capacity, int maxInsert, int maxExtract, ToDoubleFunction<UpgradeType> upgrades, Runnable onChanged) {
        super(capacity, maxInsert, maxExtract);
        this.baseCapacity = capacity;
        this.baseInsert = maxInsert;
        this.baseExtract = maxExtract;
        this.upgrades = upgrades;
        this.onChanged = onChanged;
    }

    public void refresh() {
        capacity = (int) Math.min(Integer.MAX_VALUE, baseCapacity * upgrades.applyAsDouble(UpgradeType.POWER_STORAGE));
        double transfer = upgrades.applyAsDouble(UpgradeType.POWER_TRANSFER);
        maxInsert = (int) (baseInsert * transfer);
        maxExtract = (int) (baseExtract * transfer);
        if (energy > capacity) {
            set(capacity);
        }
    }

    public int getEnergy() {
        return energy;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getMaxInsert() {
        return maxInsert;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    /** Machine-side change that ignores the insert/extract limits (generation and consumption). */
    public void add(int amount) {
        set(Math.max(0, Math.min(capacity, energy + amount)));
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
