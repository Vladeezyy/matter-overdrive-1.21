package matteroverdrive.compat.component;

import com.mojang.serialization.Codec;

import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.StreamCodec;

/**
 * 1.20.1 stand-in for the 1.20.5+ item data component types: a value kept in the item stack's NBT under {@link #key()},
 * written with {@link #codec()}. Read and written through {@link Components}. Not a registry entry (1.20.1 has no such
 * registry); {@link matteroverdrive.platform.Registrar.DataComponents} only names it.
 */
public final class DataComponentType<T> {
    private final String key;
    private final Codec<T> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

    DataComponentType(String key, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        this.key = key;
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    /** The NBT key of the value in the stack's tag */
    public String key() {
        return key;
    }

    public Codec<T> codec() {
        return codec;
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return streamCodec;
    }

    @Override
    public String toString() {
        return "component " + key;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private Codec<T> codec;
        private StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        public DataComponentType<T> build(String key) {
            if (codec == null) throw new IllegalStateException("component " + key + " has no codec");
            return new DataComponentType<>(key, codec, streamCodec);
        }
    }
}
