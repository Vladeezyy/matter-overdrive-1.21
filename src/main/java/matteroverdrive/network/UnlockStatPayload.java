package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.android.BioticStat;
import matteroverdrive.android.BioticStats;
import matteroverdrive.init.MOSounds;
import matteroverdrive.menu.AndroidStationMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: 1.7.10 PacketUnlockBioticStat, sent by clicking a stat in the android station. */
public record UnlockStatPayload(String stat, int level) implements CustomPacketPayload {
    public static final Type<UnlockStatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "unlock_stat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockStatPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UnlockStatPayload::stat, ByteBufCodecs.VAR_INT, UnlockStatPayload::level, UnlockStatPayload::new);

    @Override
    public Type<UnlockStatPayload> type() {
        return TYPE;
    }

    public static void handle(UnlockStatPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof AndroidStationMenu)) return;
        BioticStat stat = BioticStats.get(payload.stat());
        if (stat == null) return;
        int level = Android.get(player).getUnlockedLevel(stat) + 1;
        if (level != payload.level() || level > stat.maxLevel()) return;
        if (Android.tryUnlock(player, stat, level)) {
            player.level().playSound(null, player.blockPosition(), MOSounds.BIOTIC_STAT_UNLOCK.get(), SoundSource.PLAYERS, 1, 1);
        }
    }
}
