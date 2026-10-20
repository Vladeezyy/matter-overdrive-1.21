package matteroverdrive.platform.transfer;

/** Stored energy (the methods of NeoForge 1.21.1's {@code IEnergyStorage}); Fabric has no common energy API */
public interface IEnergyStorage {
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
