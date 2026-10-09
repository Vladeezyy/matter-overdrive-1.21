package matteroverdrive.machine;

import java.util.function.ToDoubleFunction;

import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 1.7.10 MachineEnergyStorage: capacity scales with POWER_STORAGE upgrades, transfer rates with POWER_TRANSFER.
 * Call {@link #refresh()} whenever the upgrades change.
 */
public class MachineEnergy extends EnergyStorage {
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
        maxReceive = (int) (baseInsert * transfer);
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
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public void set(int amount) {
        if (amount == energy) return;
        energy = amount;
        onChanged.run();
    }

    /** Machine-side change that ignores the insert/extract limits (generation and consumption). */
    public void add(int amount) {
        set(Math.max(0, Math.min(capacity, energy + amount)));
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) onChanged.run();
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) onChanged.run();
        return extracted;
    }

    public void serialize(ValueOutput output) {
        output.putInt("energy", energy);
    }

    public void deserialize(ValueInput input) {
        energy = Math.max(0, input.getIntOr("energy", 0));
    }
}
