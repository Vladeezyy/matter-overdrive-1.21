package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.BioticStat;
import matteroverdrive.android.BioticStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Client -> server android packets: 1.7.10 PacketBioticActionKey, PacketAndroidChangeAbility, PacketTeleportPlayer. */
public final class AndroidPayloads {
    public record Action() implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.unit(new Action());

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    public record SelectStat(String stat) implements CustomPacketPayload {
        public static final Type<SelectStat> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectStat> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SelectStat::stat, SelectStat::new);

        @Override
        public Type<SelectStat> type() {
            return TYPE;
        }
    }

    public record Teleport(double x, double y, double z) implements CustomPacketPayload {
        public static final Type<Teleport> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_teleport"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Teleport> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Teleport::x, ByteBufCodecs.DOUBLE, Teleport::y, ByteBufCodecs.DOUBLE, Teleport::z, Teleport::new);

        @Override
        public Type<Teleport> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) Android.onActionKey(player);
        });
        registrar.playToServer(SelectStat.TYPE, SelectStat.STREAM_CODEC, (payload, context) -> {
            BioticStat stat = BioticStats.get(payload.stat());
            if (context.player() instanceof ServerPlayer player && stat != null && stat.showOnWheel()
                    && Android.get(player).isUnlocked(stat, 0)) {
                Android.get(player).setActiveStat(stat.id());
                Android.sync(player);
            }
        });
        registrar.playToServer(Teleport.TYPE, Teleport.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) Android.teleport(player, new Vec3(payload.x(), payload.y(), payload.z()));
        });
    }

    private AndroidPayloads() {}
}
