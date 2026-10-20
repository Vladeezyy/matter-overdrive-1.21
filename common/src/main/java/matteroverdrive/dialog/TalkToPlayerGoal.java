package matteroverdrive.dialog;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7.10 EntityAITalkToPlayer + EntityAIWatchDialogPlayer: while in a conversation (player within sqrt(32) blocks) the
 * NPC stands still and looks at the player; walking away ends the conversation.
 */
public class TalkToPlayerGoal extends Goal {
    private final DialogNpc npc;

    public TalkToPlayerGoal(DialogNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        Player player = npc.getDialogPlayer();
        return npc.getEntity().isAlive() && player != null && npc.getEntity().distanceToSqr(player) <= 32;
    }

    @Override
    public void start() {
        npc.getEntity().getNavigation().stop();
    }

    @Override
    public void tick() {
        Player player = npc.getDialogPlayer();
        if (player != null) npc.getEntity().getLookControl().setLookAt(player, 30, 30);
    }

    @Override
    public void stop() {
        if (npc.getDialogPlayer() != null && npc.getEntity().distanceToSqr(npc.getDialogPlayer()) > 32) {
            DialogPayloads.endConversation(npc);
        }
    }
}
