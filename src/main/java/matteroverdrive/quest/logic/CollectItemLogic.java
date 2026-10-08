package matteroverdrive.quest.logic;

import java.util.List;
import java.util.function.Supplier;

import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 QuestLogicCollectItem (without destroyOnCollect): have 1 + min..max of the item in the inventory; they are
 * taken when the quest is completed.
 */
public class CollectItemLogic extends RandomItemLogic {
    private final int minItemCount, maxItemCount, xpPerItem;

    public CollectItemLogic(List<Supplier<ItemStack>> items, int minItemCount, int maxItemCount, int xpPerItem) {
        super(items);
        this.minItemCount = minItemCount;
        this.maxItemCount = maxItemCount;
        this.xpPerItem = xpPerItem;
    }

    /** 1.7.10 getMaxItemCount: the quest item's stack size (1) + the rolled count. */
    public int getMaxItemCount(QuestStack stack) {
        return getItem(stack).getCount() + tag(stack).getIntOr("MaxItemCount", 0);
    }

    public int getItemCount(Player player, QuestStack stack) {
        ItemStack item = getItem(stack);
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item.getItem())) count += s.getCount();
        }
        return count;
    }

    @Override
    public String modifyInfo(QuestStack stack, String info) {
        return QuestLogic.fmt(info, getMaxItemCount(stack), getItem(stack).getHoverName().getString());
    }

    @Override
    public String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        return QuestLogic.fmt(objective, getItemCount(player, stack), getMaxItemCount(stack), getItem(stack).getHoverName().getString());
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return getItemCount(player, stack) >= getMaxItemCount(stack);
    }

    @Override
    public void initQuestStack(RandomSource random, QuestStack stack) {
        initItemType(random, stack);
        tag(stack).putInt("MaxItemCount", QuestLogic.random(random, minItemCount, maxItemCount));
    }

    @Override
    public void onCompleted(QuestStack stack, Player player) {
        int left = getMaxItemCount(stack);
        ItemStack item = getItem(stack);
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.is(item.getItem())) continue;
            int taken = Math.min(left, s.getCount());
            s.shrink(taken);
            left -= taken;
        }
    }

    @Override
    public int modifyXP(QuestStack stack, Player player, int xp) {
        return xp + getMaxItemCount(stack) * xpPerItem;
    }
}
