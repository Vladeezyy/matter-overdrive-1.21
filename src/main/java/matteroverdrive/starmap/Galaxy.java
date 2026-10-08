package matteroverdrive.starmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 Galaxy: the quadrants (with their stars and planets) and the ships travelling between planets. One per world,
 * generated from the world seed; the server updates one quadrant per tick.
 */
public class Galaxy extends SpaceBody {
    public static final float GALAXY_SIZE_TO_LY = 8000;
    public static final float LY_TO_TICKS = 8;
    public static final float AU_TO_TICKS = 10;
    public static final float PLANET_SYSTEM_SIZE_TO_AU = 100;
    public static float GALAXY_BUILD_TIME_MULTIPLY = 1;
    public static float GALAXY_TRAVEL_TIME_MULTIPLY = 1;
    /** 1.7.10 Reference.COLOR_HOLO / _RED / _GREEN / _YELLOW / _PURPLE. */
    public static final int COLOR_HOLO = 0xA9E2FB, COLOR_HOLO_RED = 0xE65014, COLOR_HOLO_GREEN = 0x18CF00, COLOR_HOLO_YELLOW = 0xFCDF74,
            COLOR_HOLO_PURPLE = 0x7417E6;

    private long seed;
    private final Map<Integer, Quadrant> quadrants = new HashMap<>();
    private final List<TravelEvent> travelEvents = new ArrayList<>();
    private int version;
    private boolean dirty;
    private Iterator<Quadrant> quadrantUpdateIterator = quadrants.values().iterator();

    public Galaxy() {}

    public Galaxy(String name, int id, long seed) {
        super(name, id);
        this.seed = seed;
    }

    /** 1.7.10 update: travel events, then one quadrant per call. */
    public void update(Level level) {
        manageTravelEvents(level);
        try {
            if (quadrantUpdateIterator.hasNext()) quadrantUpdateIterator.next().update(level);
            else quadrantUpdateIterator = quadrants.values().iterator();
        } catch (java.util.ConcurrentModificationException e) {
            quadrantUpdateIterator = quadrants.values().iterator();
        }
    }

    private void manageTravelEvents(Level level) {
        Iterator<TravelEvent> it = travelEvents.iterator();
        while (it.hasNext()) {
            TravelEvent event = it.next();
            if (!event.isValid(this)) {
                it.remove();
            } else if (event.isComplete(level)) {
                if (!level.isClientSide()) {
                    Planet to = getPlanet(event.getTo());
                    Planet from = getPlanet(event.getFrom());
                    if (to != null && from != null) {
                        to.addShip(event.getShip());
                        from.markDirty();
                        to.markDirty();
                        to.onTravelEvent(event.getShip(), level);
                        if (level instanceof net.minecraft.server.level.ServerLevel server) GalaxyServer.sendTravelEvents(server.getServer());
                    }
                }
                it.remove();
            }
        }
    }

    void onSave() {
        dirty = false;
        for (Quadrant quadrant : getQuadrants()) quadrant.onSave();
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeBody(tag);
        tag.putInt("Version", version);
        ListTag list = new ListTag();
        for (Quadrant quadrant : getQuadrants()) list.add(quadrant.toNBT(registries));
        tag.put("Quadrants", list);
        ListTag events = new ListTag();
        for (TravelEvent event : travelEvents) events.add(event.toNBT(registries));
        tag.put("TravelEvents", events);
        return tag;
    }

    public void readNBT(CompoundTag tag, HolderLookup.Provider registries) {
        readBody(tag);
        quadrants.clear();
        travelEvents.clear();
        version = tag.getIntOr("Version", 0);
        ListTag list = tag.getListOrEmpty("Quadrants");
        for (int i = 0; i < list.size(); i++) {
            Quadrant quadrant = new Quadrant();
            quadrant.readNBT(list.getCompoundOrEmpty(i), registries);
            addQuadrant(quadrant);
        }
        ListTag events = tag.getListOrEmpty("TravelEvents");
        for (int i = 0; i < events.size(); i++) travelEvents.add(TravelEvent.fromNBT(events.getCompoundOrEmpty(i), registries));
        quadrantUpdateIterator = quadrants.values().iterator();
    }

