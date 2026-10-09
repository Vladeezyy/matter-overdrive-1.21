package matteroverdrive.dialog;

import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 1.7.10 PacketManageConversation (both ways) and PacketConversationInteract (client to server). */
public final class DialogPayloads {
    /** Client: open the dialog screen for an NPC (set by the client setup). */
    public static Consumer<DialogNpc> openScreen = npc -> {};

    public record Manage(int npcId, boolean start) implements CustomPacketPayload {
        public static final Type<Manage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "dialog_manage"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Manage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Manage::npcId, ByteBufCodecs.BOOL, Manage::start, Manage::new);

        @Override
        public Type<Manage> type() {
            return TYPE;
        }
    }

    public record Interact(int npcId, int messageId, int option) implements CustomPacketPayload {
        public static final Type<Interact> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "dialog_interact"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Interact> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Interact::npcId, ByteBufCodecs.VAR_INT, Interact::messageId, ByteBufCodecs.VAR_INT, Interact::option,
                Interact::new);

        @Override
        public Type<Interact> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(Manage.TYPE, Manage.STREAM_CODEC, DialogPayloads::handleManage)
                .playToServer(Interact.TYPE, Interact.STREAM_CODEC, DialogPayloads::handleInteract);
    }

    /** Server: start a conversation (1.7.10 handleServerMessage start). */
    public static boolean startConversation(ServerPlayer player, DialogNpc npc) {
        if (npc.getDialogPlayer() != null || !npc.canTalkTo(player)) return false;
        npc.setDialogPlayer(player);
        npc.onPlayerInteract(player, null);
        if (player.connection.hasChannel(Manage.TYPE)) PacketDistributor.sendToPlayer(player, new Manage(npc.getEntity().getId(), true));
        return true;
    }

    /** Server: end the conversation (the player walked away). */
    public static void endConversation(DialogNpc npc) {
        if (npc.getDialogPlayer() instanceof ServerPlayer player && player.connection.hasChannel(Manage.TYPE)) {
            PacketDistributor.sendToPlayer(player, new Manage(npc.getEntity().getId(), false));
        }
        npc.setDialogPlayer(null);
    }

    private static void handleManage(Manage payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.level().getEntity(payload.npcId()) instanceof DialogNpc npc)) return;
        if (player instanceof ServerPlayer server) {
            if (payload.start()) {
                if (server.distanceToSqr(npc.getEntity()) <= 64) startConversation(server, npc);
            } else if (npc.getDialogPlayer() == player) {
                npc.setDialogPlayer(null);
            }
        } else if (payload.start()) {
            npc.setDialogPlayer(player);
            openScreen.accept(npc);
        } else {
            npc.setDialogPlayer(null);
            DialogMessage.showOnClient.accept(null);
        }
    }

    /** 1.7.10 PacketConversationInteract: the chosen option of a message (id -1 = the NPC's start message). */
    private static void handleInteract(Interact payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level().getEntity(payload.npcId()) instanceof DialogNpc npc)
                || npc.getDialogPlayer() != player) return;
        DialogMessage message = payload.messageId() >= 0 ? DialogRegistry.get(payload.messageId()) : npc.getStartDialogMessage(player);
        if (message != null) message.onOptionsInteract(npc, player, payload.option());
    }

    private DialogPayloads() {}
}
