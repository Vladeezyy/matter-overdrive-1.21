package matteroverdrive.world;

import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.init.MOBlocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** 1.7.10 WorldGenGravitationalAnomaly: places an anomaly of 2048-10240 mass (rarity and height come from the placed feature). */
public class GravitationalAnomalyFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_MASS = 2048;
    public static final int MAX_MASS = 2048 + 8192;

    public GravitationalAnomalyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var pos = context.origin();
        if (!level.setBlock(pos, MOBlocks.GRAVITATIONAL_ANOMALY.get().defaultBlockState(), 2)) return false;
        if (level.getBlockEntity(pos) instanceof GravitationalAnomalyBlockEntity anomaly) {
            anomaly.setMass(MIN_MASS + context.random().nextInt(MAX_MASS - MIN_MASS));
        }
        return true;
    }
}
