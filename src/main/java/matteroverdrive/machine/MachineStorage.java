package matteroverdrive.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a machine's item remembers besides its energy (1.7.10 writeToDropItem "MaxEnergy", "PowerSend", "PowerReceive"
 * and "Matter", "MaxMatter", "MatterSend", "MatterReceive"); the energy part is 0 without energy, the matter part 0
 * without matter. The stored energy itself is the {@code energy} component.
 */
public record MachineStorage(int maxEnergy, int energySend, int energyReceive, int matter, int maxMatter, int matterSend, int matterReceive) {
    public static final Codec<MachineStorage> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("max_energy", 0).forGetter(MachineStorage::maxEnergy),
            Codec.INT.optionalFieldOf("energy_send", 0).forGetter(MachineStorage::energySend),
            Codec.INT.optionalFieldOf("energy_receive", 0).forGetter(MachineStorage::energyReceive),
            Codec.INT.optionalFieldOf("matter", 0).forGetter(MachineStorage::matter),
            Codec.INT.optionalFieldOf("max_matter", 0).forGetter(MachineStorage::maxMatter),
            Codec.INT.optionalFieldOf("matter_send", 0).forGetter(MachineStorage::matterSend),
            Codec.INT.optionalFieldOf("matter_receive", 0).forGetter(MachineStorage::matterReceive)
    ).apply(i, MachineStorage::new));
}
