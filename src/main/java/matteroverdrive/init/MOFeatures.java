package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.world.GravitationalAnomalyFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MatterOverdrive.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GRAVITATIONAL_ANOMALY =
            FEATURES.register("gravitational_anomaly", GravitationalAnomalyFeature::new);

    private MOFeatures() {}
}
