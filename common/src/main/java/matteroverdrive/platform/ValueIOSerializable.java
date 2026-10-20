package matteroverdrive.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Saves to and loads from vanilla's value input / output (NeoForge 21.6+'s {@code ValueIOSerializable}), and as a tag */
public interface ValueIOSerializable extends INBTSerializable<CompoundTag> {
    void serialize(ValueOutput output);

    void deserialize(ValueInput input);

    @Override
    default CompoundTag serializeNBT(HolderLookup.Provider provider) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        this.serialize(output);
        return output.buildResult();
    }

    @Override
    default void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
    }
}
