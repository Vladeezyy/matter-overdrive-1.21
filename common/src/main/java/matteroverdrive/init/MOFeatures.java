package matteroverdrive.init;

import com.mojang.serialization.MapCodec;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.world.GravitationalAnomalyFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

public final class MOFeatures {
    public static final Registrar<MapCodec<? extends Feature>> FEATURES = Registrar.create(Registries.FEATURE_TYPE, MatterOverdrive.MODID);

    /** 26.3: the feature type is its codec; the feature itself is data (worldgen/feature/gravitational_anomaly.json) */
    public static final RegistryEntry<MapCodec<? extends Feature>, MapCodec<GravitationalAnomalyFeature>> GRAVITATIONAL_ANOMALY =
            FEATURES.register("gravitational_anomaly", () -> GravitationalAnomalyFeature.CODEC);

    private MOFeatures() {}
}
