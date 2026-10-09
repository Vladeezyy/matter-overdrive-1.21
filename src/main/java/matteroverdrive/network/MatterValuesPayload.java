package matteroverdrive.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the full matter table (base + calculated, and which are estimates), sent on join and after every datapack reload. */
public record MatterValuesPayload(Map<Item, Integer> values, Set<Item> estimated) implements CustomPacketPayload {
    public static final Type<MatterValuesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "matter_values"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MatterValuesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.VAR_INT),
            MatterValuesPayload::values,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.ITEM)), MatterValuesPayload::estimated,
            MatterValuesPayload::new);

    @Override
    public Type<MatterValuesPayload> type() {
        return TYPE;
    }

    public static void handle(MatterValuesPayload payload, IPayloadContext context) {
        MatterRegistry.setClientValues(payload.values(), payload.estimated());
    }
}