    /** 1.7.10 writeToBuffer: the quadrants with their stars (without planets) and the travel events. */
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(version);
        buf.writeInt(quadrants.size());
        for (Quadrant quadrant : getQuadrants()) quadrant.write(buf);
        buf.writeInt(travelEvents.size());
        for (TravelEvent event : travelEvents) event.write(buf);
    }

    public static Galaxy read(RegistryFriendlyByteBuf buf) {
        Galaxy galaxy = new Galaxy();
        galaxy.version = buf.readInt();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) galaxy.addQuadrant(Quadrant.read(buf));
        galaxy.readTravelEvents(buf);
        return galaxy;
    }

    public void writeTravelEvents(RegistryFriendlyByteBuf buf) {
        buf.writeInt(travelEvents.size());
        for (TravelEvent event : travelEvents) event.write(buf);
    }

    public void readTravelEvents(RegistryFriendlyByteBuf buf) {
        travelEvents.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) travelEvents.add(TravelEvent.read(buf));
    }

    @Override
    public @Nullable SpaceBody getParent() {
        return null;
    }

    public Map<Integer, Quadrant> getQuadrantMap() {
        return quadrants;
    }

    public Collection<Quadrant> getQuadrants() {
        return quadrants.values();
    }

    public @Nullable Quadrant quadrant(int id) {
        return quadrants.get(id);
    }

    public void addQuadrant(Quadrant quadrant) {
        quadrants.put(quadrant.getId(), quadrant);
        quadrant.setGalaxy(this);
    }

    public int getStarCount() {
        int count = 0;
        for (Quadrant quadrant : getQuadrants()) count += quadrant.getStars().size();
        return count;
    }

    public @Nullable Quadrant getQuadrant(GalacticPosition position) {
        return position.quadrantID() >= 0 ? quadrants.get(position.quadrantID()) : null;
    }

    public @Nullable Star getStar(GalacticPosition position) {
        Quadrant quadrant = getQuadrant(position);
        return quadrant != null && position.starID() >= 0 ? quadrant.star(position.starID()) : null;
    }

    public @Nullable Planet getPlanet(GalacticPosition position) {
        Star star = getStar(position);
        return star != null && position.planetID() >= 0 ? star.planet(position.planetID()) : null;
    }

    public long getSeed() {
        return seed;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /** 1.7.10 getOwnedSystemCount: systems with a planet of the player (homeworld included). */
    public int getOwnedSystemCount(Player player) {
        int count = 0;
        for (Quadrant quadrant : getQuadrants()) for (Star star : quadrant.getStars()) if (star.isClaimed(player) > 1) count++;
        return count;
    }

    public int getEnemySystemCount(Player player) {
        int count = 0;
        for (Quadrant quadrant : getQuadrants()) for (Star star : quadrant.getStars()) if (star.isClaimed(player) == 1) count++;
        return count;
    }

    /** 1.7.10 canCompleteTravelEvent: the destination takes the ship (its owner looked up online). */
    public boolean canCompleteTravelEvent(TravelEvent event, Level level) {
        Planet to = getPlanet(event.getTo());
        if (to == null || !(event.getShip().getItem() instanceof ShipItem ship)) return false;
        java.util.UUID ownerID = ship.getOwnerID(event.getShip());
        Player owner = ownerID == null ? null : level.getPlayerByUUID(ownerID);
        return to.canAddShip(event.getShip(), owner);
    }

    public void addTravelEvent(TravelEvent event) {
        travelEvents.add(event);
    }

    public List<TravelEvent> getTravelEvents() {
        return travelEvents;
    }

    public boolean isDirty() {
        if (dirty) return true;
        for (Quadrant quadrant : getQuadrants()) if (quadrant.isDirty()) return true;
        return false;
    }

    public void markDirty() {
        dirty = true;
    }
}
