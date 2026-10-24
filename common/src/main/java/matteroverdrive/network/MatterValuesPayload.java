package matteroverdrive.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.core.registries.Registries;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import matteroverdrive.platform.PacketContext;

/** Server -> client: the full matter table (base + calculated, and which are estimates), sent on join and after every datapack reload. */
public record MatterValuesPayload(Map<Item, Integer> values, Set<Item> estimated) implements CustomPacketPayload {
    public static final Type<MatterValuesPayload> TYPE =
            new Type<>(new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, "matter_values"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MatterValuesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.VAR_INT),
            MatterValuesPayload::values,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.ITEM)), MatterValuesPayload::estimated,
            MatterValuesPayload::new);

    @Override
    public Type<MatterValuesPayload> type() {
        return TYPE;
    }

    public static void handle(MatterValuesPayload payload, PacketContext context) {
        MatterRegistry.setClientValues(payload.values(), payload.estimated());
    }
}
