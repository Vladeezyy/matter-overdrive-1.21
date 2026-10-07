package matteroverdrive;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import matteroverdrive.gametest.MOGameTests;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOCreativeTabs;
import matteroverdrive.init.MOItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MatterOverdrive.MODID)
public class MatterOverdrive {
    public static final String MODID = "matteroverdrive";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MatterOverdrive(IEventBus modEventBus, ModContainer modContainer) {
        MOBlocks.BLOCKS.register(modEventBus);
        MOItems.ITEMS.register(modEventBus);
        MOCreativeTabs.TABS.register(modEventBus);
        MOGameTests.register(modEventBus);
    }
}
