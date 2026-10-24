package matteroverdrive.neoforge;

import com.mojang.serialization.Codec;

import matteroverdrive.matter.MatterRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/** The base matter values: the {@code matteroverdrive:matter} item data map (synced, so clients may read it too) */
final class MatterValues {
    static final DataMapType<Item, Integer> MATTER = DataMapType.builder(MatterRegistry.MATTER_MAP, Registries.ITEM, ExtraCodecs.POSITIVE_INT)
            .synced(Codec.INT, false)
            .build();

    private MatterValues() {}
}
