package matteroverdrive.platform.transfer;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

/** A simple energy store with transfer limits (NeoForge's {@code EnergyStorage}); saves as an int tag like it */
public class EnergyStorage implements IEnergyStorage, matteroverdrive.platform.ValueIOSerializable {
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public EnergyStorage(int capacity) {
        this(capacity, capacity, capacity, 0);
    }

    public EnergyStorage(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer, 0);
    }

    public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, 0);
    }

    public EnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!this.canReceive() || toReceive <= 0) {
            return 0;
        }
        int received = Math.min(this.capacity - this.energy, Math.min(this.maxReceive, toReceive));
        if (!simulate) {
            this.energy += received;
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!this.canExtract() || toExtract <= 0) {
            return 0;
        }
        int extracted = Math.min(this.energy, Math.min(this.maxExtract, toExtract));
        if (!simulate) {
            this.energy -= extracted;
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }

    @Override
    public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
        output.putInt("energy", this.energy);
    }

    @Override
    public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
        this.energy = input.getIntOr("energy", 0);
    }
}
