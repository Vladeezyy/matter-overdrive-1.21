package matteroverdrive.quest.logic;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.init.MOItems;
import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * 1.7.10 QuestLogicBecomeAndroid ("Puny Humans"): bring a full set of rogue android parts (head, arms, legs, chest);
 * on completion one of each is taken and the transformation starts. A second objective (talk to the mad scientist)
 * shows once the parts are collected.
 */
public class BecomeAndroidLogic implements QuestLogic {
    private static int[] partSlots(Player player) {
        Item[] parts = {MOItems.ROGUE_ANDROID_HEAD.get(), MOItems.ROGUE_ANDROID_ARMS.get(), MOItems.ROGUE_ANDROID_LEGS.get(),
                MOItems.ROGUE_ANDROID_CHEST.get()};
        int[] slots = {-1, -1, -1, -1};
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            for (int p = 0; p < parts.length; p++) {
                if (player.getInventory().getItem(i).is(parts[p])) slots[p] = i;
            }
        }
        return slots;
    }

    private static boolean hasAllParts(Player player) {
        for (int slot : partSlots(player)) if (slot < 0) return false;
        return true;
    }

    @Override
    public int modifyObjectiveCount(QuestStack stack, Player player, int count) {
        return isObjectiveCompleted(stack, player, 0) ? 2 : 1;
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return index == 0 && hasAllParts(player);
    }

    @Override
    public void onCompleted(QuestStack stack, Player player) {
        int[] slots = partSlots(player);
        for (int slot : slots) {
            if (slot < 0) {
                player.displayClientMessage(Component.literal("<Mad Scientist>").withStyle(ChatFormatting.GOLD).append(
                        Component.translatable("entity." + MatterOverdrive.MODID + ".mad_scientist.line.fail." + player.getRandom().nextInt(4))
                                .withStyle(ChatFormatting.RED)), false);
                return;
            }
        }
        for (int slot : slots) player.getInventory().getItem(slot).shrink(1);
        if (player instanceof ServerPlayer server) {
            Android.startTransformation(server);
            server.closeContainer();
        }
    }
}
