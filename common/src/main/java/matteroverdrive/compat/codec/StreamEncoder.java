package matteroverdrive.compat.codec;

/** 1.20.1 stand-in for {@code net.minecraft.network.codec.StreamEncoder}. */
@FunctionalInterface
public interface StreamEncoder<O, T> {
    void encode(O buf, T value);
}
