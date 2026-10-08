package matteroverdrive.quest.logic;

import matteroverdrive.entity.MadScientist;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * 1.7.10 QuestLogicCocktailOfAscension: kill 5 creepers with a shovel, pick up 5 gunpowder and 5 red mushrooms in the
 * Nether (the picked-up ingredients are taken), then tell the junkie scientist. 1.7.10 quirks kept: the gunpowder count
 * is capped by the mushroom maximum and one extra gunpowder is taken per pickup.
 */
public class CocktailOfAscensionLogic implements QuestLogic {
    public static final int MAX_CREEPER_KILLS = 5;
    public static final int MAX_GUNPOWDER_COUNT = 5;
    public static final int MAX_MUSHROOM_COUNT = 5;

    public static int getCreeperKills(QuestStack stack) {
        return stack.getData().getByteOr("CreeperKills", (byte) 0);
    }

    public static int getGunpowder(QuestStack stack) {
        return stack.getData().getByteOr("GunpowderCount", (byte) 0);
    }

    public static int getMushrooms(QuestStack stack) {
        return stack.getData().getByteOr("MushroomCount", (byte) 0);
    }

    private static boolean hasTalkedTo(QuestStack stack) {
        return !stack.hasGiver() || stack.getData().getBooleanOr("TalkedToGiver", false);
    }

    @Override
    public String modifyInfo(QuestStack stack, String info) {
        return info.replace("%2$s", Integer.toString(MAX_GUNPOWDER_COUNT)).replace("%3$s", Integer.toString(MAX_MUSHROOM_COUNT));
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return switch (index) {
            case 0 -> getCreeperKills(stack) >= MAX_CREEPER_KILLS;
            case 1 -> getGunpowder(stack) >= MAX_GUNPOWDER_COUNT;
            case 2 -> getMushrooms(stack) >= MAX_MUSHROOM_COUNT;
            case 3 -> hasTalkedTo(stack);
            default -> false;
        };
    }

    @Override
    public String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        int[][] counts = {{getCreeperKills(stack), MAX_CREEPER_KILLS}, {getGunpowder(stack), MAX_GUNPOWDER_COUNT},
                {getMushrooms(stack), MAX_MUSHROOM_COUNT}};
        if (index > 2) return objective;
        return objective.replace("%2$s", Integer.toString(counts[index][0])).replace("%3$s", Integer.toString(counts[index][1]));
    }

    @Override
    public int modifyObjectiveCount(QuestStack stack, Player player, int count) {
        return stack.hasGiver() && getCreeperKills(stack) >= MAX_CREEPER_KILLS && getGunpowder(stack) >= MAX_GUNPOWDER_COUNT
                && getMushrooms(stack) >= MAX_MUSHROOM_COUNT ? 4 : 3;
    }

    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        CompoundTag tag = stack.getData();
        if (getCreeperKills(stack) < MAX_CREEPER_KILLS && event instanceof LivingDeathEvent death) {
            if (death.getEntity() instanceof Creeper && player.getMainHandItem().is(ItemTags.SHOVELS)) {
                tag.putByte("CreeperKills", (byte) (getCreeperKills(stack) + 1));
                return true;
            }
        } else if (event instanceof ItemEntityPickupEvent.Pre pickup) {
            ItemStack item = pickup.getItemEntity().getItem();
            if (item.is(Items.RED_MUSHROOM) && player.level().dimension() == Level.NETHER) {
                int count = getMushrooms(stack);
                if (count < MAX_MUSHROOM_COUNT) {
                    int now = Math.min(count + item.getCount(), MAX_MUSHROOM_COUNT);
                    item.shrink(now - count);
                    tag.putByte("MushroomCount", (byte) now);
                    pickup.getItemEntity().setItem(item);
                    return true;
                }
            } else if (item.is(Items.GUNPOWDER)) {
                int count = getGunpowder(stack);
                if (count < MAX_GUNPOWDER_COUNT) {
                    int now = Math.min(count + item.getCount(), MAX_MUSHROOM_COUNT);
                    item.shrink(now - count);
                    tag.putByte("GunpowderCount", (byte) now);
                    item.shrink(1);
                    pickup.getItemEntity().setItem(item);
                    return true;
                }
            }
        } else if (event instanceof QuestEvents.DialogInteract dialog && dialog.npc() instanceof MadScientist
                && dialog.message() == MadScientist.Dialogs.cocktailComplete) {
            tag.putBoolean("TalkedToGiver", true);
            stack.markCompleted(player, false);
            return true;
        }
        return false;
    }
}
