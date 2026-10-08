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

    /** 1.7.10 MOExtendedProperties.addQuest (server). */
    public static boolean addQuest(ServerPlayer player, QuestStack stack) {
        if (stack.getQuest() == null) return false;
        PlayerQuests quests = PlayerQuests.get(player);
        // 1.7.10 gave a Data Pad with the first quest; it arrives with the Data Pad port
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
            if (quest != null && quest.logic().onEvent(stack, event, player)) changed = true;
        }
        if (changed) PlayerQuests.sync(server);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) onEvent(player, event);
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
            quest.logic().onCompleted(stack, player);
            player.sendSystemMessage(Component.translatable("chat." + MatterOverdrive.MODID + ".quest_completed", player.getDisplayName(),
                    Component.translatable(quest.key("title")).withStyle(ChatFormatting.GOLD)));
        }
        if (changed) PlayerQuests.sync(player);
    }

    private QuestEvents() {}
}
