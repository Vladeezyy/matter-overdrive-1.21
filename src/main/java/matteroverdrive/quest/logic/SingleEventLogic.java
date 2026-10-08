package matteroverdrive.quest.logic;

import matteroverdrive.quest.AbstractLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.world.entity.player.Player;

/** 1.7.10 QuestLogicSingleEvent: done once an event of the kind happens to the player. */
public class SingleEventLogic extends AbstractLogic {
    private final Class<?> event;

    public SingleEventLogic(Class<?> event) {
        this.event = event;
    }

    public boolean hasEventFired(QuestStack stack) {
        return tag(stack).getBooleanOr("e", false);
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return hasEventFired(stack);
    }

    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        if (hasEventFired(stack) || !this.event.isInstance(event)) return false;
        tag(stack).putBoolean("e", true);
        maybeComplete(stack, player);
        return true;
    }
}
