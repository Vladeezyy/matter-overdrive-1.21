package matteroverdrive.dialog;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** 1.7.10 IDialogNpc (+ IDialogQuestGiver): an entity a player can have a conversation with. */
public interface DialogNpc {
    /** The first message for the current dialog player (built per player in {@link #setDialogPlayer}). */
    @Nullable DialogMessage getStartDialogMessage(Player player);

    void setDialogPlayer(@Nullable Player player);

    @Nullable Player getDialogPlayer();

    boolean canTalkTo(Player player);

    Mob getEntity();

    /** Server: a message became active (null when the conversation starts). */
    void onPlayerInteract(Player player, @Nullable DialogMessage message);

    /** 1.7.10 IDialogQuestGiver.giveQuest. */
    default void giveQuest(DialogMessage message, matteroverdrive.quest.QuestStack stack, Player player) {}
}
