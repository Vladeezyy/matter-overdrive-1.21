package matteroverdrive.quest.logic;

import java.util.List;
import java.util.function.Supplier;

import matteroverdrive.quest.AbstractLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 QuestLogicRandomItem: one of several items, picked per stack (or the first one). */
public abstract class RandomItemLogic extends AbstractLogic {
    protected final List<Supplier<ItemStack>> items;
    protected boolean randomItem = true;

    protected RandomItemLogic(List<Supplier<ItemStack>> items) {
        this.items = items;
    }

    protected void initItemType(RandomSource random, QuestStack stack) {
        tag(stack).putByte("ItemType", (byte) (randomItem ? random.nextInt(items.size()) : 0));
    }

    public ItemStack getItem(QuestStack stack) {
        return items.get(Math.clamp(tag(stack).getByte("ItemType"), 0, items.size() - 1)).get();
    }
}
