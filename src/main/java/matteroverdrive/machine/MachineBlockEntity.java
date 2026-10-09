package matteroverdrive.machine;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.item.UpgradeItem;
import net.minecraft.core.BlockPos;
import matteroverdrive.compat.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import matteroverdrive.compat.EnergyHandlerUtil;

/**
 * Base of every powered Matter Overdrive machine (1.7.10 MOTileEntityMachine + MOTileEntityMachineEnergy):
 * inventory with upgrade slots and an optional battery slot, FE storage, redstone mode, active state.
 */
public abstract class MachineBlockEntity extends matteroverdrive.compat.CompatBlockEntity implements MenuProvider {
    /** 1.7.10 basicUpgradeHandler: no multiplier below 0.05, speed not below 0.1. */
    private static final double MIN_MULTIPLIER = 0.05;
    private static final double MIN_SPEED = 0.1;

    protected final MachineInventory inventory;
    protected final MachineEnergy energy;
    /** Matter Plasma storage for matter machines (1.7.10 MOTileEntityMachineMatter); null for the others. */
    protected @Nullable MatterTank matter;
    private final int batterySlot;
    private final Set<UpgradeType> affectedBy;
    private RedstoneMode redstoneMode = RedstoneMode.LOW;
    private boolean active;
    /** 1.7.10 MOTileEntityMachine.owner: set by a [Claim] security protocol. */
    private java.util.@Nullable UUID owner;

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
        this.batterySlot = batterySlot ? slots.add(MachineInventory.Role.ENERGY, r -> r.getCapability(Capabilities.EnergyStorage.ITEM) != null, 1) : -1;
        for (int i = 0; i < upgradeSlots; i++) {
            slots.add(MachineInventory.Role.UPGRADE, r -> r.getItem() instanceof UpgradeItem, 1);
        }
        this.inventory = slots.build(this::onInventoryChanged);
        this.energy = new MachineEnergy(capacity, maxInsert, maxExtract, this::getUpgradeMultiplier, this::setChanged);
    }

    /** Gives this machine a matter tank; call from the subclass constructor. */
    protected void initMatter(int capacity, int maxInsert, int maxExtract) {
        this.matter = new MatterTank(capacity, maxInsert, maxExtract, this::getUpgradeMultiplier, this::setChanged);
    }

    public @Nullable MatterTank getMatterTank() {
        return matter;
    }

    // --- ticking ---------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.chargeFromBattery();
        boolean enabled = be.redstoneMode.allows(level.hasNeighborSignal(pos));
        boolean nowActive = be.tickMachine(enabled);
        if (nowActive != be.active) {
            be.active = nowActive;
            be.setChanged();
            if (state.hasProperty(MachineBlock.ACTIVE)) {
                level.setBlock(pos, state.setValue(MachineBlock.ACTIVE, nowActive), Block.UPDATE_CLIENTS);
            } else {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Client ticker: keeps the machine's loop sound going while it is active. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        if (be.getLoopSound() != null) clientSoundTick.accept(be);
    }

    /** Set by the client: starts the loop sound of an active machine (1.7.10 MOTileEntityMachine.manageSound). */
    public static java.util.function.Consumer<MachineBlockEntity> clientSoundTick = be -> {};
    /** The client's playing loop sound (a MachineLoopSound). */
    public @Nullable Object clientSound;

    /** 1.7.10 getSound/hasSound: the sound looped while the machine is active, or null for a silent one. */
    public net.minecraft.sounds.@Nullable SoundEvent getLoopSound() {
        return null;
    }

    /** 1.7.10 soundVolume. */
    public float getLoopVolume() {
        return 1;
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
        IEnergyStorage battery = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (batteryChargesItem()) {
            EnergyHandlerUtil.move(energy, battery, Integer.MAX_VALUE);
        } else {
            EnergyHandlerUtil.move(battery, energy, Integer.MAX_VALUE);
        }
    }

    /** Generators fill the item in their battery slot instead of draining it (1.7.10 fusion reactor manageCharging). */
    protected boolean batteryChargesItem() {
        return false;
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
        // the renderers show what's inside (drives, the replicated item, the inscribed item)
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
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
    public @Nullable IEnergyStorage getEnergyHandler(@Nullable net.minecraft.core.Direction side) {
        return energy;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    /**
     * Values synced to an open menu. Ints are sent as shorts, so energy is split into two halves.
     * 0-1 energy, 2-3 capacity, 4 progress (0-1000), 5 active, 6 redstone mode, 7-8 matter, 9-10 matter capacity.
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
                case 7 -> matter == null ? 0 : matter.getMatter() & 0xFFFF;
                case 8 -> matter == null ? 0 : matter.getMatter() >>> 16;
                case 9 -> matter == null ? 0 : matter.getCapacity() & 0xFFFF;
                case 10 -> matter == null ? 0 : matter.getCapacity() >>> 16;
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
    public static final int DATA_COUNT = 11;

    // --- matter network destination filter (1.7.10 ComponentMatterNetworkConfigs) ------------------

    /** Adds the destination filter slot (a network flash drive). */
    protected static MachineInventory.Builder withFilterSlot(MachineInventory.Builder slots) {
        slots.add(MachineInventory.Role.FILTER, r -> r.getItem() instanceof matteroverdrive.item.NetworkFlashDriveItem, 1);
        return slots;
    }

    /** The positions in the filter slot's flash drive, or null when there's no filter (everything on the network). */
    public java.util.@Nullable List<BlockPos> getNetworkFilter() {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.spec(i).role() != MachineInventory.Role.FILTER) continue;
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof matteroverdrive.item.NetworkFlashDriveItem) return matteroverdrive.item.NetworkFlashDriveItem.getConnections(stack);
        }
        return null;
    }

    // --- security (1.7.10 MOTileEntityMachine owner / claim / unclaim / isUseableByPlayer) ---------

    public java.util.@Nullable UUID getOwner() {
        return owner;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    /** A [Claim] protocol claims an unowned machine for its owner. */
    public boolean claim(ItemStack protocol) {
        java.util.UUID protocolOwner = matteroverdrive.item.SecurityProtocolItem.getOwner(protocol);
        if (owner != null || protocolOwner == null) return false;
        owner = protocolOwner;
        ownerChanged();
        return true;
    }

    /** A [Remove] protocol of the owner removes the claim. */
    public boolean unclaim(ItemStack protocol) {
        if (owner == null || !owner.equals(matteroverdrive.item.SecurityProtocolItem.getOwner(protocol))) return false;
        owner = null;
        ownerChanged();
        return true;
    }

    /** 1.7.10 onPlaced of machines that belong to whoever places them (the star map). */
    protected void claimFor(net.minecraft.world.entity.player.Player player) {
        owner = player.getUUID();
        ownerChanged();
    }

    private void ownerChanged() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Unclaimed, or the owner, a creative player or someone carrying the owner's [Access] protocol. */
    public boolean isUseableByPlayer(net.minecraft.world.entity.player.Player player) {
        if (owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild) return true;
        for (ItemStack stack : matteroverdrive.compat.ContainerItems.of(player.getInventory())) {
            if (matteroverdrive.item.SecurityProtocolItem.is(stack, matteroverdrive.item.SecurityProtocolItem.ACCESS, owner)) return true;
        }
        return false;
    }

    /** Only the owner (or a creative player) may break or dismantle a claimed machine (1.7.10 canRemoveMachine / canDismantle). */
    public boolean canRemove(net.minecraft.world.entity.player.Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild;
    }

    // --- persistence -----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        energy.serialize(output.child("energy"));
        if (matter != null) matter.serialize(output.child("matter"));
        output.putString("redstone_mode", redstoneMode.name());
        output.putBoolean("active", active);
        output.storeNullable("owner", net.minecraft.core.UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inventory::deserialize);
        energy.refresh();
        input.child("energy").ifPresent(energy::deserialize);
        if (matter != null) input.child("matter").ifPresent(matter::deserialize);
        redstoneMode = input.getString("redstone_mode").map(s -> {
            try {
                return RedstoneMode.valueOf(s);
            } catch (IllegalArgumentException e) {
                return RedstoneMode.LOW;
            }
        }).orElse(RedstoneMode.LOW);
        active = input.getBooleanOr("active", false);
        owner = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        boolean hasEnergy = energy.getEnergy() > 0;
        boolean hasMatter = matter != null && matter.getMatter() > 0;
        if (hasEnergy) {
            components.set(MODataComponents.ENERGY.get(), energy.getEnergy());
        }
        if (hasEnergy || hasMatter) {
            components.set(MODataComponents.MACHINE_STORAGE.get(), new MachineStorage(
                    hasEnergy ? energy.getCapacity() : 0, hasEnergy ? energy.getMaxExtract() : 0, hasEnergy ? energy.getMaxInsert() : 0,
                    hasMatter ? matter.getMatter() : 0, hasMatter ? matter.getCapacity() : 0,
                    hasMatter ? matter.getMaxExtract() : 0, hasMatter ? matter.getMaxInsert() : 0));
        }
        // 1.7.10 writeToDropItem kept the owner on the machine's item
        if (owner != null) components.set(MODataComponents.SECURITY_OWNER.get(), owner);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer stored = components.get(MODataComponents.ENERGY.get());
        if (stored != null) {
            energy.refresh();
            energy.set(Math.min(stored, energy.getCapacity()));
        }
        MachineStorage storage = components.get(MODataComponents.MACHINE_STORAGE.get());
        if (matter != null && storage != null && storage.matter() > 0) {
            matter.setMatter(Math.min(storage.matter(), matter.getCapacity()));
        }
        owner = components.get(MODataComponents.SECURITY_OWNER.get());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
        output.discard("matter");
        output.discard("owner");
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
