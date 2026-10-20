package matteroverdrive.dialog;

import matteroverdrive.android.Android;
import matteroverdrive.quest.PlayerQuests;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;

/** The 1.7.10 DialogMessage subclasses. */
public final class DialogMessages {
    /** 1.7.10 DialogMessageQuit: closes the conversation. */
    public static class Quit extends DialogMessage {
        @Override
        public void onInteract(DialogNpc npc, Player player) {
            if (player instanceof ServerPlayer server) server.closeContainer();
            else showOnClient.accept(null);
        }
    }

    /** 1.7.10 DialogMessageBack: back to the parent of the message on screen. */
    public static class Back extends DialogMessage {
        @Override
        protected DialogMessage nextShown(DialogNpc npc, Player player) {
            DialogMessage current = shownOnClient.get();
            return current != null && current.getParent() != null ? current.getParent() : current;
        }
    }

    /** 1.7.10 DialogMessageBackToMain. */
    public static class BackToMain extends DialogMessage {
        @Override
        protected DialogMessage nextShown(DialogNpc npc, Player player) {
            return npc.getStartDialogMessage(player);
        }
    }

    /** 1.7.10 DialogMessageTrade: opens the NPC's trading screen. */
    public static class Trade extends DialogMessage {
        @Override
        public void onInteract(DialogNpc npc, Player player) {
            if (!player.level().isClientSide() && npc.getEntity() instanceof Merchant merchant) {
                merchant.setTradingPlayer(player);
                merchant.openTradingScreen(player, npc.getEntity().getDisplayName(), 1);
            }
        }
    }

    /** 1.7.10 DialogMessageAndroidOnly. */
    public static class AndroidOnly extends DialogMessage {
        @Override
        public boolean isVisible(DialogNpc npc, Player player) {
            return Android.isAndroid(player);
        }
    }

    /** 1.7.10 DialogMessageQuestGive: the NPC gives the quest; optionally the screen returns to the start message. */
    public static class QuestGive extends DialogMessage {
        private final Quest quest;
        private boolean returnToMain;

        public QuestGive(Quest quest) {
            this.quest = quest;
        }

        public QuestGive returnToMain() {
            returnToMain = true;
            return this;
        }

        @Override
        public void onInteract(DialogNpc npc, Player player) {
            super.onInteract(npc, player);
            if (!player.level().isClientSide()) npc.giveQuest(this, new QuestStack(quest), player);
        }

        @Override
        protected DialogMessage nextShown(DialogNpc npc, Player player) {
            return returnToMain ? npc.getStartDialogMessage(player) : this;
        }
    }

    /** 1.7.10 DialogMessageQuestStart: only shown while the quest can be accepted. */
    public static class QuestStart extends DialogMessage {
        private Quest quest;

        public QuestStart setQuest(Quest quest) {
            this.quest = quest;
            return this;
        }

        @Override
        public boolean isVisible(DialogNpc npc, Player player) {
            return quest != null && quest.canBeAccepted(new QuestStack(quest), player);
        }
    }

    /** 1.7.10 DialogMessageQuestOnObjectivesCompleted: shown when the active quest has these objectives done. */
    public static class QuestOnObjectivesCompleted extends DialogMessage {
        private final Quest quest;
        private final int[] objectives;

        public QuestOnObjectivesCompleted(Quest quest, int... objectives) {
            this.quest = quest;
            this.objectives = objectives;
        }

        @Override
        public boolean isVisible(DialogNpc npc, Player player) {
            QuestStack stack = PlayerQuests.get(player).findActive(quest);
            if (stack == null) return false;
            for (int objective : objectives) {
                if (!stack.isObjectiveCompleted(player, objective)) return false;
            }
            return true;
        }
    }

    private DialogMessages() {}
}
