package matteroverdrive.quest;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 IQuestLogic / AbstractQuestLogic: what a quest asks for and how it reacts to events. */
public interface QuestLogic {
    default String modifyTitle(QuestStack stack, String title) {
        return title;
    }

    default String modifyInfo(QuestStack stack, String info) {
        return info;
    }

    default String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        return objective;
    }

    default int modifyObjectiveCount(QuestStack stack, Player player, int count) {
        return count;
    }

    default int modifyXP(QuestStack stack, Player player, int xp) {
        return xp;
    }

    default void modifyRewards(QuestStack stack, Player player, List<ItemStack> rewards) {}

    default boolean canAccept(QuestStack stack, Player player) {
        return true;
    }

    default boolean areQuestStacksEqual(QuestStack a, QuestStack b) {
        return true;
    }

    boolean isObjectiveCompleted(QuestStack stack, Player player, int index);

    default void initQuestStack(RandomSource random, QuestStack stack) {}

    /** Server side: an event (a NeoForge event or a {@link QuestEvents.DialogInteract}); true when the stack changed. */
    default boolean onEvent(QuestStack stack, Object event, Player player) {
        return false;
    }

    default void onCompleted(QuestStack stack, Player player) {}

    /** 1.7.10 AbstractQuestLogic.random: min + nextInt(max - min). */
    static int random(RandomSource random, int min, int max) {
        int range = max - min;
        return min + (range > 0 ? random.nextInt(range) : 0);
    }

    /** 1.7.10 String.format(text, "", a, b, c): fills %2$s, %3$s, ... (%1$s, the player, is already replaced). */
    static String fmt(String text, Object... args) {
        for (int i = 0; i < args.length; i++) text = text.replace("%" + (i + 2) + "$s", String.valueOf(args[i]));
        return text;
    }

    /** 1.7.10 getTag with a null id: the stack's whole data tag. */
    static CompoundTag tag(QuestStack stack) {
        return stack.getData();
    }
}
