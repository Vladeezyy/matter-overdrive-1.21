package matteroverdrive.machine;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.item.UpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

/**
 * Base of every powered Matter Overdrive machine (1.7.10 MOTileEntityMachine + MOTileEntityMachineEnergy):
 * inventory with upgrade slots and an optional battery slot, FE storage, redstone mode, active state.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    /** 1.7.10 basicUpgradeHandler: no multiplier below 0.05, speed not below 0.1. */
    private static final double MIN_MULTIPLIER = 0.05;
    private static final double MIN_SPEED = 0.1;

    protected final MachineInventory inventory;
    protected final MachineEnergy energy;
    private final int batterySlot;
    private final Set<UpgradeType> affectedBy;
    private RedstoneMode redstoneMode = RedstoneMode.LOW;
    private boolean active;

    /**
     * @param slots         the machine's own slots; the battery slot (if any) and the upgrade slots are appended
     * @param batterySlot   whether a battery placed in the machine charges it (1.7.10 EnergySlot)
     * @param upgradeSlots  number of upgrade slots
     */
    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineInventory.Builder slots,
                                 boolean batterySlot, int upgradeSlots, int capacity, int maxInsert, int maxExtract,
                                 Set<UpgradeType> affectedBy) {
        super(type, pos, state);
        this.affectedBy = affectedBy.isEmpty() ? EnumSet.noneOf(UpgradeType.class) : EnumSet.copyOf(affectedBy);
        this.batterySlot = batterySlot ? slots.add(MachineInventory.Role.ENERGY, r -> ItemAccess.forStack(r.toStack()).getCapability(Capabilities.Energy.ITEM) != null, 1) : -1;
        for (int i = 0; i < upgradeSlots; i++) {
            slots.add(MachineInventory.Role.UPGRADE, r -> r.getItem() instanceof UpgradeItem, 1);
        }
        this.inventory = slots.build(this::onInventoryChanged);
        this.energy = new MachineEnergy(capacity, maxInsert, maxExtract, this::getUpgradeMultiplier, this::setChanged);
    }

    // --- ticking ---------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.chargeFromBattery();
        boolean enabled = be.redstoneMode.allows(level.hasNeighborSignal(pos));
        boolean nowActive = be.tickMachine(enabled);
        if (nowActive != be.active) {
            be.active = nowActive;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /** Runs one tick of the machine's work. Returns whether it is actively working. */
    protected abstract boolean tickMachine(boolean redstoneAllows);

    /** Work progress 0..1 for the GUI, or 0 when the machine has none. */
    public float getProgress() {
        return 0;
    }

    private void chargeFromBattery() {
        if (batterySlot < 0) return;
        ItemStack stack = inventory.getStack(batterySlot);
        if (stack.isEmpty()) return;
        EnergyHandler battery = ItemAccess.forHandlerIndex(inventory, batterySlot).getCapability(Capabilities.Energy.ITEM);
        EnergyHandlerUtil.move(battery, energy, Integer.MAX_VALUE, null);
    }

    // --- upgrades ----------------------------------------------------------------------------------

    public double getUpgradeMultiplier(UpgradeType type) {
        if (!affectedBy.contains(type)) return 1;
        double multiplier = 1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.spec(i).role() == MachineInventory.Role.UPGRADE
                    && inventory.getStack(i).getItem() instanceof UpgradeItem upgrade) {
                multiplier *= upgrade.getUpgrades().getOrDefault(type, 1d);
            }
        }
        if (type == UpgradeType.SPEED) multiplier = Math.max(multiplier, MIN_SPEED);
        return Math.max(multiplier, MIN_MULTIPLIER);
    }

    public Set<UpgradeType> getAffectedBy() {
        return affectedBy;
    }

    protected void onInventoryChanged() {
        energy.refresh();
        setChanged();
    }

    // --- accessors -------------------------------------------------------------------------------

    public MachineInventory getInventory() {
        return inventory;
    }

    public MachineEnergy getEnergy() {
        return energy;
    }

    public int getBatterySlot() {
        return batterySlot;
    }

    public boolean isActive() {
        return active;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    /** Energy capability for a side; null side means internal access. */
    public @Nullable EnergyHandler getEnergyHandler(@Nullable net.minecraft.core.Direction side) {
        return energy;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    /**
     * Values synced to an open menu. Ints are sent as shorts, so energy is split into two halves.
     * 0-1 energy, 2-3 capacity, 4 progress (0-1000), 5 active, 6 redstone mode.
     */
    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergy() & 0xFFFF;
                case 1 -> energy.getEnergy() >>> 16;
                case 2 -> energy.getCapacity() & 0xFFFF;
                case 3 -> energy.getCapacity() >>> 16;
                case 4 -> Math.round(getProgress() * 1000);
                case 5 -> active ? 1 : 0;
                case 6 -> redstoneMode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    public static final int DATA_COUNT = 7;

    // --- persistence -----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        energy.serialize(output.child("energy"));
        output.putString("redstone_mode", redstoneMode.name());
        output.putBoolean("active", active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inventory::deserialize);
        energy.refresh();
        input.child("energy").ifPresent(energy::deserialize);
        redstoneMode = input.getString("redstone_mode").map(s -> {
            try {
                return RedstoneMode.valueOf(s);
            } catch (IllegalArgumentException e) {
                return RedstoneMode.LOW;
            }
        }).orElse(RedstoneMode.LOW);
        active = input.getBooleanOr("active", false);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.getEnergy() > 0) {
            components.set(MODataComponents.ENERGY.get(), energy.getEnergy());
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer stored = components.get(MODataComponents.ENERGY.get());
        if (stored != null) {
            energy.refresh();
            energy.set(Math.min(stored, energy.getCapacity()));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (int i = 0; i < inventory.size(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStack(i));
            }
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
