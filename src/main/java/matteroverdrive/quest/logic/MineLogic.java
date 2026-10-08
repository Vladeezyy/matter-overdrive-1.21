package matteroverdrive.quest.logic;

import java.util.function.Supplier;

import matteroverdrive.quest.AbstractLogic;
import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 1.7.10 QuestLogicMine: break min..max of the block ($mineAmount / $maxMineAmount / $mineBlock). */
public class MineLogic extends AbstractLogic {
    private final Supplier<Block> block;
    /** Blocks that count too (deepslate ores, which 1.7.10 didn't have). */
    private final java.util.List<Supplier<Block>> also;
    private final int minMineCount, maxMineCount, xpPerMine;

    @SafeVarargs
    public MineLogic(Supplier<Block> block, int minMineCount, int maxMineCount, int xpPerMine, Supplier<Block>... also) {
        this.block = block;
        this.also = java.util.List.of(also);
        this.minMineCount = minMineCount;
        this.maxMineCount = maxMineCount;
        this.xpPerMine = xpPerMine;
    }

    public int getMineCount(QuestStack stack) {
        return tag(stack).getIntOr("MineCount", 0);
    }

    public int getMaxMineCount(QuestStack stack) {
        return tag(stack).getIntOr("MaxMineCount", 0);
    }

    private String replace(QuestStack stack, String text) {
        return text.replace("$mineAmount", Integer.toString(getMineCount(stack)))
                .replace("$maxMineAmount", Integer.toString(getMaxMineCount(stack)))
                .replace("$mineBlock", block.get().getName().getString());
    }

    @Override
    public String modifyInfo(QuestStack stack, String info) {
        return replace(stack, info);
    }

    @Override
    public String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        return replace(stack, objective);
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return getMineCount(stack) >= getMaxMineCount(stack);
    }

    @Override
    public void initQuestStack(RandomSource random, QuestStack stack) {
        tag(stack).putByte("BlockType", (byte) 0);
        tag(stack).putInt("MaxMineCount", QuestLogic.random(random, minMineCount, maxMineCount));
    }

    /** 1.7.10 HarvestDropsEvent by the player. */
    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        if (!(event instanceof BlockEvent.BreakEvent broken)) return false;
        if (!broken.getState().is(block.get()) && also.stream().noneMatch(b -> broken.getState().is(b.get()))) return false;
        if (getMineCount(stack) >= getMaxMineCount(stack)) return false;
        tag(stack).putInt("MineCount", getMineCount(stack) + 1);
        if (isObjectiveCompleted(stack, player, 0)) maybeComplete(stack, player);
        return true;
    }

    @Override
    public int modifyXP(QuestStack stack, Player player, int xp) {
        return xp + getMaxMineCount(stack) * xpPerMine;
    }
}
