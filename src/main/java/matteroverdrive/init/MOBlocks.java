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
import matteroverdrive.block.HoloSignBlock;
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
            MOBlocks.props(p -> p.mapColor(MapColor.STONE).strength(8f, 5f).requiresCorrectToolForDrops()));
    // Drops a dilithium crystal and 2-5 xp, like the 1.7.10 DilithiumOre (drop itself lives in the loot table).
    public static final DeferredBlock<DropExperienceBlock> DILITHIUM_ORE = BLOCKS.registerBlock("dilithium_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            MOBlocks.props(p -> p.mapColor(MapColor.STONE).strength(4f, 5f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> TRITANIUM_BLOCK = BLOCKS.registerSimpleBlock("tritanium_block",
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(15f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // Machines (1.7.10: hardness 20, iron pickaxe)
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel", SolarPanelBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 5f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<InscriberBlock> INSCRIBER = BLOCKS.registerBlock("inscriber", InscriberBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // Matter (phase 3)
    public static final DeferredBlock<DecomposerBlock> DECOMPOSER = BLOCKS.registerBlock("decomposer", DecomposerBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<RecyclerBlock> RECYCLER = BLOCKS.registerBlock("matter_recycler", RecyclerBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<MatterPipeBlock> MATTER_PIPE = BLOCKS.registerBlock("matter_pipe",
            p -> new MatterPipeBlock(false, p), MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<MatterPipeBlock> HEAVY_MATTER_PIPE = BLOCKS.registerBlock("heavy_matter_pipe",
            p -> new MatterPipeBlock(true, p), MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<AnalyzerBlock> ANALYZER = BLOCKS.registerBlock("matter_analyzer", AnalyzerBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // Matter network
    public static final DeferredBlock<NetworkPipeBlock> NETWORK_PIPE = BLOCKS.registerBlock("network_pipe", NetworkPipeBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<NetworkRouterBlock> NETWORK_ROUTER = BLOCKS.registerBlock("network_router", NetworkRouterBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<NetworkSwitchBlock> NETWORK_SWITCH = BLOCKS.registerBlock("network_switch", NetworkSwitchBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<PatternStorageBlock> PATTERN_STORAGE = BLOCKS.registerBlock("pattern_storage", PatternStorageBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<PatternMonitorBlock> PATTERN_MONITOR = BLOCKS.registerBlock("pattern_monitor", PatternMonitorBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 8)));
    public static final DeferredBlock<ReplicatorBlock> REPLICATOR = BLOCKS.registerBlock("replicator", ReplicatorBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));

    // Phase 4
    public static final DeferredBlock<GravitationalAnomalyBlock> GRAVITATIONAL_ANOMALY = BLOCKS.registerBlock("gravitational_anomaly",
            GravitationalAnomalyBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.COLOR_BLACK).strength(-1f, 6000000f).noOcclusion().noLootTable()
                    .lightLevel(s -> 0)));
    public static final DeferredBlock<GravitationalStabilizerBlock> GRAVITATIONAL_STABILIZER = BLOCKS.registerBlock("gravitational_stabilizer",
            GravitationalStabilizerBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // 1.7.10 machine_hull: tritanium plating for the fusion reactor ring
    public static final DeferredBlock<Block> MACHINE_HULL = BLOCKS.registerSimpleBlock("machine_hull",
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(15f, 8f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> FUSION_REACTOR_COIL = BLOCKS.registerSimpleBlock("fusion_reactor_coil",
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(30f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<FusionReactorIOBlock> FUSION_REACTOR_IO = BLOCKS.registerBlock("fusion_reactor_io", FusionReactorIOBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(30f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<FusionReactorControllerBlock> FUSION_REACTOR_CONTROLLER = BLOCKS.registerBlock("fusion_reactor_controller",
            FusionReactorControllerBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(30f, 10f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().lightLevel(s -> 10)));

    // Weapons (phase 5c; 1.7.10 hardness 20, resistance 9, light 10)
    public static final DeferredBlock<WeaponStationBlock> WEAPON_STATION = BLOCKS.registerBlock("weapon_station", WeaponStationBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10)));

    // Androids (phase 6)
    public static final DeferredBlock<AndroidStationBlock> ANDROID_STATION = BLOCKS.registerBlock("android_station", AndroidStationBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10)));
    public static final DeferredBlock<ChargingStationBlock> CHARGING_STATION = BLOCKS.registerBlock("charging_station", ChargingStationBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10)));

    // World & extras (phase 7): 1.7.10 BlockHoloSign hardness 20
    public static final DeferredBlock<HoloSignBlock> HOLO_SIGN = BLOCKS.registerBlock("holo_sign", HoloSignBlock::new,
            MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 5f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 8)));
    /** 1.7.10 tritanium_crate.<dye>: hardness 20, resistance 9, iron pickaxe; one block per dye colour. */
    public static final java.util.List<DeferredBlock<matteroverdrive.block.TritaniumCrateBlock>> TRITANIUM_CRATES =
            java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values()).map(color -> BLOCKS.registerBlock("tritanium_crate_" + color.getName(),
                    matteroverdrive.block.TritaniumCrateBlock::new, MOBlocks.props(p -> p.mapColor(color.getMapColor()).strength(20f, 9f).sound(SoundType.METAL)
                            .requiresCorrectToolForDrops().noOcclusion()))).toList();

    public static DeferredBlock<matteroverdrive.block.TritaniumCrateBlock> crate(net.minecraft.world.item.DyeColor color) {
        return TRITANIUM_CRATES.get(color.getId());
    }

    public static final DeferredBlock<matteroverdrive.block.TransporterBlock> TRANSPORTER = BLOCKS.registerBlock("transporter",
            matteroverdrive.block.TransporterBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    /** 1.7.10 BlockContractMarket: a BlockMonitor, hardness 20. */
    public static final DeferredBlock<matteroverdrive.block.StarMapBlock> STAR_MAP = BLOCKS.registerBlock("star_map",
            matteroverdrive.block.StarMapBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10)));
    public static final DeferredBlock<matteroverdrive.block.ContractMarketBlock> CONTRACT_MARKET = BLOCKS.registerBlock("contract_market",
            matteroverdrive.block.ContractMarketBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(20f, 9f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 8)));

    /** 1.7.10 BlockAndroidSpawner: unbreakable (hardness -1). */
    public static final DeferredBlock<matteroverdrive.block.AndroidSpawnerBlock> ANDROID_SPAWNER = BLOCKS.registerBlock("android_spawner",
            matteroverdrive.block.AndroidSpawnerBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(-1f, 3600000f).sound(SoundType.METAL)
                    .noLootTable()));

    /** 1.7.10 BlockMicrowave: hardness 10, resistance 9, iron pickaxe. */
    public static final DeferredBlock<matteroverdrive.block.MicrowaveBlock> MICROWAVE = BLOCKS.registerBlock("microwave",
            matteroverdrive.block.MicrowaveBlock::new, MOBlocks.props(p -> p.mapColor(MapColor.METAL).strength(10f, 9f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()));

    /** 1.7.10 blockMatterPlasma: the placed fluid. */
    public static final DeferredBlock<net.minecraft.world.level.block.LiquidBlock> MATTER_PLASMA = BLOCKS.registerBlock("matter_plasma",
            p -> new net.minecraft.world.level.block.LiquidBlock(MOFluids.MATTER_PLASMA.get(), p),
            MOBlocks.props(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).replaceable().noCollission().strength(100f).pushReaction(
                    net.minecraft.world.level.material.PushReaction.DESTROY).noLootTable().liquid().lightLevel(s -> 15)
                    .sound(SoundType.EMPTY)));

    /** 1.21.1: DeferredRegister.Blocks takes finished properties; this keeps the 1.21.10 "p -> p..." style. */
    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties props(
            java.util.function.UnaryOperator<net.minecraft.world.level.block.state.BlockBehaviour.Properties> props) {
        return props.apply(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
    }

    private MOBlocks() {}
}
