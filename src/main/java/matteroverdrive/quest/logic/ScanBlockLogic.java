package matteroverdrive.quest.logic;

import java.util.function.Supplier;

import matteroverdrive.item.DataPadItem;
import matteroverdrive.quest.AbstractLogic;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/**
 * 1.7.10 QuestLogicScanBlock: scan min..max of the block ($scanAmount / $maxScanAmount / $block); "only destroyable"
 * (1.7.10's setter always set it) counts only scans with a pad that destroys what it scans.
 */
public class ScanBlockLogic extends AbstractLogic {
    private final Supplier<Block> block;
    private final int minBlockScan, maxBlockScan, xpPerBlock;
    private boolean onlyDestroyable;

    public ScanBlockLogic(Supplier<Block> block, int minBlockScan, int maxBlockScan, int xpPerBlock) {
        this.block = block;
        this.minBlockScan = minBlockScan;
        this.maxBlockScan = maxBlockScan;
        this.xpPerBlock = xpPerBlock;
    }

    public ScanBlockLogic onlyDestroyable() {
        onlyDestroyable = true;
        return this;
    }

    public int getBlockScan(QuestStack stack) {
        return tag(stack).getShort("BlockScan");
    }

    public int getMaxBlockScan(QuestStack stack) {
        return tag(stack).getShort("MaxBlockScan");
    }

    private String replace(QuestStack stack, String text) {
        return text.replace("$block", block.get().getName().getString()).replace("$scanAmount", Integer.toString(getBlockScan(stack)))
                .replace("$maxScanAmount", Integer.toString(getMaxBlockScan(stack)));
    }

    @Override
    public String modifyInfo(QuestStack stack, String info) {
        return info.replace("$block", block.get().getName().getString());
    }

    @Override
    public String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        return replace(stack, objective);
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return getBlockScan(stack) >= getMaxBlockScan(stack);
    }

    @Override
    public void initQuestStack(RandomSource random, QuestStack stack) {
        tag(stack).putShort("MaxBlockScan", (short) QuestLogic.random(random, minBlockScan, maxBlockScan));
    }

    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        if (!(event instanceof QuestEvents.Scan scan)) return false;
        if (onlyDestroyable && scan.scanner().getItem() instanceof DataPadItem) {
            DataPadItem.Scan settings = DataPadItem.getScan(scan.scanner());
            if (settings == null || !settings.destroys()) return false;
        }
        if (!scan.state().is(block.get()) || getBlockScan(stack) >= getMaxBlockScan(stack)) return false;
        tag(stack).putShort("BlockScan", (short) (getBlockScan(stack) + 1));
        if (getBlockScan(stack) >= getMaxBlockScan(stack)) maybeComplete(stack, player);
        return true;
    }

    @Override
    public int modifyXP(QuestStack stack, Player player, int xp) {
        return xp + getMaxBlockScan(stack) * xpPerBlock;
    }
}
