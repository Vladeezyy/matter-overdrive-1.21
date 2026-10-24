package matteroverdrive.platform;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link PlayerData} of one player (1.20.1 has no data attachments: Forge keeps this in a capability, Fabric in a
 * field the mixin adds to the player). Saved under the data ids; values are made on first use.
 */
public final class PlayerDataStore implements INBTSerializable<CompoundTag> {
    /** A player carrying a store (Fabric's mixin) */
    public interface Holder {
        PlayerDataStore matteroverdrive$playerData();
    }

    private final Map<ResourceLocation, INBTSerializable<CompoundTag>> values = new HashMap<>();
    /** Saved data of a type not created yet (kept until it is) */
    private CompoundTag pending = new CompoundTag();
    private HolderLookup.Provider registries = net.minecraft.core.RegistryAccess.EMPTY;

    @SuppressWarnings("unchecked")
    public <T extends INBTSerializable<CompoundTag>> T get(PlayerData<T> data) {
        return (T) values.computeIfAbsent(data.id(), id -> {
            T value = data.factory().get();
            String key = id.toString();
            if (pending.contains(key)) {
                value.deserializeNBT(registries, pending.getCompound(key));
                pending.remove(key);
            }
            return value;
        });
    }

    public <T extends INBTSerializable<CompoundTag>> void set(PlayerData<T> data, T value) {
        values.put(data.id(), value);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = pending.copy();
        values.forEach((id, value) -> tag.put(id.toString(), value.serializeNBT(provider)));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        values.clear();
        pending = tag.copy();
        registries = provider;
    }

    /** Death or return from the End: the new player gets everything (1.21 attachments copyOnDeath) */
    public void copyFrom(PlayerDataStore other, HolderLookup.Provider provider) {
        deserializeNBT(provider, other.serializeNBT(provider));
    }
}
