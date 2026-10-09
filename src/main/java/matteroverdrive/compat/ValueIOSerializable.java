package matteroverdrive.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Stand-in for NeoForge 21.6+'s ValueIOSerializable, on top of INBTSerializable. */
public interface ValueIOSerializable extends INBTSerializable<CompoundTag> {
    void serialize(ValueOutput output);

    void deserialize(ValueInput input);

    @Override
    default CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        serialize(ValueOutput.of(tag, provider));
        return tag;
    }

    @Override
    default void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        deserialize(ValueInput.of(tag, provider));
    }
}
