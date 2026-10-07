package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MatterOverdrive.MODID);

    // Hardness/resistance values from the 1.7.10 MatterOverdriveBlocks.
    public static final DeferredBlock<Block> TRITANIUM_ORE = BLOCKS.registerSimpleBlock("tritanium_ore",
            p -> p.mapColor(MapColor.STONE).strength(8f, 5f).requiresCorrectToolForDrops());
    // Drops a dilithium crystal and 2-5 xp, like the 1.7.10 DilithiumOre (drop itself lives in the loot table).
    public static final DeferredBlock<DropExperienceBlock> DILITHIUM_ORE = BLOCKS.registerBlock("dilithium_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            p -> p.mapColor(MapColor.STONE).strength(4f, 5f).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> TRITANIUM_BLOCK = BLOCKS.registerSimpleBlock("tritanium_block",
            p -> p.mapColor(MapColor.METAL).strength(15f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    private MOBlocks() {}
}
