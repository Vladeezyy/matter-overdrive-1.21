package matteroverdrive.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.StreamCodec;

/** 1.7.10 TransportLocation: a named destination of a transporter. */
public record TransportLocation(String name, BlockPos pos) {
    public static final int MAX_NAME = 32;
    public static final Codec<TransportLocation> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(TransportLocation::name),
            BlockPos.CODEC.fieldOf("pos").forGetter(TransportLocation::pos)).apply(i, TransportLocation::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransportLocation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME), TransportLocation::name, matteroverdrive.compat.codec.VanillaCodecs.BLOCK_POS, TransportLocation::pos, TransportLocation::new);

    public double distanceTo(BlockPos other) {
        return Math.sqrt(pos.distSqr(other));
    }
}
