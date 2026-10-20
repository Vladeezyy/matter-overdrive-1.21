package matteroverdrive.quest.logic;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.quest.AbstractLogic;
import matteroverdrive.quest.QuestEvents;
import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 QuestLogicPlaceBlock with a block stack: place min..max of exactly that stack (same item and components, so a
 * renamed "Communication Relay" coil, not any coil), within the radius of the stack's "Pos" when it has one
 * ($block, $distance = blocks still to go; like 1.7.10 only in the objective, the info text keeps its "$block").
 */
public class PlaceBlockLogic extends AbstractLogic {
    private final int radius;
    private final Supplier<ItemStack> blockStack;
    private final int minBlockPlace, maxBlockPlace;

    public PlaceBlockLogic(int radius, Supplier<ItemStack> blockStack, int minBlockPlace, int maxBlockPlace) {
        this.radius = radius;
        this.blockStack = blockStack;
        this.minBlockPlace = minBlockPlace;
        this.maxBlockPlace = maxBlockPlace;
    }

    public int getBlockPlaced(QuestStack stack) {
        return tag(stack).getShort("Placed");
    }

    public int getMaxBlockPlace(QuestStack stack) {
        return tag(stack).getShort("MaxPlaced");
    }

    public @Nullable BlockPos getPos(QuestStack stack) {
        int[] a = tag(stack).getIntArray("Pos");
        return a.length == 3 ? new BlockPos(a[0], a[1], a[2]) : null;
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return getBlockPlaced(stack) >= getMaxBlockPlace(stack);
    }

    @Override
    public String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        objective = objective.replace("$block", blockStack.get().getHoverName().getString());
        BlockPos pos = getPos(stack);
        if (pos != null) {
            double distance = new Vec3(Math.floor(player.getX()), Math.floor(player.getY()), Math.floor(player.getZ())).distanceTo(Vec3.atLowerCornerOf(pos));
            return objective.replace("$distance", (int) Math.max(distance - radius, 0) + " blocks");
        }
        return objective.replace("$distance", "0 blocks");
    }

    @Override
    public void initQuestStack(RandomSource random, QuestStack stack) {
        tag(stack).putShort("MaxPlaced", (short) QuestLogic.random(random, minBlockPlace, maxBlockPlace));
    }

    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        if (!(event instanceof QuestEvents.Place place) || !ItemStack.isSameItemSameComponents(blockStack.get(), place.itemInHand())) return false;
        BlockPos pos = getPos(stack);
        if (pos != null && Vec3.atLowerCornerOf(place.pos()).distanceTo(Vec3.atLowerCornerOf(pos)) > radius) return false;
        tag(stack).putShort("Placed", (short) (getBlockPlaced(stack) + 1));
        maybeComplete(stack, player);
        return true;
    }
}
