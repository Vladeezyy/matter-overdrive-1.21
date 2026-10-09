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
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

/**
 * 1.21.1 stand-in for the 1.21.6+ {@code net.minecraft.world.level.storage.ValueInput}: the subset of its API the
 * mod uses, read from a CompoundTag. Keeps the save code the same as on the 1.21.10 branch.
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
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(tag.getCompound(key), lookup)) : Optional.empty();
    }

    public ValueInput childOrEmpty(String key) {
        return new ValueInput(tag.getCompound(key), lookup);
    }

    public Optional<ValueInputList> childrenList(String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) return Optional.empty();
        return Optional.of(childrenListOrEmpty(key));
    }

    public ValueInputList childrenListOrEmpty(String key) {
        List<ValueInput> children = new ArrayList<>();
        for (Tag t : tag.getList(key, Tag.TAG_COMPOUND)) children.add(new ValueInput((CompoundTag) t, lookup));
        return new ValueInputList(children);
    }

    public <T> Optional<TypedInputList<T>> list(String key, Codec<T> codec) {
        if (!tag.contains(key, Tag.TAG_LIST)) return Optional.empty();
        return Optional.of(listOrEmpty(key, codec));
    }

    public <T> TypedInputList<T> listOrEmpty(String key, Codec<T> codec) {
        List<T> values = new ArrayList<>();
        Tag list = tag.get(key);
        if (list instanceof ListTag l) {
            var ops = lookup.createSerializationContext(NbtOps.INSTANCE);
            for (Tag t : l) codec.parse(ops, t).result().ifPresent(values::add);
        }
        return new TypedInputList<>(values);
    }

    private Optional<NumericTag> number(String key) {
        return tag.get(key) instanceof NumericTag n ? Optional.of(n) : Optional.empty();
    }

    public boolean getBooleanOr(String key, boolean defaultValue) {
        return number(key).map(n -> n.getAsByte() != 0).orElse(defaultValue);
    }

    public byte getByteOr(String key, byte defaultValue) {
        return number(key).map(NumericTag::getAsByte).orElse(defaultValue);
    }

    public int getShortOr(String key, short defaultValue) {
        return number(key).map(NumericTag::getAsShort).orElse(defaultValue);
    }

    public Optional<Integer> getInt(String key) {
        return number(key).map(NumericTag::getAsInt);
    }

    public int getIntOr(String key, int defaultValue) {
        return number(key).map(NumericTag::getAsInt).orElse(defaultValue);
    }

    public Optional<Long> getLong(String key) {
        return number(key).map(NumericTag::getAsLong);
    }

    public long getLongOr(String key, long defaultValue) {
        return number(key).map(NumericTag::getAsLong).orElse(defaultValue);
    }

    public float getFloatOr(String key, float defaultValue) {
        return number(key).map(NumericTag::getAsFloat).orElse(defaultValue);
    }

    public double getDoubleOr(String key, double defaultValue) {
        return number(key).map(NumericTag::getAsDouble).orElse(defaultValue);
    }

    public Optional<String> getString(String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public String getStringOr(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    public Optional<int[]> getIntArray(String key) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? Optional.of(tag.getIntArray(key)) : Optional.empty();
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
