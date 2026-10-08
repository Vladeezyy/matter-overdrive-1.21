package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.world.ImageStructure;
import matteroverdrive.world.ImageStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Phase 7 structures: the 1.7.10 image-generated buildings (data/matteroverdrive/worldgen/structure). */
public final class MOStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MatterOverdrive.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MatterOverdrive.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<ImageStructure>> IMAGE = TYPES.register("image", () -> () -> ImageStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> IMAGE_PIECE = PIECES.register("image",
            () -> (StructurePieceType.ContextlessType) ImageStructurePiece::new);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MatterOverdrive.MODID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<matteroverdrive.world.RandomCrateProcessor>> RANDOM_CRATE =
            PROCESSORS.register("random_crate", () -> () -> matteroverdrive.world.RandomCrateProcessor.CODEC);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<matteroverdrive.world.MadScientistCrateProcessor>> MAD_SCIENTIST_CRATE =
            PROCESSORS.register("mad_scientist_crate", () -> () -> matteroverdrive.world.MadScientistCrateProcessor.CODEC);

    private MOStructures() {}
}
