package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.world.ImageStructure;
import matteroverdrive.world.ImageStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

/** Phase 7 structures: the 1.7.10 image-generated buildings (data/matteroverdrive/worldgen/structure). */
public final class MOStructures {
    public static final Registrar<StructureType<?>> TYPES = Registrar.create(Registries.STRUCTURE_TYPE, MatterOverdrive.MODID);
    public static final Registrar<StructurePieceType> PIECES = Registrar.create(Registries.STRUCTURE_PIECE, MatterOverdrive.MODID);

    public static final RegistryEntry<StructureType<?>, StructureType<ImageStructure>> IMAGE = TYPES.register("image", () -> () -> ImageStructure.CODEC.codec());
    public static final RegistryEntry<StructurePieceType, StructurePieceType> IMAGE_PIECE = PIECES.register("image",
            () -> (StructurePieceType.ContextlessType) ImageStructurePiece::new);

    public static final Registrar<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>> PROCESSORS =
            Registrar.create(Registries.STRUCTURE_PROCESSOR, MatterOverdrive.MODID);
    public static final RegistryEntry<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<matteroverdrive.world.RandomCrateProcessor>> RANDOM_CRATE =
            PROCESSORS.register("random_crate", () -> () -> matteroverdrive.world.RandomCrateProcessor.CODEC);

    public static final RegistryEntry<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<matteroverdrive.world.MadScientistCrateProcessor>> MAD_SCIENTIST_CRATE =
            PROCESSORS.register("mad_scientist_crate", () -> () -> matteroverdrive.world.MadScientistCrateProcessor.CODEC);

    private MOStructures() {}
}
