package matteroverdrive.network;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.AndroidSpawnerBlockEntity;
import matteroverdrive.menu.AndroidSpawnerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server from the android spawner's config page (1.7.10 sendConfigsToServer); values clamped server-side. */
public record AndroidSpawnerPayload(int containerId, int maxSpawnAmount, int spawnRange, int spawnDelay, String team)
        implements CustomPacketPayload {
    public static final Type<AndroidSpawnerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_spawner"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AndroidSpawnerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AndroidSpawnerPayload::containerId,
            ByteBufCodecs.VAR_INT, AndroidSpawnerPayload::maxSpawnAmount,
            ByteBufCodecs.VAR_INT, AndroidSpawnerPayload::spawnRange,
            ByteBufCodecs.VAR_INT, AndroidSpawnerPayload::spawnDelay,
            ByteBufCodecs.stringUtf8(AndroidSpawnerBlockEntity.MAX_TEAM_LENGTH), AndroidSpawnerPayload::team, AndroidSpawnerPayload::new);

    @Override
    public Type<AndroidSpawnerPayload> type() {
        return TYPE;
    }

    public static void handle(AndroidSpawnerPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof AndroidSpawnerMenu menu)
                || menu.containerId != payload.containerId() || !menu.stillValid(player)) return;
        menu.getMachine().setConfig(payload.maxSpawnAmount(), payload.spawnRange(), payload.spawnDelay(), payload.team());
    }
}
