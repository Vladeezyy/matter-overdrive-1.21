package matteroverdrive;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import matteroverdrive.gametest.MOGameTests;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOCreativeTabs;
import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOFeatures;
import matteroverdrive.init.MOSounds;
import matteroverdrive.init.MOFluids;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOItems;
import matteroverdrive.init.MOMenus;
import matteroverdrive.init.MORecipes;
import matteroverdrive.item.BatteryItem;
import matteroverdrive.machine.MachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(MatterOverdrive.MODID)
public class MatterOverdrive {
    public static final String MODID = "matteroverdrive";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MatterOverdrive(IEventBus modEventBus, ModContainer modContainer) {
        MODataComponents.COMPONENTS.register(modEventBus);
        MOFluids.FLUID_TYPES.register(modEventBus);
        MOFluids.FLUIDS.register(modEventBus);
        matteroverdrive.init.MODecorative.init();
        MOBlocks.BLOCKS.register(modEventBus);
        MOItems.ITEMS.register(modEventBus);
        MOBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        MOMenus.MENUS.register(modEventBus);
        MORecipes.TYPES.register(modEventBus);
        MORecipes.SERIALIZERS.register(modEventBus);
        MOFeatures.FEATURES.register(modEventBus);
        MOEntities.ENTITIES.register(modEventBus);
        MOSounds.SOUNDS.register(modEventBus);
        matteroverdrive.init.MOStructures.TYPES.register(modEventBus);
        matteroverdrive.init.MOStructures.PIECES.register(modEventBus);
        matteroverdrive.init.MOStructures.PROCESSORS.register(modEventBus);
        matteroverdrive.init.MOAttachments.ATTACHMENTS.register(modEventBus);
        matteroverdrive.init.MOAttributes.ATTRIBUTES.register(modEventBus);
        MOCreativeTabs.TABS.register(modEventBus);
        MOGameTests.register(modEventBus);
        modEventBus.addListener(MatterOverdrive::registerCapabilities);
        modEventBus.addListener(MatterOverdrive::registerAttributes);
        modEventBus.addListener(MatterOverdrive::registerSpawnPlacements);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new matteroverdrive.item.MatterContainerItem.FluidHandler(stack),
                MOItems.MATTER_CONTAINER.get(), MOItems.MATTER_CONTAINER_FULL.get());
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> MOItems.PORTABLE_DECOMPOSER.get().createEnergyHandler(stack),
                MOItems.PORTABLE_DECOMPOSER.get());
        for (BatteryItem battery : new BatteryItem[] {MOItems.BATTERY.get(), MOItems.HC_BATTERY.get(), MOItems.CREATIVE_BATTERY.get()}) {
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> battery.createEnergyHandler(stack), battery);
        }
        for (var weapon : new matteroverdrive.item.weapon.EnergyWeaponItem[] {MOItems.PHASER.get(), MOItems.PHASER_RIFLE.get(), MOItems.PLASMA_SHOTGUN.get(), MOItems.ION_SNIPER.get(), MOItems.OMNI_TOOL.get()}) {
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> weapon.createEnergyHandler(stack), weapon);
        }
        machine(event, MOBlockEntities.SOLAR_PANEL.get());
        machine(event, MOBlockEntities.CHARGING_STATION.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MOBlockEntities.WEAPON_STATION.get(), (be, side) -> be.getInventory().automation());
        machine(event, MOBlockEntities.INSCRIBER.get());
        machine(event, MOBlockEntities.DECOMPOSER.get());
        machine(event, MOBlockEntities.RECYCLER.get());
        machine(event, MOBlockEntities.ANALYZER.get());
        machine(event, MOBlockEntities.TRANSPORTER.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MOBlockEntities.CONTRACT_MARKET.get(), (be, side) -> be.getInventory().automation());
        machine(event, MOBlockEntities.PATTERN_STORAGE.get());
        machine(event, MOBlockEntities.REPLICATOR.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MOBlockEntities.MATTER_PIPE.get(), (pipe, side) -> pipe.getTank());
        machine(event, MOBlockEntities.GRAVITATIONAL_STABILIZER.get());
        machine(event, MOBlockEntities.FUSION_REACTOR_CONTROLLER.get());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MOBlockEntities.FUSION_REACTOR_IO.get(), (io, side) -> io.getEnergy());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MOBlockEntities.FUSION_REACTOR_IO.get(), (io, side) -> io.getMatter());
    }

    private static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(MOEntities.ROGUE_ANDROID.get(), matteroverdrive.entity.monster.MeleeRogueAndroid.createAttributes().build());
        event.put(MOEntities.RANGED_ROGUE_ANDROID.get(), matteroverdrive.entity.monster.RangedRogueAndroid.createAttributes().build());
        event.put(MOEntities.MAD_SCIENTIST.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.5).build());
        event.put(MOEntities.MUTANT_SCIENTIST.get(), matteroverdrive.entity.monster.MutantScientist.createAttributes().build());
        event.put(MOEntities.FAILED_PIG.get(), net.minecraft.world.entity.animal.Pig.createAttributes().build());
        event.put(MOEntities.FAILED_COW.get(), net.minecraft.world.entity.animal.Cow.createAttributes().build());
        event.put(MOEntities.FAILED_CHICKEN.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(MOEntities.FAILED_SHEEP.get(), net.minecraft.world.entity.animal.sheep.Sheep.createAttributes().build());
    }

    private static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        for (var type : java.util.List.of(MOEntities.ROGUE_ANDROID.get(), MOEntities.RANGED_ROGUE_ANDROID.get())) {
            event.register(type, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    matteroverdrive.entity.monster.RogueAndroid::checkSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private static <T extends MachineBlockEntity> void machine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.getInventory().automation());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> be.getMatterTank());
    }
}
