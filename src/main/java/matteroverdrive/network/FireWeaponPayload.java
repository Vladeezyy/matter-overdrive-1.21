package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server while the attack key is held with an energy weapon: fire if the weapon allows it. */
public record FireWeaponPayload(boolean zoomed) implements CustomPacketPayload {
    public static final Type<FireWeaponPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "fire_weapon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireWeaponPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FireWeaponPayload::zoomed, FireWeaponPayload::new);

    @Override
    public Type<FireWeaponPayload> type() {
        return TYPE;
    }

    public static void handle(FireWeaponPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()
                && player.getMainHandItem().getItem() instanceof EnergyWeaponItem weapon) {
            weapon.tryFire(player, player.getMainHandItem(), payload.zoomed() && player.isUsingItem());
        }
    }
}
