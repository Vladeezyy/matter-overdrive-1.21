package matteroverdrive.quest.logic;

import java.util.function.Supplier;

import matteroverdrive.quest.AbstractLogic;
import matteroverdrive.quest.QuestLogic;
import matteroverdrive.quest.QuestStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 1.7.10 QuestLogicKillCreature: kill a random count (min..max) of one of the target kinds (picked per stack);
 * optionally only young ones. XP + xpPerKill per kill to do.
 */
public class KillCreatureLogic extends AbstractLogic {
    /** A kind of target: which entities count, and the type whose name is shown. */
    public record Target(Class<? extends LivingEntity> type, Supplier<EntityType<?>> nameType) {}

    private final Target[] targets;
    private final int minKillCount, maxKillCount, xpPerKill;
    private boolean onlyChildren;

    public KillCreatureLogic(int minKillCount, int maxKillCount, int xpPerKill, Target... targets) {
        this.targets = targets;
        this.minKillCount = minKillCount;
        this.maxKillCount = maxKillCount;
        this.xpPerKill = xpPerKill;
    }

    public KillCreatureLogic onlyChildren() {
        onlyChildren = true;
        return this;
    }

    private Target target(QuestStack stack) {
        return targets[Math.clamp(tag(stack).getByte("KillType"), 0, targets.length - 1)];
    }

    public int getKillCount(QuestStack stack) {
        return tag(stack).getInt("KillCount");
    }

    public int getMaxKillCount(QuestStack stack) {
        return tag(stack).getInt("MaxKillCount");
    }

    private String targetName(QuestStack stack) {
        return target(stack).nameType().get().getDescription().getString();
    }

    @Override
    public String modifyInfo(QuestStack stack, String info) {
        return QuestLogic.fmt(info, getMaxKillCount(stack), targetName(stack));
    }

    @Override
    public String modifyObjective(QuestStack stack, Player player, String objective, int index) {
        return QuestLogic.fmt(objective, getKillCount(stack), getMaxKillCount(stack), targetName(stack));
    }

    @Override
    public boolean isObjectiveCompleted(QuestStack stack, Player player, int index) {
        return getKillCount(stack) >= getMaxKillCount(stack);
    }

    @Override
    public void initQuestStack(RandomSource random, QuestStack stack) {
        CompoundTag tag = tag(stack);
        tag.putInt("MaxKillCount", QuestLogic.random(random, minKillCount, maxKillCount));
        tag.putByte("KillType", (byte) random.nextInt(targets.length));
    }

    @Override
    public boolean onEvent(QuestStack stack, Object event, Player player) {
        if (!(event instanceof LivingDeathEvent death) || !target(stack).type().isInstance(death.getEntity())) return false;
        if (onlyChildren && !death.getEntity().isBaby()) return false;
        int kills = getKillCount(stack);
        if (kills >= getMaxKillCount(stack)) return false;
        tag(stack).putInt("KillCount", kills + 1);
        if (isObjectiveCompleted(stack, player, 0)) maybeComplete(stack, player);
        return true;
    }

    @Override
    public int modifyXP(QuestStack stack, Player player, int xp) {
        return xp + xpPerKill * getMaxKillCount(stack);
    }
}
