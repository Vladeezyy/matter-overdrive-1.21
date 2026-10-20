package matteroverdrive.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/** Saves to and loads from a tag (NeoForge's {@code INBTSerializable}) */
public interface INBTSerializable<T extends Tag> {
    T serializeNBT(HolderLookup.Provider provider);

    void deserializeNBT(HolderLookup.Provider provider, T tag);
}
