package matteroverdrive.quest;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7.10 QuestStack: a quest taken by a player, with its progress data (the logic's NBT), who gave it, and whether it
 * is done. The quest is kept by id.
 */
public final class QuestStack {
    public static final Codec<QuestStack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("quest").forGetter(s -> s.questId),
            CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(s -> s.data),
            UUIDUtil.CODEC.optionalFieldOf("giver").forGetter(s -> Optional.ofNullable(s.giver)),
            Codec.BOOL.optionalFieldOf("completed", false).forGetter(s -> s.completed)
    ).apply(i, (id, data, giver, completed) -> {
        QuestStack s = new QuestStack(id);
        s.data = data.copy();
        s.giver = giver.orElse(null);
        s.completed = completed;
        return s;
    }));

    private final String questId;
    private CompoundTag data = new CompoundTag();
    private @Nullable UUID giver;
    boolean completed;

    public QuestStack(String questId) {
        this.questId = questId;
    }

    public QuestStack(Quest quest) {
        this(quest.id());
    }

    public String getQuestId() {
        return questId;
    }

    public @Nullable Quest getQuest() {
        return Quests.get(questId);
    }

    /** The logic's progress data (1.7.10 tagCompound). */
    public CompoundTag getData() {
        return data;
    }

    public boolean hasGiver() {
        return giver != null;
    }

    public boolean isGiver(Entity entity) {
        return giver != null && giver.equals(entity.getUUID());
    }

    public void setGiver(Entity entity) {
        this.giver = entity.getUUID();
    }

    public boolean isCompleted() {
        return completed;
    }

    /** 1.7.10 markComplited: forced, or through the quest (which may refuse). */
    public void markCompleted(Player player, boolean force) {
        Quest quest = getQuest();
        if (force || quest == null) completed = true;
        else quest.setCompleted(this, player);
    }

    public int getObjectivesCount(Player player) {
        Quest quest = getQuest();
        return quest == null ? 0 : quest.getObjectivesCount(this, player);
    }

    public boolean isObjectiveCompleted(Player player, int objective) {
        Quest quest = getQuest();
        return quest != null && quest.isObjectiveCompleted(this, player, objective);
    }

    public QuestStack copy() {
        QuestStack s = new QuestStack(questId);
        s.data = data.copy();
        s.giver = giver;
        s.completed = completed;
        return s;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof QuestStack s && s.questId.equals(questId) && s.data.equals(data) && java.util.Objects.equals(s.giver, giver)
                && s.completed == completed;
    }

    @Override
    public int hashCode() {
        return questId.hashCode();
    }
}
