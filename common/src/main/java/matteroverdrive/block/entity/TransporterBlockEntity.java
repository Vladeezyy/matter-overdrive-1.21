package matteroverdrive.block.entity;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOSounds;
import matteroverdrive.item.TransportFlashDriveItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.TransporterMenu;
import matteroverdrive.transport.TransportLocation;
import net.minecraft.core.BlockPos;
import matteroverdrive.compat.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 1.7.10 TileEntityMachineTransporter: up to 3 entities standing on it are sent to the selected destination after 70
 * ticks (speed upgrades), then it rests 80 ticks; each jump costs 16 FE per block of distance (power usage upgrades).
 * Destinations must be within 32 blocks (range upgrades) and not right above or below it. 1024000 FE (512 FE/t in),
 * a 512 mB matter tank (unused, like 1.7.10), a transport flash drive slot to import marked spots.
 */
public class TransporterBlockEntity extends MachineBlockEntity {
    public static final int MAX_ENTITIES_PER_TRANSPORT = 3;
    public static final int TRANSPORT_TIME = 70;
    public static final int TRANSPORT_DELAY = 80;
    public static final int TRANSPORT_RANGE = 32;
    public static final int ENERGY_STORAGE = 1024000;
    public static final int ENERGY_INPUT = 512;
    public static final int ENERGY_PER_UNIT = 16;
    public static final int FLASH_DRIVE = 0;

    private final List<TransportLocation> locations = new ArrayList<>();
    private int selected;
    private int transportTimer;
    private long transportTracker;

