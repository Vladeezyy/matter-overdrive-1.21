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

    private MOFeatures() {}
}
