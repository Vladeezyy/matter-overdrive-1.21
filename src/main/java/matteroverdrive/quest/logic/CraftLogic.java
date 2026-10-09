package matteroverdrive.quest.logic;

import java.util.List;
import java.util.function.Supplier;

import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 1.7.10 QuestLogicCraft: craft 1 + min..max of the item ($craftAmount / $craftMaxAmount / $craftItem). */
public class CraftLogic extends RandomItemLogic {
    private final int minCraftCount, maxCraftCount, xpPerCraft;

    public CraftLogic(List<Supplier<ItemStack>> items, int minCraftCount, int maxCraftCount, int xpPerCraft) {
        super(items);
        this.minCraftCount = minCraftCount;
        this.maxCraftCount = maxCraftCount;
        this.xpPerCraft = xpPerCraft;
    }

    public int getCraftCount(QuestStack stack) {
        return tag(stack).getInt("CraftCount");
    }

    public int getMaxCraftCount(QuestStack stack) {
        return getItem(stack).getCount() + tag(stack).getInt("MaxCraftCount");
    }

    private String replace(QuestStack stack, String text) {
        return text.replace("$craftAmount", Integer.toString(getCraftCount(stack)))
                .replace("$craftMaxAmount", Integer.toString(getMaxCraftCount(stack)))
                .replace("$craftItem", getItem(stack).getHoverName().getString());
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
        return getCraftCount(stack) >= getMaxCraftCount(stack);
    }

    @Override
    public void initQuestStack(RandomSource random, QuestStack stack) {
        initItemType(random, stack);
        tag(stack).putInt("MaxCraftCount", QuestLogic.random(random, minCraftCount, maxCraftCount));
    }

    /** 1.7.10 ItemCraftedEvent: one per craft (not per item made). */
    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        if (!(event instanceof PlayerEvent.ItemCraftedEvent crafted) || !crafted.getCrafting().is(getItem(stack).getItem())) return false;
        if (getCraftCount(stack) >= getMaxCraftCount(stack)) return false;
        tag(stack).putInt("CraftCount", getCraftCount(stack) + 1);
        if (isObjectiveCompleted(stack, player, 0)) maybeComplete(stack, player);
        return true;
    }

    @Override
    public int modifyXP(QuestStack stack, Player player, int xp) {
        return xp + xpPerCraft * getMaxCraftCount(stack);
    }
}
