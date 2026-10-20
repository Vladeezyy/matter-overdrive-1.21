package matteroverdrive.compat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * Stand-in for the 1.21.6+ {@code net.minecraft.world.level.storage.ValueInput}: the subset of its API the mod uses,
 * read from a CompoundTag. Keeps the save code the same as on the 1.21.10 branch.
 */
public final class ValueInput {
    private final CompoundTag tag;
    private final HolderLookup.Provider lookup;

    private ValueInput(CompoundTag tag, HolderLookup.Provider lookup) {
        this.tag = tag;
        this.lookup = lookup;
    }

    public static ValueInput of(CompoundTag tag, HolderLookup.Provider lookup) {
        return new ValueInput(tag, lookup);
    }

    public CompoundTag tag() {
        return tag;
    }

    public HolderLookup.Provider lookup() {
        return lookup;
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag value = tag.get(key);
        if (value == null) return Optional.empty();
        return codec.parse(lookup.createSerializationContext(NbtOps.INSTANCE), value).result();
    }

    public Optional<ValueInput> child(String key) {
        return tag.getCompound(key).map(t -> new ValueInput(t, lookup));
    }

    public ValueInput childOrEmpty(String key) {
        return new ValueInput(tag.getCompoundOrEmpty(key), lookup);
    }

    public Optional<ValueInputList> childrenList(String key) {
        return tag.getList(key).map(l -> childrenListOrEmpty(key));
    }

    public ValueInputList childrenListOrEmpty(String key) {
        List<ValueInput> children = new ArrayList<>();
        for (Tag t : tag.getListOrEmpty(key)) if (t instanceof CompoundTag c) children.add(new ValueInput(c, lookup));
        return new ValueInputList(children);
    }

    public <T> Optional<TypedInputList<T>> list(String key, Codec<T> codec) {
        return tag.getList(key).map(l -> listOrEmpty(key, codec));
    }

    public <T> TypedInputList<T> listOrEmpty(String key, Codec<T> codec) {
        List<T> values = new ArrayList<>();
        ListTag list = tag.getListOrEmpty(key);
        var ops = lookup.createSerializationContext(NbtOps.INSTANCE);
        for (Tag t : list) codec.parse(ops, t).result().ifPresent(values::add);
        return new TypedInputList<>(values);
    }

    public boolean getBooleanOr(String key, boolean defaultValue) {
        return tag.getBooleanOr(key, defaultValue);
    }

    public byte getByteOr(String key, byte defaultValue) {
        return tag.getByteOr(key, defaultValue);
    }

    public int getShortOr(String key, short defaultValue) {
        return tag.getShortOr(key, defaultValue);
    }

    public Optional<Integer> getInt(String key) {
        return tag.getInt(key);
    }

    public int getIntOr(String key, int defaultValue) {
        return tag.getIntOr(key, defaultValue);
    }

    public Optional<Long> getLong(String key) {
        return tag.getLong(key);
    }

    public long getLongOr(String key, long defaultValue) {
        return tag.getLongOr(key, defaultValue);
    }

    public float getFloatOr(String key, float defaultValue) {
        return tag.getFloatOr(key, defaultValue);
    }

    public double getDoubleOr(String key, double defaultValue) {
        return tag.getDoubleOr(key, defaultValue);
    }

    public Optional<String> getString(String key) {
        return tag.getString(key);
    }

    public String getStringOr(String key, String defaultValue) {
        return tag.getStringOr(key, defaultValue);
    }

    public Optional<int[]> getIntArray(String key) {
        return tag.getIntArray(key);
    }

    public static final class TypedInputList<T> implements Iterable<T> {
        private final List<T> values;

        private TypedInputList(List<T> values) {
            this.values = values;
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        public Stream<T> stream() {
            return values.stream();
        }

        @Override
        public Iterator<T> iterator() {
            return values.iterator();
        }
    }

    public static final class ValueInputList implements Iterable<ValueInput> {
        private final List<ValueInput> values;

        private ValueInputList(List<ValueInput> values) {
            this.values = values;
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        public Stream<ValueInput> stream() {
            return values.stream();
        }

        @Override
        public Iterator<ValueInput> iterator() {
            return values.iterator();
        }
    }
}