    public TransporterBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.TRANSPORTER.get(), pos, state, slots(), true, 5, ENERGY_STORAGE, ENERGY_INPUT, 32000,
                Set.of(UpgradeType.POWER_USAGE, UpgradeType.SPEED, UpgradeType.RANGE, UpgradeType.POWER_STORAGE));
        initMatter(512, 512, 512);
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.OTHER, r -> r.getItem() instanceof TransportFlashDriveItem, 1);
        return b;
    }

    // --- locations (1.7.10 setSelectedLocation / addNewLocation / removeLocation) --------------------

    public List<TransportLocation> getLocations() {
        return locations;
    }

    public int getSelectedIndex() {
        return selected;
    }

    /** The selected destination, or this block ("Unknown") without one. */
    public TransportLocation getSelected() {
        return selected >= 0 && selected < locations.size() ? locations.get(selected) : new TransportLocation("Unknown", getBlockPos());
    }

    public void select(int index) {
        if (index >= 0 && index < locations.size()) {
            selected = index;
            changed();
        }
    }

    public void setSelected(String name, BlockPos pos) {
        TransportLocation location = new TransportLocation(clip(name), pos.immutable());
        if (selected >= 0 && selected < locations.size()) {
            locations.set(selected, location);
        } else {
            selected = 0;
            locations.add(location);
        }
        changed();
    }

    public void addLocation(String name) {
        locations.add(new TransportLocation(clip(name), getBlockPos()));
        selected = locations.size() - 1;
        changed();
    }

    public void removeLocation(int index) {
        if (index < 0 || index >= locations.size()) return;
        locations.remove(index);
        selected = Mth.clamp(selected, 0, Math.max(0, locations.size() - 1));
        changed();
    }

    /** 1.7.10 GuiTransporter "Import": the flash drive's marked block, one block up. */
    public boolean importFromFlashDrive(String name) {
        BlockPos target = TransportFlashDriveItem.getTarget(inventory.getStack(FLASH_DRIVE));
        if (target == null || !isInRange(target.above())) return false;
        setSelected(name, target.above());
        return true;
    }

    private static String clip(String name) {
        return name.length() > TransportLocation.MAX_NAME ? name.substring(0, TransportLocation.MAX_NAME) : name;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // --- transport ---------------------------------------------------------------------------------

    public int getRange() {
        return (int) Math.round(getUpgradeMultiplier(UpgradeType.RANGE) * TRANSPORT_RANGE);
    }

    public boolean isInRange(BlockPos pos) {
        return Math.sqrt(pos.distSqr(getBlockPos())) < getRange();
    }

    /** 1.7.10 isLocationValid: in range and not in its own column within 4 blocks. */
    public boolean isLocationValid(TransportLocation location) {
        BlockPos p = location.pos(), me = getBlockPos();
        boolean ownColumn = p.getX() == me.getX() && p.getZ() == me.getZ() && p.getY() < me.getY() + 4 && p.getY() > me.getY() - 4;
        return !ownColumn && isInRange(p);
    }

    public int getEnergyDrain() {
        return (int) Math.round(getUpgradeMultiplier(UpgradeType.POWER_USAGE) * getSelected().distanceTo(getBlockPos()) * ENERGY_PER_UNIT);
    }

    public int getSpeed() {
        return Math.max(1, (int) Math.round(getUpgradeMultiplier(UpgradeType.SPEED) * TRANSPORT_TIME));
    }

    public int getTransportDelay() {
        return (int) Math.round(getUpgradeMultiplier(UpgradeType.SPEED) * TRANSPORT_DELAY);
    }

    @Override
    public float getProgress() {
        return Math.min(1, (float) transportTimer / getSpeed());
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        ServerLevel level = (ServerLevel) getLevel();
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(getBlockPos()).expandTowards(0, 1, 0),
                e -> !e.isSpectator() && !e.isPassenger());
        TransportLocation target = getSelected();
        if (!redstoneAllows || entities.isEmpty() || energy.getEnergy() <= getEnergyDrain() || !isLocationValid(target)) {
            transportTimer = 0;
            return false;
        }
        if (transportTracker >= level.getGameTime()) return false;
        transportTimer++;
        if (transportTimer % 2 == 0) {
            for (Entity e : entities) particles(level, e.position(), e);
            particles(level, Vec3.atBottomCenterOf(target.pos()), entities.get(0));
        }
        if (transportTimer >= getSpeed()) {
            Vec3 to = Vec3.atBottomCenterOf(target.pos());
            for (int i = 0; i < Math.min(entities.size(), MAX_ENTITIES_PER_TRANSPORT); i++) {
                Entity e = entities.get(i);
                e.teleportTo(level, to.x, to.y, to.z, RelativeMovement.ROTATION, e.getYRot(), e.getXRot());
                e.resetFallDistance();
                // 1.7.10 MOEventTransport ("Is it really me?")
                if (e instanceof net.minecraft.world.entity.player.Player player) {
                    matteroverdrive.quest.QuestEvents.onEvent(player, new matteroverdrive.quest.QuestEvents.Transport(getBlockPos(), target.pos()));
                }
            }
            energy.add(-getEnergyDrain());
            transportTracker = level.getGameTime() + getTransportDelay();
            transportTimer = 0;
        }
        return true;
    }

    /** 1.7.10 SpawnReplicateParticles: a swirl around the entity, here and at the destination. */
    private static void particles(ServerLevel level, Vec3 at, Entity entity) {
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + entity.getBbHeight() / 2, at.z, 12,
                entity.getBbWidth() / 2, entity.getBbHeight() / 3, entity.getBbWidth() / 2, 0.3);
    }

    // --- persistence -------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("locations", TransportLocation.CODEC.listOf(), List.copyOf(locations));
        output.putInt("selected", selected);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        locations.clear();
        input.read("locations", TransportLocation.CODEC.listOf()).ifPresent(locations::addAll);
        selected = input.getIntOr("selected", 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!locations.isEmpty()) {
            components.set(MODataComponents.TRANSPORT_LOCATIONS.get(), List.copyOf(locations));
            components.set(MODataComponents.TRANSPORT_SELECTED.get(), selected);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        List<TransportLocation> stored = components.get(MODataComponents.TRANSPORT_LOCATIONS.get());
        if (stored != null) {
            locations.clear();
            locations.addAll(stored);
            selected = components.getOrDefault(MODataComponents.TRANSPORT_SELECTED.get(), 0);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("locations");
        output.discard("selected");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TransporterMenu(id, inventory, this, dataAccess);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getLoopSound() {
        return MOSounds.TRANSPORTER.get();
    }

    @Override
    public float getLoopVolume() {
        return 0.5f;
    }
}
