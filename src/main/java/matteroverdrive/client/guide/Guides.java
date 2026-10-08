package matteroverdrive.client.guide;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 1.7.10 MatterOverdriveGuides + MatterOverdriveGuide: the three categories and their entries at the 1.7.10 grid
 * positions (18 + x * 28, 16 + y * 28).
 */
public final class Guides {
    public record Category(String name, String icon, List<GuideEntry> entries) {}

    private static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();
    private static final List<GuideEntry> ENTRIES = new ArrayList<>();
    private static boolean built;

    private static void build() {
        if (built) return;
        built = true;
        Category general = category("general", "home_icon");
        Category weapons = category("weapons", "ammo");
        Category android = category("android", "android_slot_arms");
        add(general, block(MOBlocks.DILITHIUM_ORE.get()), "resources", 3, 0);
        add(general, block(MOBlocks.TRITANIUM_ORE.get()), "resources", 4, 0);
        add(general, item(MOItems.DILITHIUM_CRYSTAL.get()), "resources", 3, 1);
        add(general, item(MOItems.TRITANIUM_INGOT.get()), "resources", 4, 1);
        add(general, block(MOBlocks.REPLICATOR.get()), "machines", 0, 0);
        add(general, block(MOBlocks.DECOMPOSER.get()), "machines", 1, 0);
        add(general, block(MOBlocks.RECYCLER.get()), "machines", 0, 1);
        add(general, block(MOBlocks.ANALYZER.get()), "machines", 1, 1);
        add(general, block(MOBlocks.PATTERN_STORAGE.get()), "machines", 0, 2);
        add(general, block(MOBlocks.PATTERN_MONITOR.get()), "machines", 1, 2);
        add(general, block(MOBlocks.TRANSPORTER.get()), "machines", 0, 3);
        add(general, block(MOBlocks.HOLO_SIGN.get()), "machines", 1, 3);
        add(general, block(MOBlocks.INSCRIBER.get()), "machines", 0, 4);
        add(general, block(MOBlocks.CONTRACT_MARKET.get()), "machines", 1, 4);
        add(general, named("fusion_reactor", MOBlocks.FUSION_REACTOR_CONTROLLER.get(), MOBlocks.FUSION_REACTOR_COIL.get(),
                matteroverdrive.init.MODecorative.FORCE_GLASS.get(), MOBlocks.FUSION_REACTOR_IO.get()), "power", 3, 3);
        add(general, block(MOBlocks.GRAVITATIONAL_ANOMALY.get()), "power", 4, 3);
        add(general, block(MOBlocks.SOLAR_PANEL.get()), "power", 3, 4);
        add(general, named("batteries", MOItems.BATTERY.get(), MOItems.HC_BATTERY.get(), MOItems.CREATIVE_BATTERY.get()), "power", 4, 4);
        add(general, named("matter_transport", MOBlocks.HEAVY_MATTER_PIPE.get()), "matter", 6, 0);
        add(general, named("matter_fail", MOItems.MATTER_DUST.get()), "matter", 7, 0);
        add(general, named("matter_plasma", MOItems.MATTER_CONTAINER_FULL.get()), "matter", 6, 1);
        add(general, item(MOItems.MATTER_SCANNER.get()), "matter", 7, 1);
        add(general, item(MOItems.PATTERN_DRIVE.get()), "matter", 6, 2);
        add(general, item(MOItems.PORTABLE_DECOMPOSER.get()), "matter", 7, 2);
        add(general, block(MOBlocks.NETWORK_PIPE.get()), "matter_network", 6, 4);
        add(general, block(MOBlocks.NETWORK_SWITCH.get()), "matter_network", 7, 4);
        add(general, item(MOItems.NETWORK_FLASH_DRIVE.get()), "matter_network", 6, 5);
        add(general, block(MOBlocks.NETWORK_ROUTER.get()), "matter_network", 7, 5);
        add(general, item(MOItems.SPACETIME_EQUALIZER.get()), "items", 0, 6);
        add(general, item(MOItems.SECURITY_PROTOCOL.get()), "items", 1, 6);
        add(general, named("upgrades", LegacyNames.upgrades()), "items", 2, 6);
        add(general, named("drinks", MOItems.ROMULAN_ALE.get(), MOItems.EARL_GRAY_TEA.get()), "items", 3, 6);
        add(general, named("food", MOItems.EMERGENCY_RATION.get()), "items", 4, 6);
        add(general, item(MOItems.TRITANIUM_WRENCH.get()), "items", 0, 7);
        add(general, item(MOItems.TRANSPORT_FLASH_DRIVE.get()), "items", 1, 7);
        add(general, item(MOItems.CONTRACT.get()), "items", 2, 7);
        add(weapons, item(MOItems.PHASER.get()), "weapons", 4, 0);
        add(weapons, item(MOItems.PHASER_RIFLE.get()), "weapons", 5, 0);
        add(weapons, item(MOItems.OMNI_TOOL.get()), "weapons", 6, 0);
        add(weapons, item(MOItems.PLASMA_SHOTGUN.get()), "weapons", 4, 1);
        add(weapons, item(MOItems.ION_SNIPER.get()), "weapons", 5, 1);
        add(weapons, named("tritanium_tools", MOItems.TRITANIUM_AXE.get(), MOItems.TRITANIUM_SWORD.get(), MOItems.TRITANIUM_HOE.get(),
                MOItems.TRITANIUM_PICKAXE.get()), "weapons", 6, 1);
        add(weapons, item(MOItems.ENERGY_PACK.get()), "parts", 1, 0);
        add(weapons, named("weapon.modules.barrels", MOItems.BARREL_DAMAGE.get(), MOItems.BARREL_FIRE.get(), MOItems.BARREL_EXPLOSION.get(),
                MOItems.BARREL_HEAL.get()), "parts", 2, 0);
        add(weapons, named("weapon.modules.colors", MOItems.COLOR_MODULES.stream().map(c -> (ItemLike) c.get()).toArray(ItemLike[]::new)),
                "parts", 1, 1);
        add(weapons, item(MOItems.SNIPER_SCOPE.get()), "parts", 2, 1);
        add(weapons, named("tritanium_armor", MOItems.TRITANIUM_CHESTPLATE.get(), MOItems.TRITANIUM_LEGGINGS.get(), MOItems.TRITANIUM_BOOTS.get(),
                MOItems.TRITANIUM_HELMET.get()), "armor", 1, 3);
        add(weapons, block(MOBlocks.WEAPON_STATION.get()), "machines", 4, 3);
        add(android, named("android.pills", MOItems.ANDROID_PILL_RED.get(), MOItems.ANDROID_PILL_BLUE.get(), MOItems.ANDROID_PILL_YELLOW.get()),
                "items", 5, 1);
        add(android, named("android.parts", MOItems.ROGUE_ANDROID_HEAD.get(), MOItems.ROGUE_ANDROID_ARMS.get(), MOItems.ROGUE_ANDROID_LEGS.get(),
                MOItems.ROGUE_ANDROID_CHEST.get()), "items", 5, 2);
        add(android, item(MOItems.TRITANIUM_SPINE.get()), "items", 5, 3);
        add(android, block(MOBlocks.ANDROID_STATION.get()), "machines", 2, 2);
        add(android, block(MOBlocks.CHARGING_STATION.get()), "machines", 3, 2);
    }

