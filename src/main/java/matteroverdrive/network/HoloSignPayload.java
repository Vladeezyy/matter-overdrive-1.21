package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.HoloSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the edited text of a holo sign (1.7.10 GuiHoloSign sent the text with its config packet). */
public record HoloSignPayload(BlockPos pos, String text) implements CustomPacketPayload {
    public static final Type<HoloSignPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "holo_sign"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloSignPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HoloSignPayload::pos, ByteBufCodecs.stringUtf8(HoloSignBlockEntity.MAX_LENGTH), HoloSignPayload::text, HoloSignPayload::new);

    @Override
    public Type<HoloSignPayload> type() {
        return TYPE;
    }

    public static void handle(HoloSignPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.canInteractWithBlock(payload.pos(), 4)
                && player.level().getBlockEntity(payload.pos()) instanceof HoloSignBlockEntity sign) {
            sign.setText(payload.text());
        }
    }
}
