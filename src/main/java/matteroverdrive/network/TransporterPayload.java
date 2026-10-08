package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.TransporterBlockEntity;
import matteroverdrive.menu.TransporterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server from the transporter screen (1.7.10 sendConfigsToServer): select / add / remove / edit / import /
 * reset destinations. Edits are clamped to the transporter's range like 1.7.10's integer fields.
 */
public record TransporterPayload(int containerId, Action action, int index, String name, BlockPos pos) implements CustomPacketPayload {
    public enum Action { SELECT, NEW, REMOVE, SET, IMPORT, RESET }

    public static final Type<TransporterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "transporter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransporterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransporterPayload::containerId,
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), TransporterPayload::action,
            ByteBufCodecs.VAR_INT, TransporterPayload::index,
            ByteBufCodecs.stringUtf8(64), TransporterPayload::name,
            BlockPos.STREAM_CODEC, TransporterPayload::pos, TransporterPayload::new);

    @Override
    public Type<TransporterPayload> type() {
        return TYPE;
    }

    public static void handle(TransporterPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof TransporterMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player)) return;
        TransporterBlockEntity t = menu.getMachine();
        BlockPos me = t.getBlockPos();
        int r = t.getRange();
        switch (payload.action()) {
            case SELECT -> t.select(payload.index());
            case NEW -> t.addLocation(payload.name());
            case REMOVE -> t.removeLocation(payload.index());
            case SET -> t.setSelected(payload.name(), new BlockPos(
                    Math.clamp(payload.pos().getX(), me.getX() - r, me.getX() + r),
                    Math.clamp(payload.pos().getY(), me.getY() - r, me.getY() + r),
                    Math.clamp(payload.pos().getZ(), me.getZ() - r, me.getZ() + r)));
            case IMPORT -> t.importFromFlashDrive(payload.name());
            case RESET -> t.setSelected(t.getSelected().name(), me);
        }
    }
}
