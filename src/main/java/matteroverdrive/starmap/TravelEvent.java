package matteroverdrive.starmap;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.7.10 TravelEvent: a ship on its way from one planet to another; ly * 8 ticks (or au * 10 within a system). */
public class TravelEvent {
    private long timeStart;
    private int timeLength;
    private GalacticPosition from = GalacticPosition.NONE, to = GalacticPosition.NONE;
    private ItemStack ship = ItemStack.EMPTY;

    private TravelEvent() {}

    public TravelEvent(Level level, GalacticPosition from, GalacticPosition to, ItemStack ship, Galaxy galaxy) {
        this.timeStart = level.getGameTime();
        this.from = from;
        this.to = to;
        this.ship = ship;
        this.timeLength = (int) (from.distanceToLY(galaxy, to) * Galaxy.LY_TO_TICKS);
        if (timeLength == 0) timeLength = (int) (from.distanceToAU(galaxy, to) * Galaxy.AU_TO_TICKS);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!ship.isEmpty()) tag.put("Ship", Planet.saveStack(registries, ship));
        tag.putInt("TimeLength", timeLength);
        tag.putLong("TimeStart", timeStart);
        tag.put("From", from.toNBT());
        tag.put("To", to.toNBT());
        return tag;
    }

    public static TravelEvent fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        TravelEvent event = new TravelEvent();
        event.from = GalacticPosition.fromNBT(tag.getCompoundOrEmpty("From"));
        event.to = GalacticPosition.fromNBT(tag.getCompoundOrEmpty("To"));
        event.ship = tag.getCompound("Ship").map(t -> Planet.loadStack(registries, t)).orElse(ItemStack.EMPTY);
        event.timeLength = tag.getIntOr("TimeLength", 0);
        event.timeStart = tag.getLongOr("TimeStart", 0);
        return event;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        GalacticPosition.STREAM_CODEC.encode(buf, from);
        GalacticPosition.STREAM_CODEC.encode(buf, to);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ship);
        buf.writeInt(timeLength);
        buf.writeLong(timeStart);
    }

    public static TravelEvent read(RegistryFriendlyByteBuf buf) {
        TravelEvent event = new TravelEvent();
        event.from = GalacticPosition.STREAM_CODEC.decode(buf);
        event.to = GalacticPosition.STREAM_CODEC.decode(buf);
        event.ship = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        event.timeLength = buf.readInt();
        event.timeStart = buf.readLong();
        return event;
    }

    public int getTimeLength() {
        return (int) Math.ceil(timeLength * Galaxy.GALAXY_TRAVEL_TIME_MULTIPLY);
    }

    public long getTimeStart() {
        return timeStart;
    }

    public long getTimeRemaining(Level level) {
        return timeStart + getTimeLength() - level.getGameTime();
    }

    /** 1.7.10 getPercent: without the travel time multiplier. */
    public double getPercent(Level level) {
        return 1d - (double) (timeStart + timeLength - level.getGameTime()) / timeLength;
    }

    public ItemStack getShip() {
        return ship;
    }

    public GalacticPosition getFrom() {
        return from;
    }

    public GalacticPosition getTo() {
        return to;
    }

    public boolean isValid(Galaxy galaxy) {
        return galaxy.getPlanet(from) != null && galaxy.getPlanet(to) != null && !ship.isEmpty();
    }

    public boolean isComplete(Level level) {
        return getTimeRemaining(level) <= 0;
    }

}
