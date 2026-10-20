package matteroverdrive.starmap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

/** 1.7.10 SpaceBody: an id and a name. */
public abstract class SpaceBody {
    protected int id;
    protected String name = "";

    protected SpaceBody() {}

    protected SpaceBody(String name, int id) {
        this.id = id;
        this.name = name;
    }

    protected void writeBody(CompoundTag tag) {
        tag.putInt("ID", id);
        tag.putString("Name", name);
    }

    protected void readBody(CompoundTag tag) {
        id = tag.getInt("ID");
        name = tag.getString("Name");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract @Nullable SpaceBody getParent();
}
