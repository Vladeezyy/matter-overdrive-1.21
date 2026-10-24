package matteroverdrive.compat.codec;

import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;

/**
 * 1.20.1 stand-in for the 1.20.5+ {@code net.minecraft.network.codec.StreamCodec}: the subset of its API the mod uses,
 * so the payloads, recipes and components keep one form on every Minecraft version.
 */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return of((buf, value) -> encoder.encode(value, buf), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(V instance) {
        return of((buf, value) -> {
            if (!value.equals(instance)) throw new IllegalStateException("Can't encode " + value + ", expected " + instance);
        }, buf -> instance);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        StreamCodec<B, V> self = this;
        return of((buf, value) -> self.encode(buf, from.apply(value)), buf -> to.apply(self.decode(buf)));
    }

    default <O> O apply(Function<? super StreamCodec<B, V>, O> operation) {
        return operation.apply(this);
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    static <B, V> StreamCodec<B, V> recursive(Function<StreamCodec<B, V>, StreamCodec<B, V>> modifier) {
        return new StreamCodec<>() {
            private final Supplier<StreamCodec<B, V>> inner = com.google.common.base.Suppliers.memoize(() -> modifier.apply(this));

            @Override
            public V decode(B buf) {
                return inner.get().decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                inner.get().encode(buf, value);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> f) {
        return of((buf, v) -> c1.encode(buf, g1.apply(v)), buf -> f.apply(c1.decode(buf)));
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                      Function<C, T2> g2, java.util.function.BiFunction<T1, T2, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
        }, buf -> f.apply(c1.decode(buf), c2.decode(buf)));
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                          Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                          Function3<T1, T2, T3, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            return f.apply(a, b, c3.decode(buf));
        });
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                              StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                              StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                              StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                              Function4<T1, T2, T3, T4, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            return f.apply(a, b, c, c4.decode(buf));
        });
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                                  StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                  StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                  StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                  StreamCodec<? super B, T5> c5, Function<C, T5> g5,
                                                                  Function5<T1, T2, T3, T4, T5, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            return f.apply(a, b, c, d, c5.decode(buf));
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                                      StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                      StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                      StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                      StreamCodec<? super B, T5> c5, Function<C, T5> g5,
                                                                      StreamCodec<? super B, T6> c6, Function<C, T6> g6,
                                                                      Function6<T1, T2, T3, T4, T5, T6, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
            c6.encode(buf, g6.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            T5 e = c5.decode(buf);
            return f.apply(a, b, c, d, e, c6.decode(buf));
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                                          StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                          StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                          StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                          StreamCodec<? super B, T5> c5, Function<C, T5> g5,
                                                                          StreamCodec<? super B, T6> c6, Function<C, T6> g6,
                                                                          StreamCodec<? super B, T7> c7, Function<C, T7> g7,
                                                                          Function7<T1, T2, T3, T4, T5, T6, T7, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
            c6.encode(buf, g6.apply(v));
            c7.encode(buf, g7.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            T5 e = c5.decode(buf);
            T6 g = c6.decode(buf);
            return f.apply(a, b, c, d, e, g, c7.decode(buf));
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                                              StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                              StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                              StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                              StreamCodec<? super B, T5> c5, Function<C, T5> g5,
                                                                              StreamCodec<? super B, T6> c6, Function<C, T6> g6,
                                                                              StreamCodec<? super B, T7> c7, Function<C, T7> g7,
                                                                              StreamCodec<? super B, T8> c8, Function<C, T8> g8,
                                                                              Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> f) {
        return of((buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
            c6.encode(buf, g6.apply(v));
            c7.encode(buf, g7.apply(v));
            c8.encode(buf, g8.apply(v));
        }, buf -> {
            T1 a = c1.decode(buf);
            T2 b = c2.decode(buf);
            T3 c = c3.decode(buf);
            T4 d = c4.decode(buf);
            T5 e = c5.decode(buf);
            T6 g = c6.decode(buf);
            T7 h = c7.decode(buf);
            return f.apply(a, b, c, d, e, g, h, c8.decode(buf));
        });
    }
}
