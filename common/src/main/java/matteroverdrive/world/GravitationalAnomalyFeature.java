package matteroverdrive.world;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.init.MOBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** 1.7.10 WorldGenGravitationalAnomaly: places an anomaly of 2048-10240 mass (rarity and height come from the placed feature). */
public record GravitationalAnomalyFeature() implements Feature {
    public static final int MIN_MASS = 2048;
    public static final int MAX_MASS = 2048 + 8192;
    public static final MapCodec<GravitationalAnomalyFeature> CODEC = MapCodec.unit(GravitationalAnomalyFeature::new);

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos pos) {
        if (!level.setBlock(pos, MOBlocks.GRAVITATIONAL_ANOMALY.get().defaultBlockState(), 2)) return false;
        if (level.getBlockEntity(pos) instanceof GravitationalAnomalyBlockEntity anomaly) {
            anomaly.setMass(MIN_MASS + random.nextInt(MAX_MASS - MIN_MASS));
        }
        return true;
    }
}
