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
import matteroverdrive.menu.WeaponStationMenu;
import matteroverdrive.menu.AndroidStationMenu;
import matteroverdrive.menu.ChargingStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

public final class MOMenus {
    public static final Registrar<MenuType<?>> MENUS = Registrar.create(Registries.MENU, MatterOverdrive.MODID);

    public static final RegistryEntry<MenuType<?>, MenuType<SolarPanelMenu>> SOLAR_PANEL =
            MENUS.register("solar_panel", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(SolarPanelMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<InscriberMenu>> INSCRIBER =
            MENUS.register("inscriber", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(InscriberMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<DecomposerMenu>> DECOMPOSER =
            MENUS.register("decomposer", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(DecomposerMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<RecyclerMenu>> RECYCLER =
            MENUS.register("matter_recycler", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(RecyclerMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<AnalyzerMenu>> ANALYZER =
            MENUS.register("matter_analyzer", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(AnalyzerMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<PatternStorageMenu>> PATTERN_STORAGE =
            MENUS.register("pattern_storage", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(PatternStorageMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<PatternMonitorMenu>> PATTERN_MONITOR =
            MENUS.register("pattern_monitor", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(PatternMonitorMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<ReplicatorMenu>> REPLICATOR =
            MENUS.register("replicator", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(ReplicatorMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<FusionReactorMenu>> FUSION_REACTOR =
            MENUS.register("fusion_reactor_controller", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(FusionReactorMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<WeaponStationMenu>> WEAPON_STATION =
            MENUS.register("weapon_station", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(WeaponStationMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<AndroidStationMenu>> ANDROID_STATION =
            MENUS.register("android_station", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(AndroidStationMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<ChargingStationMenu>> CHARGING_STATION =
            MENUS.register("charging_station", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(ChargingStationMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<matteroverdrive.menu.TransporterMenu>> TRANSPORTER =
            MENUS.register("transporter", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(matteroverdrive.menu.TransporterMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<matteroverdrive.menu.AndroidSpawnerMenu>> ANDROID_SPAWNER =
            MENUS.register("android_spawner", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(matteroverdrive.menu.AndroidSpawnerMenu::new));

    public static final RegistryEntry<MenuType<?>, MenuType<matteroverdrive.menu.StarMapMenu>> STAR_MAP =
            MENUS.register("star_map", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(matteroverdrive.menu.StarMapMenu::new));
    public static final RegistryEntry<MenuType<?>, MenuType<matteroverdrive.menu.ContractMarketMenu>> CONTRACT_MARKET =
            MENUS.register("contract_market", () -> matteroverdrive.platform.Platform.INSTANCE.menuType(matteroverdrive.menu.ContractMarketMenu::new));

    private MOMenus() {}
}
