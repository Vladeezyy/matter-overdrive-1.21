package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.menu.AnalyzerMenu;
import matteroverdrive.menu.DecomposerMenu;
import matteroverdrive.menu.FusionReactorMenu;
import matteroverdrive.menu.InscriberMenu;
import matteroverdrive.menu.PatternMonitorMenu;
import matteroverdrive.menu.PatternStorageMenu;
import matteroverdrive.menu.ReplicatorMenu;
import matteroverdrive.menu.RecyclerMenu;
import matteroverdrive.menu.SolarPanelMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MatterOverdrive.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SolarPanelMenu>> SOLAR_PANEL =
            MENUS.register("solar_panel", () -> IMenuTypeExtension.create(SolarPanelMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<InscriberMenu>> INSCRIBER =
            MENUS.register("inscriber", () -> IMenuTypeExtension.create(InscriberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DecomposerMenu>> DECOMPOSER =
            MENUS.register("decomposer", () -> IMenuTypeExtension.create(DecomposerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RecyclerMenu>> RECYCLER =
            MENUS.register("matter_recycler", () -> IMenuTypeExtension.create(RecyclerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AnalyzerMenu>> ANALYZER =
            MENUS.register("matter_analyzer", () -> IMenuTypeExtension.create(AnalyzerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PatternStorageMenu>> PATTERN_STORAGE =
            MENUS.register("pattern_storage", () -> IMenuTypeExtension.create(PatternStorageMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<PatternMonitorMenu>> PATTERN_MONITOR =
            MENUS.register("pattern_monitor", () -> IMenuTypeExtension.create(PatternMonitorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ReplicatorMenu>> REPLICATOR =
            MENUS.register("replicator", () -> IMenuTypeExtension.create(ReplicatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FusionReactorMenu>> FUSION_REACTOR =
            MENUS.register("fusion_reactor_controller", () -> IMenuTypeExtension.create(FusionReactorMenu::new));

    private MOMenus() {}
}