    private static Category category(String name, String icon) {
        Category c = new Category(name, icon, new ArrayList<>());
        CATEGORIES.put(name, c);
        return c;
    }

    private static void add(Category category, GuideEntry entry, String group, int x, int y) {
        entry.place(group, 18 + x * 28, 16 + y * 28);
        category.entries().add(entry);
        ENTRIES.add(entry);
    }

    private static Supplier<List<ItemStack>> stacks(ItemLike... items) {
        return () -> java.util.Arrays.stream(items).map(ItemStack::new).toList();
    }

    /** 1.7.10 MOGuideEntryBlock / MOGuideEntryItem: named by the unlocalized name, e.g. tile.decomposer / item.phaser. */
    private static GuideEntry block(ItemLike block) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(block.asItem()).getPath();
        return new GuideEntry("tile." + id, "tile." + LegacyNames.file(id), true, stacks(block));
    }

    private static GuideEntry item(Item item) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        return new GuideEntry("item." + id, "item." + LegacyNames.file(id), true, stacks(item));
    }

    private static GuideEntry named(String name, ItemLike... icons) {
        return new GuideEntry(name, name, false, stacks(icons));
    }

    public static Map<String, Category> categories() {
        build();
        return CATEGORIES;
    }

    public static List<GuideEntry> entries() {
        build();
        return ENTRIES;
    }

    /** 1.7.10 findGuide: by entry name, also by the item's unlocalized name ("item.X" / "tile.X"). */
    public static @Nullable GuideEntry find(String name) {
        for (GuideEntry e : entries()) {
            if (e.name().equalsIgnoreCase(name) || e.file().equalsIgnoreCase(name)) return e;
        }
        return null;
    }

    public static @Nullable GuideEntry findByStack(ItemStack stack) {
        for (GuideEntry e : entries()) {
            if (!e.icon().isEmpty() && e.icons().stream().anyMatch(s -> s.is(stack.getItem())) && (e.name().startsWith("tile.") || e.name().startsWith("item."))) {
                return e;
            }
        }
        return null;
    }

    private Guides() {}
}
