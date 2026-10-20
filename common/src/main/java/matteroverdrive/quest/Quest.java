package matteroverdrive.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import matteroverdrive.MatterOverdrive;
import net.minecraft.locale.Language;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 GenericQuest: a quest with one logic, an XP reward and item rewards. Text comes from
 * {@code quest.matteroverdrive.<id>.title/info/objective.N}; it is translated where it is shown (the client).
 */
public class Quest {
    protected final String id;
    private final QuestLogic logic;
    protected final int xp;
    protected final List<Supplier<ItemStack>> rewards = new ArrayList<>();
    protected final List<QuestStackReward> questRewards = new ArrayList<>();

    /** 1.7.10 QuestStackReward: another quest given on completion, copying some of this one's data. */
    public record QuestStackReward(Supplier<Quest> quest, List<String> copyData) {}

    public Quest(String id, QuestLogic logic, int xp) {
        this.id = id;
        this.logic = logic;
        this.xp = xp;
    }

    /** Item rewards, made when given (items aren't registered yet when quests are defined). */
    @SafeVarargs
    public final Quest rewards(Supplier<ItemStack>... stacks) {
        rewards.addAll(List.of(stacks));
        return this;
    }

    public Quest questReward(Supplier<Quest> quest, String... copyData) {
        questRewards.add(new QuestStackReward(quest, List.of(copyData)));
        return this;
    }

    public List<QuestStackReward> getQuestRewards() {
        return questRewards;
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

    /** 1.7.10 QuestFactory.generateQuestStack: a new stack set up by the logic (contracts). */
    public QuestStack generate(RandomSource random) {
        QuestStack stack = new QuestStack(this);
        initQuestStack(random, stack);
        return stack;
    }

    public void initQuestStack(RandomSource random, QuestStack stack) {
        logic.initQuestStack(random, stack);
    }

    /** 1.7.10 initQuestStack(random, stack, player): when a player takes the quest. */
    public void onTaken(RandomSource random, QuestStack stack, Player player) {}

    public String key(QuestStack stack, String part) {
        return "quest." + MatterOverdrive.MODID + "." + id + "." + part;
    }

    /** The title's translation key (contract names). */
    public String titleKey(QuestStack stack) {
        return key(stack, "title");
    }

    protected static String translate(String key, Player player) {
        // 1.7.10 replaceVariables: %1$s is the player's name
        String text = Language.getInstance().getOrDefault(key);
        return player == null ? text : text.replace("%1$s", player.getName().getString());
    }

    public String getTitle(QuestStack stack, Player player) {
        return logic.modifyTitle(stack, translate(key(stack, "title"), player));
    }

    public String getInfo(QuestStack stack, Player player) {
        return logic.modifyInfo(stack, translate(key(stack, "info"), player));
    }

    public String getObjective(QuestStack stack, Player player, int index) {
        return logic.modifyObjective(stack, player, translate(key(stack, "objective." + index), player), index);
    }

    public int getObjectivesCount(QuestStack stack, Player player) {
        return logic.modifyObjectiveCount(stack, player, 1);
    }

    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return logic.isObjectiveCompleted(stack, player, index);
    }

    public boolean onEvent(QuestStack stack, Object event, Player player) {
        return logic.onEvent(stack, event, player);
    }

