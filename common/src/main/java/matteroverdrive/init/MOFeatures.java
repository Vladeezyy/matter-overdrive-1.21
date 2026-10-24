package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.world.GravitationalAnomalyFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

public final class MOFeatures {
    public static final Registrar<Feature<?>> FEATURES = Registrar.create(Registries.FEATURE, MatterOverdrive.MODID);

    public static final RegistryEntry<Feature<?>, Feature<NoneFeatureConfiguration>> GRAVITATIONAL_ANOMALY =
            FEATURES.register("gravitational_anomaly", GravitationalAnomalyFeature::new);

    public static final Registrar<net.minecraft.world.level.storage.loot.functions.LootItemFunctionType> LOOT_FUNCTIONS =
            Registrar.create(Registries.LOOT_FUNCTION_TYPE, MatterOverdrive.MODID);
    public static final RegistryEntry<net.minecraft.world.level.storage.loot.functions.LootItemFunctionType, net.minecraft.world.level.storage.loot.functions.LootItemFunctionType> COPY_COMPONENTS =
            LOOT_FUNCTIONS.register("copy_components", () -> new net.minecraft.world.level.storage.loot.functions.LootItemFunctionType(
                    new matteroverdrive.world.loot.CopyComponentsFunction.Serializer()));

    private MOFeatures() {}
}
