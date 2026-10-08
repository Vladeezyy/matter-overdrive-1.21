package matteroverdrive.quest;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 GenericQuest: a quest with one logic, an XP reward and item rewards. Text comes from
 * {@code quest.matteroverdrive.<id>.title/info/objective.N}; it is translated where it is shown (the client).
 */
public class Quest {
    private final String id;
    private final QuestLogic logic;
    private final int xp;
    private final List<java.util.function.Supplier<ItemStack>> rewards = new ArrayList<>();

    public Quest(String id, QuestLogic logic, int xp) {
        this.id = id;
        this.logic = logic;
        this.xp = xp;
    }

    /** Item rewards, made when given (items aren't registered yet when quests are defined). */
    @SafeVarargs
    public final Quest rewards(java.util.function.Supplier<ItemStack>... stacks) {
        rewards.addAll(List.of(stacks));
        return this;
    }

    public String id() {
        return id;
    }

    public QuestLogic logic() {
        return logic;
    }

    /** 1.7.10 GenericQuest.canBeAccepted: the logic allows it and the player hasn't got or done it. */
    public boolean canBeAccepted(QuestStack stack, Player player) {
        PlayerQuests quests = PlayerQuests.get(player);
        return logic.canAccept(stack, player) && !quests.hasCompletedQuest(stack) && !quests.hasQuest(stack);
    }

    public boolean areQuestStacksEqual(QuestStack a, QuestStack b) {
        return a.getQuestId().equals(b.getQuestId()) && logic.areQuestStacksEqual(a, b);
    }

    public String key(String part) {
        return "quest." + MatterOverdrive.MODID + "." + id + "." + part;
    }

    private static String translate(String key, Player player) {
        // 1.7.10 replaceVariables: %1$s is the player's name
        String text = Language.getInstance().getOrDefault(key);
        return player == null ? text : text.replace("%1$s", player.getName().getString());
    }

    public String getTitle(QuestStack stack, Player player) {
        return logic.modifyTitle(stack, translate(key("title"), player));
    }

    public String getInfo(QuestStack stack, Player player) {
        return logic.modifyInfo(stack, translate(key("info"), player));
    }

    public String getObjective(QuestStack stack, Player player, int index) {
        return logic.modifyObjective(stack, player, translate(key("objective." + index), player), index);
    }

    public int getObjectivesCount(QuestStack stack, Player player) {
        return logic.modifyObjectiveCount(stack, player, 1);
    }

    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return logic.isObjectiveCompleted(stack, player, index);
    }

    public int getXpReward(QuestStack stack, Player player) {
        return logic.modifyXP(stack, player, xp);
    }

    public List<ItemStack> getRewards(QuestStack stack, Player player) {
        List<ItemStack> list = new ArrayList<>();
        for (var s : rewards) list.add(s.get());
        logic.modifyRewards(stack, player, list);
        return list;
    }

    /** 1.7.10 Quest.setCompleted. */
    public void setCompleted(QuestStack stack, Player player) {
        stack.completed = true;
    }
}
