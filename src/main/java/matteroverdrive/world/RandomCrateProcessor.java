package matteroverdrive.world;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.TritaniumCrateBlock;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Gives template tritanium crates a random colour (1.7.10 MadScientistHouse: tritaniumCrate[random.nextInt(16)]). */
public class RandomCrateProcessor extends StructureProcessor {
    public static final RandomCrateProcessor INSTANCE = new RandomCrateProcessor();
    public static final MapCodec<RandomCrateProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                            StructureTemplate.StructureBlockInfo blockInfo,
                                                            StructureTemplate.StructureBlockInfo relative, StructurePlaceSettings settings) {
        if (!(relative.state().getBlock() instanceof TritaniumCrateBlock)) return relative;
        RandomSource random = settings.getRandom(relative.pos());
        BlockState crate = MOBlocks.TRITANIUM_CRATES.get(random.nextInt(MOBlocks.TRITANIUM_CRATES.size())).get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, relative.state().getValue(HorizontalDirectionalBlock.FACING));
        return new StructureTemplate.StructureBlockInfo(relative.pos(), crate, relative.nbt());
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return MOStructures.RANDOM_CRATE.get();
    }
}
