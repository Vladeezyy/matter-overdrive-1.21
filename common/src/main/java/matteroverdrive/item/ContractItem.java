package matteroverdrive.item;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestStack;
import matteroverdrive.quest.Quests;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 Contract: an item carrying a quest. Named after the quest, objectives in the tooltip; use shows the contract
 * (accept to take the quest). A blank contract gets a random contract quest when used.
 */
public class ContractItem extends Item {
    /** Client: show the contract in this hand. */
    public static Consumer<InteractionHand> openScreen = hand -> {};
    /** Client: the local player, for objective texts in tooltips. */
    public static Supplier<Player> clientPlayer = () -> null;

    public ContractItem(Properties properties) {
        super(properties);
    }

    public static @Nullable QuestStack getQuest(ItemStack contract) {
        QuestStack stack = contract.get(MODataComponents.CONTRACT_QUEST.get());
        return stack == null || stack.getQuest() == null ? null : stack;
    }

    public static ItemStack of(QuestStack stack) {
        ItemStack contract = new ItemStack(matteroverdrive.init.MOItems.CONTRACT.get());
        contract.set(MODataComponents.CONTRACT_QUEST.get(), stack);
        return contract;
    }

    @Override
    public Component getName(ItemStack stack) {
        QuestStack quest = getQuest(stack);
        return quest != null ? Component.translatable(quest.getQuest().titleKey(quest)) : super.getName(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack contract = player.getItemInHand(hand);
        if (level.isClientSide()) {
            if (getQuest(contract) != null) openScreen.accept(hand);
        } else if (getQuest(contract) == null) {
            Quest quest = Quests.randomContract(player.getRandom());
            contract.set(MODataComponents.CONTRACT_QUEST.get(), quest.generate(player.getRandom()));
        }
        return InteractionResult.SUCCESS;
    }

    /** 1.7.10 addDetails: the objectives. */
    @Override
    public void appendHoverText(ItemStack contract, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        QuestStack stack = getQuest(contract);
        Player player = clientPlayer.get();
        if (stack == null || player == null) return;
        for (int i = 0; i < stack.getObjectivesCount(player); i++) {
            boolean done = stack.isObjectiveCompleted(player, i);
            tooltip.accept(Component.literal((done ? "■ " : "□ ") + stack.getQuest().getObjective(stack, player, i))
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN));
        }
    }
}
