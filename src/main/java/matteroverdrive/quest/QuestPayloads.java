package matteroverdrive.quest;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.item.DataPadItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 1.7.10 PacketQuestActions (complete / abandon an active quest) and PacketDataPadCommands (the pad's screen state). */
public final class QuestPayloads {
    public enum Action { COMPLETE, ABANDON }

    public record QuestAction(Action action, int index) implements CustomPacketPayload {
        public static final Type<QuestAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "quest_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, QuestAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), QuestAction::action, ByteBufCodecs.VAR_INT, QuestAction::index,
                QuestAction::new);

        @Override
        public Type<QuestAction> type() {
            return TYPE;
        }
    }

    public record DataPadState(boolean offhand, DataPadItem.State state) implements CustomPacketPayload {
        public static final Type<DataPadState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "data_pad_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DataPadState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, DataPadState::offhand, DataPadItem.State.STREAM_CODEC, DataPadState::state, DataPadState::new);

        @Override
        public Type<DataPadState> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(QuestAction.TYPE, QuestAction.STREAM_CODEC, QuestPayloads::handleAction)
                .playToServer(DataPadState.TYPE, DataPadState.STREAM_CODEC, QuestPayloads::handleState);
    }

    /** 1.7.10: complete when every objective is done (forced), abandon removes it. */
    public static void apply(ServerPlayer player, Action action, int index) {
        PlayerQuests quests = PlayerQuests.get(player);
        if (index < 0 || index >= quests.active.size()) return;
        QuestStack stack = quests.active.get(index);
        if (action == Action.COMPLETE) {
            for (int i = 0; i < stack.getObjectivesCount(player); i++) {
                if (!stack.isObjectiveCompleted(player, i)) return;
            }
            stack.markCompleted(player, true);
            QuestEvents.manageQuestCompletion(player);
        } else {
            quests.active.remove(index);
            PlayerQuests.sync(player);
        }
    }

    private static void handleAction(QuestAction payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) apply(player, payload.action(), payload.index());
    }

    private static void handleState(DataPadState payload, IPayloadContext context) {
        ItemStack pad = context.player().getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (pad.getItem() instanceof DataPadItem) pad.set(MODataComponents.DATA_PAD.get(), payload.state());
    }

    private QuestPayloads() {}
}
