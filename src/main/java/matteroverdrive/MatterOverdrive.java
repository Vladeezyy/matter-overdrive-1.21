package matteroverdrive;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import matteroverdrive.gametest.MOGameTests;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOCreativeTabs;
import matteroverdrive.init.MOFeatures;
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
        MOBlocks.BLOCKS.register(modEventBus);
        MOItems.ITEMS.register(modEventBus);
        MOBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        MOMenus.MENUS.register(modEventBus);
        MORecipes.TYPES.register(modEventBus);
        MORecipes.SERIALIZERS.register(modEventBus);
        MOFeatures.FEATURES.register(modEventBus);
        MOCreativeTabs.TABS.register(modEventBus);
        MOGameTests.register(modEventBus);
        modEventBus.addListener(MatterOverdrive::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (BatteryItem battery : new BatteryItem[] {MOItems.BATTERY.get(), MOItems.HC_BATTERY.get(), MOItems.CREATIVE_BATTERY.get()}) {
            event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> battery.createEnergyHandler(access), battery);
        }
        machine(event, MOBlockEntities.SOLAR_PANEL.get());
        machine(event, MOBlockEntities.INSCRIBER.get());
        machine(event, MOBlockEntities.DECOMPOSER.get());
        machine(event, MOBlockEntities.RECYCLER.get());
        machine(event, MOBlockEntities.ANALYZER.get());
        machine(event, MOBlockEntities.PATTERN_STORAGE.get());
        machine(event, MOBlockEntities.REPLICATOR.get());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, MOBlockEntities.MATTER_PIPE.get(), (pipe, side) -> pipe.getTank());
        machine(event, MOBlockEntities.GRAVITATIONAL_STABILIZER.get());
        machine(event, MOBlockEntities.FUSION_REACTOR_CONTROLLER.get());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MOBlockEntities.FUSION_REACTOR_IO.get(), (io, side) -> io.getEnergy());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, MOBlockEntities.FUSION_REACTOR_IO.get(), (io, side) -> io.getMatter());
    }

    private static <T extends MachineBlockEntity> void machine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, MachineBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.getInventory().automation());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> be.getMatterTank());
    }
}
