package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import matteroverdrive.compat.codec.ByteBufCodecs;
import matteroverdrive.compat.codec.StreamCodec;
import matteroverdrive.compat.codec.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import matteroverdrive.platform.PacketContext;

/** Client -> server while the attack key is held with an energy weapon: fire if the weapon allows it. */
public record FireWeaponPayload(boolean zoomed) implements CustomPacketPayload {
    public static final Type<FireWeaponPayload> TYPE = new Type<>(new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, "fire_weapon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireWeaponPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FireWeaponPayload::zoomed, FireWeaponPayload::new);

    @Override
    public Type<FireWeaponPayload> type() {
        return TYPE;
    }

    public static void handle(FireWeaponPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()
                && player.getMainHandItem().getItem() instanceof EnergyWeaponItem weapon) {
            weapon.tryFire(player, player.getMainHandItem(), payload.zoomed() && player.isUsingItem());
        }
    }
}
