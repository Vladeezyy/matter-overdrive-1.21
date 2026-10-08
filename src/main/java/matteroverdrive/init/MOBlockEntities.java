package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.AnalyzerBlockEntity;
import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.block.entity.FusionReactorControllerBlockEntity;
import matteroverdrive.block.entity.FusionReactorIOBlockEntity;
import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.block.entity.GravitationalStabilizerBlockEntity;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.block.entity.MatterPipeBlockEntity;
import matteroverdrive.block.entity.PatternMonitorBlockEntity;
import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity;
import matteroverdrive.block.entity.RecyclerBlockEntity;
import matteroverdrive.block.entity.SolarPanelBlockEntity;
import matteroverdrive.block.entity.WeaponStationBlockEntity;
import matteroverdrive.block.entity.AndroidStationBlockEntity;
import matteroverdrive.block.entity.ChargingStationBlockEntity;
import matteroverdrive.block.entity.HoloSignBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MatterOverdrive.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel", () -> new BlockEntityType<>(SolarPanelBlockEntity::new, MOBlocks.SOLAR_PANEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InscriberBlockEntity>> INSCRIBER =
            BLOCK_ENTITIES.register("inscriber", () -> new BlockEntityType<>(InscriberBlockEntity::new, MOBlocks.INSCRIBER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecomposerBlockEntity>> DECOMPOSER =
            BLOCK_ENTITIES.register("decomposer", () -> new BlockEntityType<>(DecomposerBlockEntity::new, MOBlocks.DECOMPOSER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecyclerBlockEntity>> RECYCLER =
            BLOCK_ENTITIES.register("matter_recycler", () -> new BlockEntityType<>(RecyclerBlockEntity::new, MOBlocks.RECYCLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterPipeBlockEntity>> MATTER_PIPE =
            BLOCK_ENTITIES.register("matter_pipe", () -> new BlockEntityType<>(MatterPipeBlockEntity::new,
                    MOBlocks.MATTER_PIPE.get(), MOBlocks.HEAVY_MATTER_PIPE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnalyzerBlockEntity>> ANALYZER =
            BLOCK_ENTITIES.register("matter_analyzer", () -> new BlockEntityType<>(AnalyzerBlockEntity::new, MOBlocks.ANALYZER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternStorageBlockEntity>> PATTERN_STORAGE =
            BLOCK_ENTITIES.register("pattern_storage", () -> new BlockEntityType<>(PatternStorageBlockEntity::new, MOBlocks.PATTERN_STORAGE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternMonitorBlockEntity>> PATTERN_MONITOR =
            BLOCK_ENTITIES.register("pattern_monitor", () -> new BlockEntityType<>(PatternMonitorBlockEntity::new, MOBlocks.PATTERN_MONITOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplicatorBlockEntity>> REPLICATOR =
            BLOCK_ENTITIES.register("replicator", () -> new BlockEntityType<>(ReplicatorBlockEntity::new, MOBlocks.REPLICATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravitationalAnomalyBlockEntity>> GRAVITATIONAL_ANOMALY =
            BLOCK_ENTITIES.register("gravitational_anomaly", () -> new BlockEntityType<>(GravitationalAnomalyBlockEntity::new, MOBlocks.GRAVITATIONAL_ANOMALY.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravitationalStabilizerBlockEntity>> GRAVITATIONAL_STABILIZER =
            BLOCK_ENTITIES.register("gravitational_stabilizer", () -> new BlockEntityType<>(GravitationalStabilizerBlockEntity::new, MOBlocks.GRAVITATIONAL_STABILIZER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionReactorControllerBlockEntity>> FUSION_REACTOR_CONTROLLER =
            BLOCK_ENTITIES.register("fusion_reactor_controller", () -> new BlockEntityType<>(FusionReactorControllerBlockEntity::new, MOBlocks.FUSION_REACTOR_CONTROLLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeaponStationBlockEntity>> WEAPON_STATION =
            BLOCK_ENTITIES.register("weapon_station", () -> new BlockEntityType<>(WeaponStationBlockEntity::new, MOBlocks.WEAPON_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AndroidStationBlockEntity>> ANDROID_STATION =
            BLOCK_ENTITIES.register("android_station", () -> new BlockEntityType<>(AndroidStationBlockEntity::new, MOBlocks.ANDROID_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION =
            BLOCK_ENTITIES.register("charging_station", () -> new BlockEntityType<>(ChargingStationBlockEntity::new, MOBlocks.CHARGING_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoloSignBlockEntity>> HOLO_SIGN =
            BLOCK_ENTITIES.register("holo_sign", () -> new BlockEntityType<>(HoloSignBlockEntity::new, MOBlocks.HOLO_SIGN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionReactorIOBlockEntity>> FUSION_REACTOR_IO =
            BLOCK_ENTITIES.register("fusion_reactor_io", () -> new BlockEntityType<>(FusionReactorIOBlockEntity::new, MOBlocks.FUSION_REACTOR_IO.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<matteroverdrive.block.entity.TritaniumCrateBlockEntity>> TRITANIUM_CRATE =
            BLOCK_ENTITIES.register("tritanium_crate", () -> new BlockEntityType<>(matteroverdrive.block.entity.TritaniumCrateBlockEntity::new,
                    MOBlocks.TRITANIUM_CRATES.stream().map(b -> (net.minecraft.world.level.block.Block) b.get()).collect(java.util.stream.Collectors.toSet())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<matteroverdrive.block.entity.TransporterBlockEntity>> TRANSPORTER =
            BLOCK_ENTITIES.register("transporter", () -> new BlockEntityType<>(matteroverdrive.block.entity.TransporterBlockEntity::new,
                    MOBlocks.TRANSPORTER.get()));

    private MOBlockEntities() {}
}
