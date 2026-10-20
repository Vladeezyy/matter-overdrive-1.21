package matteroverdrive.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import matteroverdrive.init.MOStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A 1.7.10 MOImageGen building as a structure: one {@link ImageStructurePiece} at a random block of the chunk. */
public class ImageStructure extends Structure {
    public static final MapCodec<ImageStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i), Building.CODEC.fieldOf("building").forGetter(s -> s.building)).apply(i, ImageStructure::new));

    private final Building building;

    public ImageStructure(StructureSettings settings, Building building) {
        super(settings);
        this.building = building;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMinBlockX() + context.random().nextInt(16);
        int z = context.chunkPos().getMinBlockZ() + context.random().nextInt(16);
        long seed = context.random().nextLong();
        return building.baseY(context, x, z).map(y -> {
            BlockPos origin = new BlockPos(x, y, z);
            return new GenerationStub(origin, builder -> builder.addPiece(new ImageStructurePiece(building, origin, seed)));
        });
    }

    @Override
    public StructureType<?> type() {
        return MOStructures.IMAGE.get();
    }
}
