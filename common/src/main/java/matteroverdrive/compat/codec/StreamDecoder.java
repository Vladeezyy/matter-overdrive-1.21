package matteroverdrive.compat.codec;

/** 1.20.1 stand-in for {@code net.minecraft.network.codec.StreamDecoder}. */
@FunctionalInterface
public interface StreamDecoder<I, T> {
    T decode(I buf);
}
