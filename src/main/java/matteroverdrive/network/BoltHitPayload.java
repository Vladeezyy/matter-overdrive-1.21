package matteroverdrive.network;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import matteroverdrive.MatterOverdrive;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> clients: a plasma bolt hit something (1.7.10 PlasmaBolt.onHit ran on the client, which saw the hit itself;
 * here only the server does). Kind: 0 block, 1 entity, 2 living entity.
 */
public record BoltHitPayload(float x, float y, float z, float nx, float ny, float nz, int color, float size, int kind) implements CustomPacketPayload {
    public static final Type<BoltHitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "bolt_hit"));
    public static final StreamCodec<ByteBuf, BoltHitPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BoltHitPayload decode(ByteBuf buf) {
            return new BoltHitPayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readInt(), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, BoltHitPayload p) {
            buf.writeFloat(p.x).writeFloat(p.y).writeFloat(p.z).writeFloat(p.nx).writeFloat(p.ny).writeFloat(p.nz).writeInt(p.color).writeFloat(p.size);
            ByteBufCodecs.VAR_INT.encode(buf, p.kind);
        }
    };
    public static final int BLOCK = 0, ENTITY = 1, LIVING = 2;

    /** Set by the client: the hit effects. */
    public static Consumer<BoltHitPayload> onClient = hit -> {};

    @Override
    public Type<BoltHitPayload> type() {
        return TYPE;
    }

    public static void handle(BoltHitPayload payload, IPayloadContext context) {
        onClient.accept(payload);
    }

    /** Sends the hit to the players near enough to see it. */
    public static void send(ServerLevel level, Vec3 at, Vec3 normal, int color, float size, int kind) {
        BoltHitPayload hit = new BoltHitPayload((float) at.x, (float) at.y, (float) at.z, (float) normal.x, (float) normal.y, (float) normal.z,
                color, size, kind);
        for (var player : level.players()) {
            if (player.distanceToSqr(at) < 64 * 64 && player.connection.hasChannel(TYPE)) PacketDistributor.sendToPlayer(player, hit);
        }
    }
}
