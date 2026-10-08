package matteroverdrive.starmap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 Planet: orbit, size, type, owner (homeworld), building and fleet spaces, the buildings and ships on it and 4
 * construction slots (2 buildings, 2 ships). Type is always 2 after generation (1.7.10 PlanetAbstractGen set it to 2
 * whichever gen was picked; the gen only picks size and spaces).
 */
public class Planet extends SpaceBody {
    public static final int SLOT_COUNT = 4;

    private @Nullable Star star;
    private float size, orbit;
    private byte type;
    private @Nullable UUID ownerUUID;
    private final ItemStack[] inventory = new ItemStack[SLOT_COUNT];
    private final List<ItemStack> buildings = new ArrayList<>();
    private final List<ItemStack> fleet = new ArrayList<>();
    private boolean dirty, homeworld, needsClientUpdate;
    private int buildingSpaces, fleetSpaces, seed;

    public Planet() {
        java.util.Arrays.fill(inventory, ItemStack.EMPTY);
    }

    public Planet(String name, int id) {
        super(name, id);
        java.util.Arrays.fill(inventory, ItemStack.EMPTY);
    }

    // --- persistence (1.7.10 NBT keys) -------------------------------------------------------------

    static Tag saveStack(HolderLookup.Provider registries, ItemStack stack) {
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    static ItemStack loadStack(HolderLookup.Provider registries, Tag tag) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeBody(tag);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!inventory[i].isEmpty()) tag.put("Slot" + i, saveStack(registries, inventory[i]));
        }
        for (int i = 0; i < buildings.size(); i++) tag.put("Building" + i, saveStack(registries, buildings.get(i)));
        for (int i = 0; i < fleet.size(); i++) tag.put("Ship" + i, saveStack(registries, fleet.get(i)));
        if (ownerUUID != null) tag.putString("OwnerUUID", ownerUUID.toString());
        tag.putBoolean("Homeworld", homeworld);
        tag.putFloat("Size", size);
        tag.putByte("Type", type);
        tag.putFloat("Orbit", orbit);
        tag.putInt("BuildingSpaces", buildingSpaces);
        tag.putInt("FleetSpaces", fleetSpaces);
        tag.putInt("Seed", seed);
        return tag;
    }

    /** 1.7.10 readFromNBT: buildings / ships are only read up to the raw space counts. */
    public void readNBT(CompoundTag tag, HolderLookup.Provider registries) {
        readBody(tag);
        buildings.clear();
        fleet.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory[i] = tag.getCompound("Slot" + i).map(t -> loadStack(registries, t)).orElse(ItemStack.EMPTY);
        }
        buildingSpaces = tag.getIntOr("BuildingSpaces", 0);
        for (int i = 0; i < getBuildingSpaces(); i++) {
            tag.getCompound("Building" + i).map(t -> loadStack(registries, t)).filter(s -> !s.isEmpty()).ifPresent(buildings::add);
        }
        fleetSpaces = tag.getIntOr("FleetSpaces", 0);
        for (int i = 0; i < getFleetSpaces(); i++) {
            tag.getCompound("Ship" + i).map(t -> loadStack(registries, t)).filter(s -> !s.isEmpty()).ifPresent(fleet::add);
        }
        ownerUUID = null;
        tag.getString("OwnerUUID").ifPresent(s -> {
            try {
                ownerUUID = UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {}
        });
        homeworld = tag.getBooleanOr("Homeworld", false);
        size = tag.getFloatOr("Size", 0);
        type = tag.getByteOr("Type", (byte) 0);
        orbit = tag.getFloatOr("Orbit", 0);
        seed = tag.getIntOr("Seed", 0);
    }

    // --- update ---------------------------------------------------------------------------------

    /** Server: sends the planet to the clients when it changed (1.7.10 needsClientUpdate -> PacketUpdatePlanet). */
    public void update(net.minecraft.world.level.Level level) {
        if (!level.isClientSide() && needsClientUpdate) {
            needsClientUpdate = false;
            if (level instanceof net.minecraft.server.level.ServerLevel server) GalaxyServer.sendPlanet(server.getServer(), this);
        }
    }

    public void markDirty() {
        dirty = true;
        needsClientUpdate = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    void onSave() {
        dirty = false;
    }

    public void markForUpdate() {
        needsClientUpdate = true;
    }

    // --- getters / setters -------------------------------------------------------------------------

    @Override
    public @Nullable SpaceBody getParent() {
        return star;
    }

    public @Nullable Star getStar() {
        return star;
    }

    public void setStar(@Nullable Star star) {
        this.star = star;
    }

    public @Nullable UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }

    public boolean isOwner(Player player) {
        return ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    public boolean isHomeworld() {
        return homeworld;
    }

    public boolean isHomeworld(Player player) {
        return isOwner(player) && homeworld;
    }

    public void setHomeworld(boolean homeworld) {
        this.homeworld = homeworld;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public float getOrbit() {
        return orbit;
    }

    public void setOrbit(float orbit) {
        this.orbit = orbit;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public int getBuildingSpaces() {
        return (int) getStatChangeFromBuildings(PlanetStatType.BUILDINGS_SIZE, buildingSpaces);
    }

    public void setBuildingSpaces(int buildingSpaces) {
        this.buildingSpaces = buildingSpaces;
    }

    public int getFleetSpaces() {
        return (int) getStatChangeFromBuildings(PlanetStatType.FLEET_SIZE, fleetSpaces);
    }

    public void setFleetSpaces(int fleetSpaces) {
        this.fleetSpaces = fleetSpaces;
    }

    public List<ItemStack> getBuildings() {
        return buildings;
    }

    public List<ItemStack> getFleet() {
        return fleet;
    }

    public ItemStack getShip(int at) {
        return fleet.get(at);
    }

    public void addShip(ItemStack ship) {
        if (!ship.isEmpty()) fleet.add(ship);
    }

    public @Nullable ItemStack removeShip(int at) {
        return at < fleet.size() ? fleet.remove(at) : null;
    }

    public boolean removeShip(ItemStack ship) {
        return fleet.remove(ship);
    }

    public void addBuilding(ItemStack building) {
        if (!building.isEmpty()) buildings.add(building);
    }

    /** 1.7.10 getFactoryCount: the number of buildings. */
    public int getFactoryCount() {
        return buildings.size();
    }

    /** 1.7.10 getFleetCount: the number of ships. */
    public int getFleetCount() {
        return fleet.size();
    }

    public int getPopulation() {
        return (int) getStatChangeFromBuildings(PlanetStatType.POPULATION_COUNT, 0);
    }

    public int getPowerProduction() {
        return (int) getStatChangeFromBuildings(PlanetStatType.ENERGY_PRODUCTION, 0);
    }

    public float getMatterProduction() {
        return getStatChangeFromBuildings(PlanetStatType.MATTER_PRODUCTION, 0);
    }

    public float getHappiness() {
        return getStatChangeFromBuildings(PlanetStatType.HAPPINESS, 0);
    }

    public float getStatChangeFromBuildings(PlanetStatType statType, float original) {
        for (ItemStack building : buildings) {
            if (building.getItem() instanceof PlanetStatChange change) original = change.changeStat(building, this, statType, original);
        }
        return original;
    }

    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? inventory[slot] : ItemStack.EMPTY;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOT_COUNT) inventory[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    /** 1.7.10 Planet.getGuiColor: yellow own homeworld, green own, red someone else's, holo blue free. */
    public int getGuiColor(@Nullable Player viewer) {
        if (ownerUUID == null) return Galaxy.COLOR_HOLO;
        if (viewer != null && ownerUUID.equals(viewer.getUUID())) return homeworld ? Galaxy.COLOR_HOLO_YELLOW : Galaxy.COLOR_HOLO_GREEN;
        return Galaxy.COLOR_HOLO_RED;
    }
}
