package matteroverdrive.platform.transfer;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/** Energy kept in an item's data component (NeoForge's {@code ComponentEnergyStorage}) */
public class ComponentEnergyStorage implements IEnergyStorage {
    protected final ItemStack parent;
    protected final DataComponentType<Integer> energyComponent;
    protected final int capacity;
    protected final int maxReceive;
    protected final int maxExtract;

    public ComponentEnergyStorage(ItemStack parent, DataComponentType<Integer> energyComponent, int capacity, int maxReceive, int maxExtract) {
        this.parent = parent;
        this.energyComponent = energyComponent;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public ComponentEnergyStorage(ItemStack parent, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
        this(parent, energyComponent, capacity, maxTransfer, maxTransfer);
    }

    public ComponentEnergyStorage(ItemStack parent, DataComponentType<Integer> energyComponent, int capacity) {
        this(parent, energyComponent, capacity, capacity, capacity);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!this.canReceive() || toReceive <= 0) {
            return 0;
        }
        int energy = this.getEnergyStored();
        int received = Math.max(0, Math.min(this.capacity - energy, Math.min(this.maxReceive, toReceive)));
        if (!simulate && received > 0) {
            this.setEnergy(energy + received);
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!this.canExtract() || toExtract <= 0) {
            return 0;
        }
        int energy = this.getEnergyStored();
        int extracted = Math.min(energy, Math.min(this.maxExtract, toExtract));
        if (!simulate && extracted > 0) {
            this.setEnergy(energy - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        int stored = this.parent.getOrDefault(this.energyComponent, 0);
        return Math.max(0, Math.min(this.capacity, stored));
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

    protected void setEnergy(int energy) {
        this.parent.set(this.energyComponent, Math.max(0, Math.min(this.capacity, energy)));
    }
}
