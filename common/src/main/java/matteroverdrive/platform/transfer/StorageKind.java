package matteroverdrive.platform.transfer;

/** The kinds of storage blocks offer to each other (NeoForge block capabilities, Fabric block API lookups) */
public final class StorageKind<T> {
    public static final StorageKind<IEnergyStorage> ENERGY = new StorageKind<>("energy");
    public static final StorageKind<IFluidHandler> FLUID = new StorageKind<>("fluid");
    public static final StorageKind<IItemHandler> ITEM = new StorageKind<>("item");

    private final String name;

    private StorageKind(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
