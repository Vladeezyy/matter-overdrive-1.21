package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.AnalyzerBlock;
import matteroverdrive.block.DecomposerBlock;
import matteroverdrive.block.FusionReactorControllerBlock;
import matteroverdrive.block.FusionReactorIOBlock;
import matteroverdrive.block.GravitationalAnomalyBlock;
import matteroverdrive.block.GravitationalStabilizerBlock;
import matteroverdrive.block.InscriberBlock;
import matteroverdrive.block.MatterPipeBlock;
import matteroverdrive.block.NetworkPipeBlock;
import matteroverdrive.block.NetworkRouterBlock;
import matteroverdrive.block.NetworkSwitchBlock;
import matteroverdrive.block.PatternMonitorBlock;
import matteroverdrive.block.PatternStorageBlock;
import matteroverdrive.block.ReplicatorBlock;
import matteroverdrive.block.RecyclerBlock;
import matteroverdrive.block.SolarPanelBlock;
import matteroverdrive.block.WeaponStationBlock;
import matteroverdrive.block.AndroidStationBlock;
import matteroverdrive.block.ChargingStationBlock;
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

    // Machines (1.7.10: hardness 20, iron pickaxe)
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel", SolarPanelBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 5f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<InscriberBlock> INSCRIBER = BLOCKS.registerBlock("inscriber", InscriberBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    // Matter (phase 3)
    public static final DeferredBlock<DecomposerBlock> DECOMPOSER = BLOCKS.registerBlock("decomposer", DecomposerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<RecyclerBlock> RECYCLER = BLOCKS.registerBlock("matter_recycler", RecyclerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<MatterPipeBlock> MATTER_PIPE = BLOCKS.registerBlock("matter_pipe",
            p -> new MatterPipeBlock(false, p), p -> p.mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<MatterPipeBlock> HEAVY_MATTER_PIPE = BLOCKS.registerBlock("heavy_matter_pipe",
            p -> new MatterPipeBlock(true, p), p -> p.mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<AnalyzerBlock> ANALYZER = BLOCKS.registerBlock("matter_analyzer", AnalyzerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    // Matter network
    public static final DeferredBlock<NetworkPipeBlock> NETWORK_PIPE = BLOCKS.registerBlock("network_pipe", NetworkPipeBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<NetworkRouterBlock> NETWORK_ROUTER = BLOCKS.registerBlock("network_router", NetworkRouterBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<NetworkSwitchBlock> NETWORK_SWITCH = BLOCKS.registerBlock("network_switch", NetworkSwitchBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<PatternStorageBlock> PATTERN_STORAGE = BLOCKS.registerBlock("pattern_storage", PatternStorageBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<PatternMonitorBlock> PATTERN_MONITOR = BLOCKS.registerBlock("pattern_monitor", PatternMonitorBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 8));
    public static final DeferredBlock<ReplicatorBlock> REPLICATOR = BLOCKS.registerBlock("replicator", ReplicatorBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    // Phase 4
    public static final DeferredBlock<GravitationalAnomalyBlock> GRAVITATIONAL_ANOMALY = BLOCKS.registerBlock("gravitational_anomaly",
            GravitationalAnomalyBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).strength(-1f, 6000000f).noOcclusion().noLootTable()
                    .lightLevel(s -> 0));
    public static final DeferredBlock<GravitationalStabilizerBlock> GRAVITATIONAL_STABILIZER = BLOCKS.registerBlock("gravitational_stabilizer",
            GravitationalStabilizerBlock::new, p -> p.mapColor(MapColor.METAL).strength(20f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    // 1.7.10 machine_hull: tritanium plating for the fusion reactor ring
    public static final DeferredBlock<Block> MACHINE_HULL = BLOCKS.registerSimpleBlock("machine_hull",
            p -> p.mapColor(MapColor.METAL).strength(15f, 8f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> FUSION_REACTOR_COIL = BLOCKS.registerSimpleBlock("fusion_reactor_coil",
            p -> p.mapColor(MapColor.METAL).strength(30f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<FusionReactorIOBlock> FUSION_REACTOR_IO = BLOCKS.registerBlock("fusion_reactor_io", FusionReactorIOBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(30f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<FusionReactorControllerBlock> FUSION_REACTOR_CONTROLLER = BLOCKS.registerBlock("fusion_reactor_controller",
            FusionReactorControllerBlock::new, p -> p.mapColor(MapColor.METAL).strength(30f, 10f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(s -> 10));

    // Weapons (phase 5c; 1.7.10 hardness 20, resistance 9, light 10)
    public static final DeferredBlock<WeaponStationBlock> WEAPON_STATION = BLOCKS.registerBlock("weapon_station", WeaponStationBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10));

    // Androids (phase 6)
    public static final DeferredBlock<AndroidStationBlock> ANDROID_STATION = BLOCKS.registerBlock("android_station", AndroidStationBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10));
    public static final DeferredBlock<ChargingStationBlock> CHARGING_STATION = BLOCKS.registerBlock("charging_station", ChargingStationBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10));

    private MOBlocks() {}
}