    public void onCompleted(QuestStack stack, Player player) {
        logic.onCompleted(stack, player);
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

    /** 1.7.10 RandomQuestText: texts {@code quest.<id>.<variation>.*}, the variation picked when the quest is taken. */
    public static class RandomText extends Quest {
        private final int variations;

        public RandomText(String id, QuestLogic logic, int variations, int xp) {
            super(id, logic, xp);
            this.variations = variations;
        }

        public static int variation(QuestStack stack) {
            return stack.getData().getShort("Variation");
        }

        @Override
        public void onTaken(RandomSource random, QuestStack stack, Player player) {
            stack.getData().putShort("Variation", (short) random.nextInt(variations));
        }

        @Override
        public boolean areQuestStacksEqual(QuestStack a, QuestStack b) {
            return super.areQuestStacksEqual(a, b) && variation(a) == variation(b);
        }

        @Override
        public String key(QuestStack stack, String part) {
            return "quest." + MatterOverdrive.MODID + "." + id + "." + variation(stack) + "." + part;
        }
    }

    /**
     * 1.7.10 GenericMultiQuest: several logics (each with its own data under its index, all auto-completing); sequential
     * ones show one objective at a time.
     */
    public static class Multi extends Quest {
        private final QuestLogic[] logics;
        private boolean sequential;
        private boolean autoComplete;

        public Multi(String id, int xp, AbstractLogic... logics) {
            super(id, null, xp);
            this.logics = logics;
            for (int i = 0; i < logics.length; i++) {
                logics[i].autoComplete(true);
                logics[i].setId(Integer.toString(i));
            }
        }

        public Multi sequential() {
            sequential = true;
            return this;
        }

        public Multi autoComplete() {
            autoComplete = true;
            return this;
        }

        public int getCurrentObjective(QuestStack stack) {
            return Math.clamp(stack.getData().getByte("CurrentObjective"), 0, logics.length - 1);
        }

        private void setCurrentObjective(QuestStack stack, int objective) {
            stack.getData().putByte("CurrentObjective", (byte) Math.clamp(objective, 0, logics.length - 1));
        }

        @Override
        public boolean canBeAccepted(QuestStack stack, Player player) {
            for (QuestLogic logic : logics) if (!logic.canAccept(stack, player)) return false;
            PlayerQuests quests = PlayerQuests.get(player);
            return !quests.hasCompletedQuest(stack) && !quests.hasQuest(stack);
        }

        @Override
        public boolean areQuestStacksEqual(QuestStack a, QuestStack b) {
            return a.getQuestId().equals(b.getQuestId());
        }

        @Override
        public String getTitle(QuestStack stack, Player player) {
            String t = translate(key(stack, "title"), player);
            return sequential ? logics[getCurrentObjective(stack)].modifyTitle(stack, t) : t;
        }

        @Override
        public String getInfo(QuestStack stack, Player player) {
            if (!sequential) return translate(key(stack, "info"), player);
            String specific = key(stack, "info." + getCurrentObjective(stack));
            String info = translate(Language.getInstance().has(specific) ? specific : key(stack, "info"), player);
            return logics[getCurrentObjective(stack)].modifyInfo(stack, info);
        }

        @Override
        public String getObjective(QuestStack stack, Player player, int index) {
            return logics[index].modifyObjective(stack, player, translate(key(stack, "objective." + index), player), index);
        }

        @Override
        public int getObjectivesCount(QuestStack stack, Player player) {
            return sequential ? getCurrentObjective(stack) + 1 : logics.length;
        }

        @Override
        public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
            return logics[index].isObjectiveCompleted(stack, player, 0);
        }

        @Override
        public void initQuestStack(RandomSource random, QuestStack stack) {
            for (QuestLogic logic : logics) logic.initQuestStack(random, stack);
        }

        @Override
        public boolean onEvent(QuestStack stack, Object event, Player player) {
            boolean changed = false;
            for (int i = 0; i < logics.length; i++) {
                if (!sequential || i <= getCurrentObjective(stack)) changed |= logics[i].onEvent(stack, event, player);
            }
            return changed;
        }

        @Override
        public void onCompleted(QuestStack stack, Player player) {
            for (QuestLogic logic : logics) logic.onCompleted(stack, player);
        }

        @Override
        public int getXpReward(QuestStack stack, Player player) {
            int total = xp;
            for (QuestLogic logic : logics) total = logic.modifyXP(stack, player, total);
            return total;
        }

        @Override
        public List<ItemStack> getRewards(QuestStack stack, Player player) {
            List<ItemStack> list = new ArrayList<>();
            for (var s : rewards) list.add(s.get());
            for (QuestLogic logic : logics) logic.modifyRewards(stack, player, list);
            return list;
        }

        /** 1.7.10: every logic done (advancing the sequential objective on the way), then complete if auto-completing. */
        @Override
        public void setCompleted(QuestStack stack, Player player) {
            for (int i = 0; i < logics.length; i++) {
                if (!logics[i].isObjectiveCompleted(stack, player, 0)) return;
                if (sequential && i >= getCurrentObjective(stack)) setCurrentObjective(stack, i + 1);
            }
            if (autoComplete) super.setCompleted(stack, player);
        }
    }
}
