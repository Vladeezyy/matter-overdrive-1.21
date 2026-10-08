package matteroverdrive.android;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 1.7.10 AndroidPlayer's saved state, as a player attachment: whether the player is an android, the built-in energy,
 * unlocked biotic stats and their levels, timed effects (transformation, glitch), the selected ability, and the
 * android inventory (head, arms, legs, chest, other bionic parts + a battery slot).
 */
public class AndroidData implements ValueIOSerializable {
    public static final int SLOT_HEAD = 0, SLOT_ARMS = 1, SLOT_LEGS = 2, SLOT_CHEST = 3, SLOT_OTHER = 4, SLOT_BATTERY = 5, SLOTS = 6;
    public static final int MAX_ENERGY = 512000;
    private static final Codec<Map<String, Integer>> STATS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    boolean android;
    int energy = MAX_ENERGY;
    final Map<String, Integer> stats = new HashMap<>();
    int turning;
    int glitchTime;
    String activeStat = "";
    final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(SLOTS);
    /** Changed since the last sync to clients. */
    boolean dirty;

    public boolean isAndroid() {
        return android;
    }

    public boolean isTurning() {
        return turning > 0;
    }

    public int getTurning() {
        return turning;
    }

    public int getGlitchTime() {
        return glitchTime;
    }

    public int getBuiltinEnergy() {
        return energy;
    }

    public int getUnlockedLevel(BioticStat stat) {
        return stats.getOrDefault(stat.id(), 0);
    }

    /** 1.7.10 isUnlocked(stat, level): unlocked at that level or higher (level 0: unlocked at all). */
    public boolean isUnlocked(BioticStat stat, int level) {
        return stats.containsKey(stat.id()) && stats.get(stat.id()) >= level;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public String getActiveStat() {
        return activeStat;
    }

    public ItemStacksResourceHandler getInventory() {
        return inventory;
    }

    public ItemStack getStack(int slot) {
        return inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot));
    }

    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, ItemResource.of(stack), stack.getCount());
        dirty = true;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("android", android);
        output.putInt("energy", energy);
        output.store("stats", STATS_CODEC, stats);
        output.putInt("turning", turning);
        output.putString("active_stat", activeStat);
        inventory.serialize(output.child("inventory"));
    }

    @Override
    public void deserialize(ValueInput input) {
        android = input.getBooleanOr("android", false);
        energy = input.getIntOr("energy", MAX_ENERGY);
        stats.clear();
        input.read("stats", STATS_CODEC).ifPresent(stats::putAll);
        turning = input.getIntOr("turning", 0);
        activeStat = input.getStringOr("active_stat", "");
        input.child("inventory").ifPresent(inventory::deserialize);
    }

    /** Sends the whole state (it is small); the client keeps its own glitch countdown running between syncs. */
    public static final AttachmentSyncHandler<AndroidData> SYNC = new AttachmentSyncHandler<>() {
        /** Real clients only (GameTest mock players have no channels). */
        @Override
        public boolean sendToPlayer(IAttachmentHolder holder, net.minecraft.server.level.ServerPlayer to) {
            return to.connection.hasChannel(net.neoforged.neoforge.network.payload.SyncAttachmentsPayload.TYPE);
        }

        @Override
        public void write(RegistryFriendlyByteBuf buf, AndroidData data, boolean initialSync) {
            data.write(buf);
        }

        @Override
        public AndroidData read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable AndroidData previous) {
            AndroidData data = previous != null ? previous : new AndroidData();
            boolean wasTurning = data.isTurning();
            data.read(buf);
            if (!wasTurning && data.isTurning() && holder instanceof net.minecraft.world.entity.player.Player player) {
                AndroidClientHooks.onTransformationStarted(player);
            }
            return data;
        }
    };

    // --- client sync -------------------------------------------------------------------------------

    void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(android);
        buf.writeVarInt(energy);
        buf.writeMap(stats, (b, k) -> b.writeUtf(k), (b, v) -> b.writeVarInt(v));
        buf.writeVarInt(turning);
        buf.writeVarInt(glitchTime);
        buf.writeUtf(activeStat);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, getStack(i));
        }
    }

    void read(RegistryFriendlyByteBuf buf) {
        android = buf.readBoolean();
        energy = buf.readVarInt();
        stats.clear();
        stats.putAll(buf.readMap(b -> b.readUtf(), b -> b.readVarInt()));
        turning = buf.readVarInt();
        glitchTime = buf.readVarInt();
        activeStat = buf.readUtf();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            inventory.set(i, ItemResource.of(stack), stack.getCount());
        }
    }
}
