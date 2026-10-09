package matteroverdrive.starmap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 Star: position in the galaxy (-1..1), size, mass, type, temperature, colour, seed and its planets. */
public class Star extends SpaceBody {
    private @Nullable Quadrant quadrant;
    private final Map<Integer, Planet> planets = new HashMap<>();
    private float x, y, z, size, mass;
    private byte type;
    private int temperature, color, seed;
    private boolean generated;
    private boolean dirty;

    public Star() {}

    public Star(String name, int id) {
        super(name, id);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeBody(tag);
        tag.putFloat("X", x);
        tag.putFloat("Y", y);
        tag.putFloat("Z", z);
        tag.putFloat("Size", size);
        tag.putFloat("Mass", mass);
        tag.putByte("Type", type);
        tag.putInt("Temperature", temperature);
        tag.putInt("Color", color);
        tag.putInt("Seed", seed);
        tag.putBoolean("Generated", generated);
        ListTag list = new ListTag();
        for (Planet planet : getPlanets()) list.add(planet.toNBT(registries));
        tag.put("Planets", list);
        return tag;
    }

    public void readNBT(CompoundTag tag, HolderLookup.Provider registries) {
        readBody(tag);
        x = tag.getFloat("X");
        y = tag.getFloat("Y");
        z = tag.getFloat("Z");
        size = tag.getFloat("Size");
        mass = tag.getFloat("Mass");
        type = tag.getByte("Type");
        temperature = tag.getInt("Temperature");
        color = tag.getInt("Color");
        seed = tag.getInt("Seed");
        generated = tag.getBoolean("Generated");
        ListTag list = tag.getList("Planets", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Planet planet = new Planet();
            planet.readNBT(list.getCompound(i), registries);
            addPlanet(planet);
        }
    }

    /** 1.7.10 writeToBuffer: the star without its planets (those come with PacketStarLoading / PacketUpdatePlanet). */
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(name);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(size);
        buf.writeFloat(mass);
        buf.writeByte(type);
        buf.writeInt(temperature);
        buf.writeInt(color);
    }

    public static Star read(FriendlyByteBuf buf) {
        Star star = new Star();
        star.id = buf.readInt();
        star.name = buf.readUtf();
        star.x = buf.readFloat();
        star.y = buf.readFloat();
        star.z = buf.readFloat();
        star.size = buf.readFloat();
        star.mass = buf.readFloat();
        star.type = buf.readByte();
        star.temperature = buf.readInt();
        star.color = buf.readInt();
        return star;
    }

    public void update(net.minecraft.world.level.Level level) {
        for (Planet planet : getPlanets()) planet.update(level);
    }

    void onSave() {
        dirty = false;
        for (Planet planet : getPlanets()) planet.onSave();
    }

    @Override
    public @Nullable SpaceBody getParent() {
        return quadrant;
    }

    public @Nullable Planet planet(int id) {
        return planets.get(id);
    }

    public boolean hasPlanet(int id) {
        return planets.containsKey(id);
    }

    public Collection<Planet> getPlanets() {
        return planets.values();
    }

    public Map<Integer, Planet> getPlanetMap() {
        return planets;
    }

    public void addPlanet(Planet planet) {
        planets.put(planet.getId(), planet);
        planet.setStar(this);
    }

    public void clearPlanets() {
        planets.clear();
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }

    public Vec3 getPosition(double multiply) {
        return new Vec3(x * multiply, y * multiply, z * multiply);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public @Nullable Quadrant getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(@Nullable Quadrant quadrant) {
        this.quadrant = quadrant;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    /** 1.7.10 isClaimed(player): 3 = has the player's homeworld, 2 = the player owns a planet, 1 = someone else does, 0 = free. */
    public int isClaimed(Player player) {
        int ownType = 0;
        for (Planet planet : getPlanets()) {
            if (!planet.hasOwner()) continue;
            if (planet.isOwner(player)) {
                ownType = Math.max(ownType, planet.isHomeworld() ? 3 : 2);
            }
            ownType = Math.max(ownType, 1);
        }
        return ownType;
    }

    public boolean isClaimed() {
        for (Planet planet : getPlanets()) if (planet.hasOwner()) return true;
        return false;
    }

    public boolean isDirty() {
        if (dirty) return true;
        for (Planet planet : getPlanets()) if (planet.isDirty()) return true;
        return false;
    }

    public void markDirty() {
        dirty = true;
    }
}
