package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.HoloSignBlockEntity;
import net.minecraft.core.BlockPos;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import matteroverdrive.platform.PacketContext;

/** Client -> server: the edited text of a holo sign (1.7.10 GuiHoloSign sent the text with its config packet). */
public record HoloSignPayload(BlockPos pos, String text) implements CustomPacketPayload {
    public static final Type<HoloSignPayload> TYPE = new Type<>(new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, "holo_sign"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloSignPayload> STREAM_CODEC = StreamCodec.composite(
            matteroverdrive.compat.codec.VanillaCodecs.BLOCK_POS, HoloSignPayload::pos, ByteBufCodecs.stringUtf8(HoloSignBlockEntity.MAX_LENGTH), HoloSignPayload::text, HoloSignPayload::new);

    @Override
    public Type<HoloSignPayload> type() {
        return TYPE;
    }

    public static void handle(HoloSignPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player && (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos())) <= (4 + 4.5) * (4 + 4.5))
                && player.level().getBlockEntity(payload.pos()) instanceof HoloSignBlockEntity sign) {
            sign.setText(payload.text());
        }
    }
}
