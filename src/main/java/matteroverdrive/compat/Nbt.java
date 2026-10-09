package matteroverdrive.compat;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** 1.21.1 stand-ins for the 1.21.5+ Optional-returning CompoundTag getters. */
public final class Nbt {
    private Nbt() {}

    public static Optional<CompoundTag> compound(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(tag.getCompound(key)) : Optional.empty();
    }

    public static Optional<String> string(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public static Optional<Integer> intValue(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getInt(key)) : Optional.empty();
    }

    public static Optional<ListTag> list(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LIST) ? Optional.of((ListTag) tag.get(key)) : Optional.empty();
    }
}
