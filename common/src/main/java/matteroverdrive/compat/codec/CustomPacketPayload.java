package matteroverdrive.compat.codec;

import net.minecraft.resources.ResourceLocation;

/** 1.20.1 stand-in for the 1.20.5+ {@code CustomPacketPayload}: a packet with a type id, sent by the loader's networking. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    static <T extends CustomPacketPayload> Type<T> createType(String id) {
        return new Type<>(new ResourceLocation(id));
    }

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {}

    record TypeAndCodec<B extends io.netty.buffer.ByteBuf, T extends CustomPacketPayload>(Type<T> type, StreamCodec<B, T> codec) {}
}
