package matteroverdrive.compat.codec;

/** 1.20.1 stand-in for {@code net.minecraft.network.codec.StreamMemberEncoder}. */
@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T value, O buf);
}
