package matteroverdrive.entity.ai;

import java.util.EnumSet;

import matteroverdrive.entity.monster.RogueAndroid;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 EntityAIMoveAlongPath: without an attack target, walk to the current waypoint of the android's path (from its
 * spawner); within range of it, go on to the next. When there's no route, take a random step towards it.
 */
public class MoveAlongPathGoal extends Goal {
    private final RogueAndroid mob;
    private final double speed;

    public MoveAlongPathGoal(RogueAndroid mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;
        Vec3 target = mob.getPathTarget();
        if (target == null) return false;
        if (!mob.getNavigation().isDone()) return true;
        if (mob.isNearPathTarget(target)) {
            mob.onPathTargetReached();
            return false;
        }
        if (mob.getNavigation().moveTo(target.x, target.y, target.z, speed)) return true;
        Vec3 step = DefaultRandomPos.getPosTowards(mob, 8, 2, target, Math.PI / 2);
        return step != null && mob.getNavigation().moveTo(step.x, step.y, step.z, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone();
    }
}
