package matteroverdrive.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import matteroverdrive.init.MOAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import matteroverdrive.compat.ValueIOSerializable;

/**
 * 1.7.10 PlayerQuestData (in MOExtendedProperties): the active and the completed quests of a player. A player
 * attachment kept through death and synced to that player only; the client compares each sync with what it had to
 * show the quest HUD notifications (1.7.10 PacketUpdateQuest ADD / UPDATE / COMPLETE).
 */
public class PlayerQuests implements ValueIOSerializable {
    private static final Codec<List<QuestStack>> LIST = QuestStack.CODEC.listOf();

    final List<QuestStack> active = new ArrayList<>();
    final List<QuestStack> completed = new ArrayList<>();

    public static PlayerQuests get(Player player) {
        return MOAttachments.QUESTS.get(player);
    }

    public static void sync(Player player) {
        MOAttachments.QUESTS.sync(player);
    }

    public List<QuestStack> getActiveQuests() {
        return active;
    }

    public List<QuestStack> getCompletedQuests() {
        return completed;
    }

    public boolean hasCompletedQuest(QuestStack stack) {
        return completed.stream().anyMatch(q -> q.getQuest() != null && q.getQuest().areQuestStacksEqual(q, stack));
    }

    public boolean hasQuest(QuestStack stack) {
        return active.stream().anyMatch(q -> q.getQuest() != null && q.getQuest().areQuestStacksEqual(q, stack));
    }

    public @Nullable QuestStack findActive(Quest quest) {
        return active.stream().filter(q -> q.getQuestId().equals(quest.id())).findFirst().orElse(null);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("active", LIST, active);
        output.store("completed", LIST, completed);
    }

    @Override
    public void deserialize(ValueInput input) {
        active.clear();
        completed.clear();
        input.read("active", LIST).ifPresent(active::addAll);
        input.read("completed", LIST).ifPresent(completed::addAll);
    }

    // --- client sync -------------------------------------------------------------------------------

    /** Client: called after a sync with the previous and the new state (quest HUD). */
    public static BiConsumerHook clientUpdated = (player, before, after) -> {};

    public interface BiConsumerHook {
        void accept(Player player, PlayerQuests before, PlayerQuests after);
    }

    public PlayerQuests copy() {
        PlayerQuests c = new PlayerQuests();
        active.forEach(q -> c.active.add(q.copy()));
        completed.forEach(q -> c.completed.add(q.copy()));
        return c;
    }

    public static final matteroverdrive.platform.PlayerData.SyncHandler<PlayerQuests> SYNC = new matteroverdrive.platform.PlayerData.SyncHandler<>() {
        @Override
        public boolean sendToPlayer(net.minecraft.world.entity.player.Player holder, net.minecraft.server.level.ServerPlayer to) {
            return holder == to && matteroverdrive.platform.Platform.INSTANCE.canSend(to, matteroverdrive.network.MatterValuesPayload.TYPE);
        }

        @Override
        public void write(RegistryFriendlyByteBuf buf, PlayerQuests data, boolean initialSync) {
            buf.writeBoolean(initialSync);
            CompoundTag tag = new CompoundTag();
            tag.put("active", LIST.encodeStart(NbtOps.INSTANCE, data.active).getOrThrow(false, e -> {}));
            tag.put("completed", LIST.encodeStart(NbtOps.INSTANCE, data.completed).getOrThrow(false, e -> {}));
            buf.writeNbt(tag);
        }

        @Override
        public PlayerQuests read(net.minecraft.world.entity.player.Player holder, RegistryFriendlyByteBuf buf, @Nullable PlayerQuests previous) {
            boolean initial = buf.readBoolean();
            CompoundTag tag = buf.readNbt();
            PlayerQuests data = previous != null ? previous : new PlayerQuests();
            PlayerQuests before = data.copy();
            data.active.clear();
            data.completed.clear();
            if (tag != null) {
                LIST.parse(NbtOps.INSTANCE, tag.get("active")).result().ifPresent(data.active::addAll);
                LIST.parse(NbtOps.INSTANCE, tag.get("completed")).result().ifPresent(data.completed::addAll);
            }
            if (!initial && holder != null) clientUpdated.accept(holder, before, data);
            return data;
        }
    };

    static void forEachActive(Player player, Consumer<QuestStack> action) {
        List.copyOf(get(player).active).forEach(action);
    }
}
