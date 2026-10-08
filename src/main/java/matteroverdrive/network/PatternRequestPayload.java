package matteroverdrive.network;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.ReplicatorBlockEntity.Task;
import matteroverdrive.menu.PatternMonitorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server from the pattern monitor GUI: queue these replications (empty list = just refresh the view).
 * The server re-checks every pattern against its own network.
 */
public record PatternRequestPayload(int containerId, List<Task> requests) implements CustomPacketPayload {
    public static final Type<PatternRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "pattern_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PatternRequestPayload::containerId,
            PatternListPayload.TASK_CODEC.apply(ByteBufCodecs.list(64)), PatternRequestPayload::requests,
            PatternRequestPayload::new);

    @Override
    public Type<PatternRequestPayload> type() {
        return TYPE;
    }

    public static void handle(PatternRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PatternMonitorMenu menu
                && menu.containerId == payload.containerId() && menu.stillValid(player)) {
            if (!payload.requests().isEmpty()) {
                menu.getMachine().request(payload.requests());
            }
            menu.getMachine().sendPatterns(player, menu.containerId);
        }
    }
}
