package matteroverdrive.network;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.ReplicatorBlockEntity.Task;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.menu.PatternMonitorMenu;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import matteroverdrive.platform.PacketContext;

/** Server -> client: the patterns on a pattern monitor's network and its request queue. */
public record PatternListPayload(int containerId, List<ItemPattern> patterns, List<Task> queue) implements CustomPacketPayload {
    public static final Type<PatternListPayload> TYPE =
            new Type<>(new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, "pattern_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Task> TASK_CODEC = StreamCodec.composite(
            ItemPattern.STREAM_CODEC, Task::pattern, ByteBufCodecs.VAR_INT, Task::count, Task::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PatternListPayload::containerId,
            ItemPattern.STREAM_CODEC.apply(ByteBufCodecs.list()), PatternListPayload::patterns,
            TASK_CODEC.apply(ByteBufCodecs.list()), PatternListPayload::queue,
            PatternListPayload::new);

    @Override
    public Type<PatternListPayload> type() {
        return TYPE;
    }

    public static void handle(PatternListPayload payload, PacketContext context) {
        if (context.player().containerMenu instanceof PatternMonitorMenu menu && menu.containerId == payload.containerId()) {
            menu.setClientPatterns(payload.patterns(), payload.queue());
        }
    }
}
