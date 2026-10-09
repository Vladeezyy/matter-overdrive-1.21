package matteroverdrive.init;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 1.7.10 BlockDecorative and friends (phase 7a): the decorative tab's blocks with their hardness, harvest level and
 * resistance. "Rotated" blocks became pillars (their vertical metadata variant), the colored tritanium plate one block
 * per dye colour; force glass is the 1.7.10 tritanium glass.
 */
public final class MODecorative {
    public static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();
    public static final List<DeferredBlock<Block>> COLORED_PLATES = new ArrayList<>();

    public static final DeferredBlock<Block> STRIPES = metal("decorative_stripes", 5, 8, MapColor.COLOR_YELLOW);
    public static final DeferredBlock<Block> COILS = metal("decorative_coils", 5, 8, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<Block> CLEAN = metal("decorative_clean", 5, 8, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> VENT_DARK = metal("decorative_vent_dark", 5, 8, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> VENT_BRIGHT = metal("decorative_vent_bright", 5, 8, MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<Block> HOLO_MATRIX = metal("decorative_holo_matrix", 3, 4, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> TRITANIUM_PLATE = metal("decorative_tritanium_plate", 10, 10, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> CARBON_FIBER_PLATE = metal("decorative_carbon_fiber_plate", 10, 12, MapColor.COLOR_BLACK);
    public static final DeferredBlock<RotatedPillarBlock> MATTER_TUBE = add(MOBlocks.BLOCKS.registerBlock("decorative_matter_tube", RotatedPillarBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3, 4).sound(SoundType.GLASS).noOcclusion().requiresCorrectToolForDrops())));
    public static final DeferredBlock<RotatedPillarBlock> BEAMS = pillar("decorative_beams", 8, 8, MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> FLOOR_TILES = clay("decorative_floor_tiles", MapColor.SAND);
    public static final DeferredBlock<Block> FLOOR_TILES_GREEN = clay("decorative_floor_tiles_green", MapColor.COLOR_GREEN);
    public static final DeferredBlock<Block> FLOOR_NOISE = clay("decorative_floor_noise", MapColor.STONE);
    public static final DeferredBlock<Block> TRITANIUM_PLATE_STRIPE = metal("decorative_tritanium_plate_stripe", 10, 10, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> FLOOR_TILE_WHITE = clay("decorative_floor_tile_white", MapColor.QUARTZ);
    public static final DeferredBlock<Block> WHITE_PLATE = metal("decorative_white_plate", 8, 8, MapColor.SNOW);
    public static final DeferredBlock<RotatedPillarBlock> SEPARATOR = pillar("decorative_separator", 8, 8, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> TRITANIUM_LAMP = add(MOBlocks.BLOCKS.registerSimpleBlock("decorative_tritanium_lamp",
            MOBlocks.props(p -> p.mapColor(MapColor.ICE).strength(2, 4).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 15))));
    public static final DeferredBlock<Block> ENGINE_EXHAUST_PLASMA = add(MOBlocks.BLOCKS.registerSimpleBlock("decorative_engine_exhaust_plasma",
            MOBlocks.props(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1, 1).sound(SoundType.WOOL).lightLevel(s -> 15))));
    public static final DeferredBlock<TransparentBlock> FORCE_GLASS = add(MOBlocks.BLOCKS.registerBlock("force_glass", TransparentBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.NONE).strength(40, 6).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, pos, type) -> false).isRedstoneConductor((s, l, pos) -> false)
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false))));

    static {
        for (DyeColor color : DyeColor.values()) {
            DeferredBlock<Block> plate = metal("decorative_tritanium_plate_" + color.getName(), 10, 10, color.getMapColor());
            COLORED_PLATES.add(plate);
        }
    }

    public static final List<DeferredItem<BlockItem>> ITEMS = items();

    private static List<DeferredItem<BlockItem>> items() {
        List<DeferredItem<BlockItem>> items = new ArrayList<>();
        for (DeferredBlock<? extends Block> block : ALL) {
            DeferredItem<BlockItem> item = MOItems.ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
            MOItems.TAB_ORDER.add(item);
            items.add(item);
        }
        return items;
    }

    private static <B extends Block> DeferredBlock<B> add(DeferredBlock<B> block) {
        ALL.add(block);
        return block;
    }

    /** 1.7.10 Material.iron, pickaxe level 1. */
    private static DeferredBlock<Block> metal(String id, float hardness, float resistance, MapColor color) {
        return add(MOBlocks.BLOCKS.registerSimpleBlock(id, MOBlocks.props(p -> props(p, hardness, resistance, color, SoundType.METAL))));
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(String id, float hardness, float resistance, MapColor color) {
        return add(MOBlocks.BLOCKS.registerBlock(id, RotatedPillarBlock::new, MOBlocks.props(p -> props(p, hardness, resistance, color, SoundType.METAL))));
    }

    /** 1.7.10 Material.clay floors: 4/4, any pickaxe. */
    private static DeferredBlock<Block> clay(String id, MapColor color) {
        return add(MOBlocks.BLOCKS.registerSimpleBlock(id, MOBlocks.props(p -> props(p, 4, 4, color, SoundType.STONE))));
    }

    private static BlockBehaviour.Properties props(BlockBehaviour.Properties p, float hardness, float resistance, MapColor color, SoundType sound) {
        return p.mapColor(color).strength(hardness, resistance).sound(sound).requiresCorrectToolForDrops();
    }

    /** Called from the mod constructor so the registrations happen before the registries freeze. */
    public static void init() {}

    private MODecorative() {}
}
