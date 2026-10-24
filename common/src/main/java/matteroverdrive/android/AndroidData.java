package matteroverdrive.android;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import matteroverdrive.compat.codec.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import matteroverdrive.compat.ValueIOSerializable;
import org.jetbrains.annotations.Nullable;
import matteroverdrive.platform.transfer.ItemStackHandler;

/**
 * 1.7.10 AndroidPlayer's saved state, as a player attachment: whether the player is an android, the built-in energy,
 * unlocked biotic stats and their levels, timed effects (transformation, glitch), the selected ability, and the
 * android inventory (head, arms, legs, chest, other bionic parts + a battery slot).
 */
public class AndroidData implements ValueIOSerializable {
    public static final int SLOT_HEAD = 0, SLOT_ARMS = 1, SLOT_LEGS = 2, SLOT_CHEST = 3, SLOT_OTHER = 4, SLOT_BATTERY = 5, SLOTS = 6;
    public static final int MAX_ENERGY = 512000;
    private static final Codec<Map<String, Integer>> STATS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<String, Long>> EFFECTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG);

    boolean android;
    int energy = MAX_ENERGY;
    final Map<String, Integer> stats = new HashMap<>();
    int turning;
    int glitchTime;
    String activeStat = "";
    /** 1.7.10 AndroidPlayer.effects: toggles (Nightvision, Cloaked, Shield as 0/1) and timestamps (last teleport...). */
    final Map<String, Long> effects = new HashMap<>();
    final ItemStackHandler inventory = new ItemStackHandler(SLOTS);
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

    public void setActiveStat(String id) {
        activeStat = id;
        dirty = true;
    }

    public long getEffect(String key) {
        return effects.getOrDefault(key, 0L);
    }

    public boolean getFlag(String key) {
        return getEffect(key) != 0;
    }

    public void setEffect(String key, long value) {
        if (value == 0) {
            effects.remove(key);
        } else {
            effects.put(key, value);
        }
        dirty = true;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStack getStack(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public void setStack(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
        dirty = true;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("android", android);
        output.putInt("energy", energy);
        output.store("stats", STATS_CODEC, stats);
        output.putInt("turning", turning);
        output.putString("active_stat", activeStat);
        output.store("effects", EFFECTS_CODEC, effects);
        output.tag().put("inventory", inventory.serializeNBT(output.lookup()));
    }

    @Override
    public void deserialize(ValueInput input) {
        android = input.getBooleanOr("android", false);
        energy = input.getIntOr("energy", MAX_ENERGY);
        stats.clear();
        input.read("stats", STATS_CODEC).ifPresent(stats::putAll);
        turning = input.getIntOr("turning", 0);
        activeStat = input.getStringOr("active_stat", "");
        effects.clear();
        input.read("effects", EFFECTS_CODEC).ifPresent(effects::putAll);
        input.child("inventory").ifPresent(c -> inventory.deserializeNBT(c.lookup(), c.tag()));
    }

    /** Sends the whole state (it is small); the client keeps its own glitch countdown running between syncs. */
    public static final matteroverdrive.platform.PlayerData.SyncHandler<AndroidData> SYNC = new matteroverdrive.platform.PlayerData.SyncHandler<>() {
        /** Real clients only (GameTest mock players have no channels). */
        @Override
        public boolean sendToPlayer(net.minecraft.world.entity.player.Player holder, net.minecraft.server.level.ServerPlayer to) {
            return matteroverdrive.platform.Platform.INSTANCE.canSend(to, matteroverdrive.network.MatterValuesPayload.TYPE);
        }

        @Override
        public void write(RegistryFriendlyByteBuf buf, AndroidData data, boolean initialSync) {
            data.write(buf);
        }

        @Override
        public AndroidData read(net.minecraft.world.entity.player.Player holder, RegistryFriendlyByteBuf buf, @Nullable AndroidData previous) {
            AndroidData data = previous != null ? previous : new AndroidData();
            boolean wasTurning = data.isTurning();
            data.read(buf);
            if (!wasTurning && data.isTurning() && holder != null) {
            net.minecraft.world.entity.player.Player player = holder;
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
        buf.writeMap(effects, (b, k) -> b.writeUtf(k), (b, v) -> b.writeVarLong(v));
        for (int i = 0; i < SLOTS; i++) {
            matteroverdrive.compat.codec.VanillaCodecs.ITEM_STACK_OPTIONAL.encode(buf, getStack(i));
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
        effects.clear();
        effects.putAll(buf.readMap(b -> b.readUtf(), b -> b.readVarLong()));
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = matteroverdrive.compat.codec.VanillaCodecs.ITEM_STACK_OPTIONAL.decode(buf);
            inventory.setStackInSlot(i, stack);
        }
    }
}
