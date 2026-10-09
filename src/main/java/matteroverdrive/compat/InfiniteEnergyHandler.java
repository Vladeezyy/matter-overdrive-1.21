package matteroverdrive.compat;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** 1.21.1 stand-in for NeoForge 21.10's InfiniteEnergyHandler: gives and takes any amount. */
public final class InfiniteEnergyHandler implements IEnergyStorage {
    public static final InfiniteEnergyHandler INSTANCE = new InfiniteEnergyHandler();

    private InfiniteEnergyHandler() {}

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return Math.max(0, toReceive);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return Math.max(0, toExtract);
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
