package matteroverdrive.quest;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7.10 AbstractQuestLogic: auto-complete and an optional id; with an id (multi quests) the logic keeps its data in
 * its own sub-tag of the stack's data.
 */
public abstract class AbstractLogic implements QuestLogic {
    protected boolean autoComplete;
    private @Nullable String id;

    @SuppressWarnings("unchecked")
    public <T extends AbstractLogic> T autoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
        return (T) this;
    }

    void setId(String id) {
        this.id = id;
    }

    /** 1.7.10 initTag + getTag. */
    protected CompoundTag tag(QuestStack stack) {
        if (id == null) return stack.getData();
        CompoundTag data = stack.getData();
        if (!(data.get(id) instanceof CompoundTag sub)) {
            CompoundTag created = new CompoundTag();
            data.put(id, created);
            return created;
        }
        return sub;
    }

    /** Completes the stack through its quest (1.7.10 markComplited(player, false)) when auto-completing. */
    protected void maybeComplete(QuestStack stack, Player player) {
        if (autoComplete) stack.markCompleted(player, false);
    }
}
