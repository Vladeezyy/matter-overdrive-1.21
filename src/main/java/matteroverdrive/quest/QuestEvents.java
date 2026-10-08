package matteroverdrive.quest;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.7.10 MOExtendedProperties + PlayerQuestData behaviour: taking quests, passing events to the active quests'
 * logics, and turning in completed quests (XP, item rewards, chat line) on the player's tick.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID)
public final class QuestEvents {
    /** 1.7.10 MOEventDialogInteract: a dialog message became active for the player. */
    public record DialogInteract(Entity npc, Object message) {}

    /** 1.7.10 MOEventTransport: a transporter sent the player somewhere. */
    public record Transport(net.minecraft.core.BlockPos from, net.minecraft.core.BlockPos to) {}

    /** 1.7.10 MOEventGravitationalAnomalyConsume.Post: the player went into an anomaly's horizon. */
    public record AnomalyConsume(net.minecraft.core.BlockPos anomaly) {}

    /** 1.7.10 MOEventScan: a Data Pad or matter scanner finished scanning a block. */
    /** 1.7.10 BlockEvent.PlaceEvent: the block placed and the stack it was placed from. */
    public record Place(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, ItemStack itemInHand) {}

    public record Scan(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, ItemStack scanner) {}

    /** 1.7.10 MOExtendedProperties.addQuest (server). */
    public static boolean addQuest(ServerPlayer player, QuestStack stack) {
        if (stack.getQuest() == null) return false;
        PlayerQuests quests = PlayerQuests.get(player);
        // 1.7.10: the first quest ever comes with a Data Pad
        if (quests.active.isEmpty() && quests.completed.isEmpty()) {
            player.getInventory().add(new ItemStack(matteroverdrive.init.MOItems.DATA_PAD.get()));
        }
        // 1.7.10 addQuest: initQuestStack(rng, stack, player)
        stack.getQuest().onTaken(player.getRandom(), stack, player);
        quests.active.add(stack);
        PlayerQuests.sync(player);
        return true;
    }

    /** 1.7.10 PlayerQuestData.onEvent: every active quest sees the event; changed ones are synced. */
    public static void onEvent(Player player, Object event) {
        if (!(player instanceof ServerPlayer server)) return;
        boolean changed = false;
        for (QuestStack stack : List.copyOf(PlayerQuests.get(player).active)) {
            Quest quest = stack.getQuest();
            if (quest != null && quest.onEvent(stack, event, player)) changed = true;
        }
        if (changed) PlayerQuests.sync(server);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) onEvent(player, event);
    }

    /** 1.7.10 PlayerEventHandler.onItemCrafted (server side). */
    @SubscribeEvent
    static void onCrafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) onEvent(player, event);
    }

    /** 1.7.10 HarvestDropsEvent with a harvester (QuestLogicMine). */
    @SubscribeEvent
    static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) onEvent(player, event);
    }

    /** NeoForge restores the stack as it was before placing while the event fires. */
    @SubscribeEvent
    static void onPlace(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack inHand = player.getMainHandItem();
        if (!(inHand.getItem() instanceof net.minecraft.world.item.BlockItem block) || block.getBlock() != event.getPlacedBlock().getBlock()) {
            inHand = player.getOffhandItem();
        }
        onEvent(player, new Place(event.getPos(), event.getPlacedBlock(), inHand));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) manageQuestCompletion(player);
    }

    /** 1.7.10 manageQuestCompletion + onQuestCompleted. */
    public static void manageQuestCompletion(ServerPlayer player) {
        PlayerQuests quests = PlayerQuests.get(player);
        boolean changed = false;
        for (int i = 0; i < quests.active.size(); ) {
            QuestStack stack = quests.active.get(i);
            if (!stack.isCompleted()) {
                i++;
                continue;
            }
            quests.active.remove(i);
            changed = true;
            Quest quest = stack.getQuest();
            if (quest == null) continue;
            if (!quests.completed.contains(stack)) quests.completed.add(stack);
            player.giveExperiencePoints(quest.getXpReward(stack, player));
            for (ItemStack reward : quest.getRewards(stack, player)) {
                if (!player.getInventory().add(reward)) {
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getEyeY(), player.getZ(), reward));
                }
            }
            quest.onCompleted(stack, player);
            // 1.7.10 QuestStackReward: the follow-up quest, with some of this one's data
            for (Quest.QuestStackReward reward : quest.getQuestRewards()) {
                QuestStack next = new QuestStack(reward.quest().get());
                if (!next.getQuest().canBeAccepted(next, player)) continue;
                next.getQuest().initQuestStack(player.getRandom(), next);
                for (String key : reward.copyData()) {
                    if (stack.getData().get(key) != null) next.getData().put(key, stack.getData().get(key).copy());
                }
                addQuest(player, next);
            }
            player.sendSystemMessage(Component.translatable("chat." + MatterOverdrive.MODID + ".quest_completed", player.getDisplayName(),
                    Component.translatable(quest.titleKey(stack)).withStyle(ChatFormatting.GOLD)));
        }
        if (changed) PlayerQuests.sync(player);
    }

    private QuestEvents() {}
}
