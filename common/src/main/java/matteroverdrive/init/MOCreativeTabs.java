package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.BatteryItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

public final class MOCreativeTabs {
    public static final Registrar<CreativeModeTab> TABS = Registrar.create(Registries.CREATIVE_MODE_TAB, MatterOverdrive.MODID);

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> matteroverdrive.platform.Platform.INSTANCE.creativeTabBuilder()
            .title(Component.translatable("itemGroup.matteroverdrive"))
            .icon(() -> MOItems.MATTER_DUST.get().getDefaultInstance())
            .displayItems((params, output) -> MOItems.TAB_ORDER.forEach(i -> {
                output.accept(i.get());
                // 1.7.10 listed batteries both empty and fully charged.
                if (i.get() instanceof BatteryItem battery && !battery.isCreative()) {
                    output.accept(battery.charged());
                }
            }))
            .build());

    private MOCreativeTabs() {}
}
