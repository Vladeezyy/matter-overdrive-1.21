package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MatterOverdrive.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.matteroverdrive"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> MOItems.MATTER_DUST.get().getDefaultInstance())
            .displayItems((params, output) -> MOItems.TAB_ORDER.forEach(i -> output.accept(i.get())))
            .build());

    private MOCreativeTabs() {}
}
