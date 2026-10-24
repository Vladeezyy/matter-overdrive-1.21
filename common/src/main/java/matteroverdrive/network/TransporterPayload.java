package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.TransporterBlockEntity;
import matteroverdrive.menu.TransporterMenu;
import net.minecraft.core.BlockPos;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import matteroverdrive.platform.PacketContext;

/**
 * Client -> server from the transporter screen (1.7.10 sendConfigsToServer): select / add / remove / edit / import /
 * reset destinations. Edits are clamped to the transporter's range like 1.7.10's integer fields.
 */
public record TransporterPayload(int containerId, Action action, int index, String name, BlockPos pos) implements CustomPacketPayload {
    public enum Action { SELECT, NEW, REMOVE, SET, IMPORT, RESET }

    public static final Type<TransporterPayload> TYPE = new Type<>(new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, "transporter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransporterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransporterPayload::containerId,
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), TransporterPayload::action,
            ByteBufCodecs.VAR_INT, TransporterPayload::index,
            ByteBufCodecs.stringUtf8(64), TransporterPayload::name,
            matteroverdrive.compat.codec.VanillaCodecs.BLOCK_POS, TransporterPayload::pos, TransporterPayload::new);

    @Override
    public Type<TransporterPayload> type() {
        return TYPE;
    }

    public static void handle(TransporterPayload payload, PacketContext context) {
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
                    net.minecraft.util.Mth.clamp(payload.pos().getX(), me.getX() - r, me.getX() + r),
                    net.minecraft.util.Mth.clamp(payload.pos().getY(), me.getY() - r, me.getY() + r),
                    net.minecraft.util.Mth.clamp(payload.pos().getZ(), me.getZ() - r, me.getZ() + r)));
            case IMPORT -> t.importFromFlashDrive(payload.name());
            case RESET -> t.setSelected(t.getSelected().name(), me);
        }
    }
}
