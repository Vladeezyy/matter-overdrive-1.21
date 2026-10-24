package matteroverdrive.compat.codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

/** 1.20.1 stand-in for the 1.20.5+ {@code net.minecraft.network.codec.ByteBufCodecs} (the parts the mod uses). */
public final class ByteBufCodecs {
    private ByteBufCodecs() {}

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf f ? f : new FriendlyByteBuf(buf);
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((b, v) -> b.writeByte(v), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of((b, v) -> friendly(b).writeVarInt(v), b -> friendly(b).readVarInt());
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of((b, v) -> friendly(b).writeVarLong(v), b -> friendly(b).readVarLong());
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = StreamCodec.of((b, v) -> friendly(b).writeNbt(v), b -> friendly(b).readNbt());

    public static StreamCodec<ByteBuf, String> stringUtf8(int maxLength) {
        return StreamCodec.of((b, v) -> friendly(b).writeUtf(v, maxLength), b -> friendly(b).readUtf(maxLength));
    }

    public static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return StreamCodec.of((b, v) -> friendly(b).writeNbt(asCompound(codec.encodeStart(NbtOps.INSTANCE, v).getOrThrow(false, e -> {}))),
                b -> codec.parse(NbtOps.INSTANCE, unwrap(friendly(b).readNbt())).getOrThrow(false, e -> {}));
    }

    /** With the buffer's registries (a {@link RegistryFriendlyByteBuf}), else built-in ones only */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
        return StreamCodec.of(
                (b, v) -> b.writeNbt(asCompound(codec.encodeStart(RegistryOps.create(NbtOps.INSTANCE, b.registryAccess()), v).getOrThrow(false, e -> {}))),
                b -> codec.parse(RegistryOps.create(NbtOps.INSTANCE, b.registryAccess()), unwrap(b.readNbt())).getOrThrow(false, e -> {}));
    }

    private static CompoundTag asCompound(Tag tag) {
        if (tag instanceof CompoundTag c) return c;
        CompoundTag wrap = new CompoundTag();
        wrap.put("v", tag);
        return wrap;
    }

    private static Tag unwrap(CompoundTag tag) {
        return tag.size() == 1 && tag.contains("v") ? tag.get("v") : tag;
    }

    @SuppressWarnings("unchecked")
    private static <T> Registry<T> builtIn(ResourceKey<? extends Registry<T>> key) {
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location());
        if (registry == null) throw new IllegalArgumentException("Not a built-in registry: " + key);
        return registry;
    }

    /** By numeric id; the mod only uses built-in registries here (items, blocks, fluids) */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> key) {
        return StreamCodec.of((b, v) -> b.writeId(builtIn(key), v), b -> b.readById(builtIn(key)));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> key) {
        return StreamCodec.of((b, v) -> b.writeId(builtIn(key), v.value()), b -> builtIn(key).wrapAsHolder(b.readById(builtIn(key))));
    }

    public static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> byId, ToIntFunction<T> toId) {
        return StreamCodec.of((b, v) -> friendly(b).writeVarInt(toId.applyAsInt(v)), b -> byId.apply(friendly(b).readVarInt()));
    }

    public static <B extends ByteBuf, V> java.util.function.Function<StreamCodec<? super B, V>, StreamCodec<B, List<V>>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    public static <B extends ByteBuf, V> java.util.function.Function<StreamCodec<? super B, V>, StreamCodec<B, List<V>>> list(int max) {
        return codec -> collection(ArrayList::new, codec, max);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> element) {
        return collection(factory, element, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> element, int max) {
        return StreamCodec.of((b, c) -> {
            if (c.size() > max) throw new IllegalArgumentException(c.size() + " elements, at most " + max);
            friendly(b).writeVarInt(c.size());
            for (V v : c) element.encode(b, v);
        }, b -> {
            int n = friendly(b).readVarInt();
            if (n > max) throw new IllegalArgumentException(n + " elements, at most " + max);
            C c = factory.apply(Math.min(n, 65536));
            for (int i = 0; i < n; i++) c.add(element.decode(b));
            return c;
        });
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory,
                                                                                      StreamCodec<? super B, K> key, StreamCodec<? super B, V> value) {
        return StreamCodec.of((b, m) -> {
            friendly(b).writeVarInt(m.size());
            m.forEach((k, v) -> {
                key.encode(b, k);
                value.encode(b, v);
            });
        }, b -> {
            int n = friendly(b).readVarInt();
            M m = factory.apply(Math.min(n, 65536));
            for (int i = 0; i < n; i++) m.put(key.decode(b), value.decode(b));
            return m;
        });
    }

    /** For callers that only have a {@link RegistryAccess}: wrap a plain buffer */
    public static RegistryFriendlyByteBuf withRegistries(ByteBuf buf, RegistryAccess access) {
        return RegistryFriendlyByteBuf.of(buf, access);
    }
}
