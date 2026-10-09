package matteroverdrive.starmap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;

/** 1.7.10 Quadrant: a cube of the galaxy (position + size) and the stars in it. */
public class Quadrant extends SpaceBody {
    private @Nullable Galaxy galaxy;
    private final Map<Integer, Star> stars = new HashMap<>();
    private float size, x, y, z;
    private boolean dirty;

    public Quadrant() {}

    public Quadrant(String name, int id) {
        super(name, id);
    }

    public CompoundTag toNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeBody(tag);
        tag.putFloat("X", x);
        tag.putFloat("Y", y);
        tag.putFloat("Z", z);
        tag.putFloat("Size", size);
        ListTag list = new ListTag();
        for (Star star : getStars()) list.add(star.toNBT(registries));
        tag.put("Stars", list);
        return tag;
    }

    public void readNBT(CompoundTag tag, HolderLookup.Provider registries) {
        readBody(tag);
        x = tag.getFloat("X");
        y = tag.getFloat("Y");
        z = tag.getFloat("Z");
        size = tag.getFloat("Size");
        ListTag list = tag.getList("Stars", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Star star = new Star();
            star.readNBT(list.getCompound(i), registries);
            addStar(star);
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(name);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(size);
        buf.writeInt(stars.size());
        for (Star star : getStars()) star.write(buf);
    }

    public static Quadrant read(FriendlyByteBuf buf) {
        Quadrant quadrant = new Quadrant();
        quadrant.id = buf.readInt();
        quadrant.name = buf.readUtf();
        quadrant.x = buf.readFloat();
        quadrant.y = buf.readFloat();
        quadrant.z = buf.readFloat();
        quadrant.size = buf.readFloat();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) quadrant.addStar(Star.read(buf));
        return quadrant;
    }

    public void update(net.minecraft.world.level.Level level) {
        for (Star star : getStars()) star.update(level);
    }

    void onSave() {
        dirty = false;
        for (Star star : getStars()) star.onSave();
    }

    @Override
    public @Nullable SpaceBody getParent() {
        return galaxy;
    }

    public @Nullable Star star(int id) {
        return stars.get(id);
    }

    public boolean hasStar(int id) {
        return stars.containsKey(id);
    }

    public Collection<Star> getStars() {
        return stars.values();
    }

    public Map<Integer, Star> getStarMap() {
        return stars;
    }

    public void addStar(Star star) {
        stars.put(star.getId(), star);
        star.setQuadrant(this);
    }

    public @Nullable Galaxy getGalaxy() {
        return galaxy;
    }

    public void setGalaxy(@Nullable Galaxy galaxy) {
        this.galaxy = galaxy;
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

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isDirty() {
        if (dirty) return true;
        for (Star star : getStars()) if (star.isDirty()) return true;
        return false;
    }

    public void markDirty() {
        dirty = true;
    }
}
