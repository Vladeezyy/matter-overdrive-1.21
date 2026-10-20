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

/**
 * 1.7.10 MOExtendedProperties + PlayerQuestData behaviour: taking quests, passing events to the active quests'
 * logics, and turning in completed quests (XP, item rewards, chat line) on the player's tick.
 */
public final class QuestEvents {
    /** An entity died (NeoForge LivingDeathEvent) */
    public record Death(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        public net.minecraft.world.entity.LivingEntity getEntity() {
            return this.entity;
        }

        public net.minecraft.world.damagesource.DamageSource getSource() {
            return this.source;
        }
    }

    /** The player is about to pick an item up (NeoForge ItemEntityPickupEvent.Pre); handlers may change the stack */
    public record Pickup(ItemEntity itemEntity) {
        public ItemEntity getItemEntity() {
            return this.itemEntity;
        }
    }

    /** The player crafted an item (NeoForge PlayerEvent.ItemCraftedEvent) */
    public record Crafted(ItemStack crafting) {
        public ItemStack getCrafting() {
            return this.crafting;
        }
    }

    /** The player broke a block (NeoForge BlockEvent.BreakEvent) */
    public record Break(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        public net.minecraft.core.BlockPos getPos() {
            return this.pos;
        }

        public net.minecraft.world.level.block.state.BlockState getState() {
            return this.state;
        }
    }

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

    /** An entity died: the killer's quests see it (both loaders call this) */
    public static void onDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) onEvent(player, new Death(entity, source));
    }

    /** 1.7.10 PlayerEventHandler.onItemCrafted (server side). */
    public static void onCrafted(Player player, ItemStack crafted) {
        if (player instanceof ServerPlayer server) onEvent(server, new Crafted(crafted));
    }

    /** 1.7.10 HarvestDropsEvent with a harvester (QuestLogicMine). */
    public static void onBreak(Player player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (player instanceof ServerPlayer server) onEvent(server, new Break(pos, state));
    }

    /** A block was placed: the stack it came from is the one in hand (NeoForge restores it while its event fires). */
    public static void onPlace(Entity entity, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState placed) {
        if (!(entity instanceof ServerPlayer player)) return;
        ItemStack inHand = player.getMainHandItem();
        if (!(inHand.getItem() instanceof net.minecraft.world.item.BlockItem block) || block.getBlock() != placed.getBlock()) {
            inHand = player.getOffhandItem();
        }
        onEvent(player, new Place(pos, placed, inHand));
    }

    /** After the player's tick */
    public static void onPlayerTick(Player player) {
        if (player instanceof ServerPlayer server) manageQuestCompletion(server);
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
