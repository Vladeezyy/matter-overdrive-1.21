package matteroverdrive.init;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.item.BatteryItem;
import matteroverdrive.item.MatterDustItem;
import matteroverdrive.item.PatternDriveItem;
import matteroverdrive.item.UpgradeItem;
import matteroverdrive.item.WrenchItem;
import matteroverdrive.item.weapon.EnergyPackItem;
import matteroverdrive.item.weapon.IonSniperItem;
import matteroverdrive.item.weapon.PhaserItem;
import matteroverdrive.item.weapon.PhaserRifleItem;
import matteroverdrive.item.weapon.PlasmaShotgunItem;
import matteroverdrive.item.weapon.WeaponColorModuleItem;
import matteroverdrive.item.weapon.WeaponBarrelItem;
import matteroverdrive.item.weapon.SniperScopeItem;
import matteroverdrive.item.android.AndroidPillItem;
import matteroverdrive.item.android.BionicPartItem;
import matteroverdrive.machine.UpgradeType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MatterOverdrive.MODID);
    /** Everything shown in the Matter Overdrive creative tab, in registration order. */
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    // Blocks
    public static final DeferredItem<BlockItem> TRITANIUM_ORE = block("tritanium_ore", MOBlocks.TRITANIUM_ORE);
    public static final DeferredItem<BlockItem> DILITHIUM_ORE = block("dilithium_ore", MOBlocks.DILITHIUM_ORE);
    public static final DeferredItem<BlockItem> TRITANIUM_BLOCK = block("tritanium_block", MOBlocks.TRITANIUM_BLOCK);

    // Materials and crafting components (plain items in 1.7.10 as well)
    public static final DeferredItem<Item> TRITANIUM_INGOT = simple("tritanium_ingot");
    public static final DeferredItem<Item> TRITANIUM_NUGGET = simple("tritanium_nugget");
    public static final DeferredItem<Item> TRITANIUM_DUST = simple("tritanium_dust");
    public static final DeferredItem<Item> TRITANIUM_PLATE = simple("tritanium_plate");
    public static final DeferredItem<Item> DILITHIUM_CRYSTAL = simple("dilithium_crystal");
    public static final DeferredItem<MatterDustItem> MATTER_DUST = item("matter_dust", p -> new MatterDustItem(false, p), p -> p);
    public static final DeferredItem<MatterDustItem> MATTER_DUST_REFINED = item("matter_dust_refined", p -> new MatterDustItem(true, p), p -> p);
    public static final DeferredItem<Item> MACHINE_CASING = simple("machine_casing");
    public static final DeferredItem<Item> S_MAGNET = simple("s_magnet");
    public static final DeferredItem<Item> H_COMPENSATOR = simple("h_compensator");
    public static final DeferredItem<Item> INTEGRATION_MATRIX = simple("integration_matrix");
    public static final DeferredItem<Item> ME_CONVERSION_MATRIX = simple("me_conversion_matrix");
    public static final DeferredItem<Item> FORCEFIELD_EMITTER = simple("forcefield_emitter");
    public static final DeferredItem<Item> WEAPON_HANDLE = simple("weapon_handle");
    public static final DeferredItem<Item> WEAPON_RECEIVER = simple("weapon_receiver");
    public static final DeferredItem<Item> PLASMA_CORE = simple("plasma_core");
    // 1.7.10 used one item with damage values 0-3; split into four items.
    public static final DeferredItem<Item> ISOLINEAR_CIRCUIT_MK1 = simple("isolinear_circuit_mk1");
    public static final DeferredItem<Item> ISOLINEAR_CIRCUIT_MK2 = simple("isolinear_circuit_mk2");
    public static final DeferredItem<Item> ISOLINEAR_CIRCUIT_MK3 = simple("isolinear_circuit_mk3");
    public static final DeferredItem<Item> ISOLINEAR_CIRCUIT_MK4 = simple("isolinear_circuit_mk4");

    // Tools: tritanium is iron-tier with 3122 durability; damage/speed follow the vanilla iron tools.
    public static final DeferredItem<Item> TRITANIUM_SWORD = item("tritanium_sword", Item::new,
            p -> p.sword(MOMaterials.TRITANIUM_TOOL, 3f, -2.4f));
    public static final DeferredItem<Item> TRITANIUM_PICKAXE = item("tritanium_pickaxe", Item::new,
            p -> p.pickaxe(MOMaterials.TRITANIUM_TOOL, 1f, -2.8f));
    public static final DeferredItem<AxeItem> TRITANIUM_AXE = item("tritanium_axe",
            p -> new AxeItem(MOMaterials.TRITANIUM_TOOL, 6f, -3.1f, p), p -> p);
    public static final DeferredItem<HoeItem> TRITANIUM_HOE = item("tritanium_hoe",
            p -> new HoeItem(MOMaterials.TRITANIUM_TOOL, -2f, -1f, p), p -> p);

    // Armor
    public static final DeferredItem<Item> TRITANIUM_HELMET = armor("tritanium_helmet", ArmorType.HELMET);
    public static final DeferredItem<Item> TRITANIUM_CHESTPLATE = armor("tritanium_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> TRITANIUM_LEGGINGS = armor("tritanium_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<Item> TRITANIUM_BOOTS = armor("tritanium_boots", ArmorType.BOOTS);

    // Machines
    public static final DeferredItem<BlockItem> SOLAR_PANEL = block("solar_panel", MOBlocks.SOLAR_PANEL);
    public static final DeferredItem<BlockItem> INSCRIBER = block("inscriber", MOBlocks.INSCRIBER);
    public static final DeferredItem<BlockItem> DECOMPOSER = block("decomposer", MOBlocks.DECOMPOSER);
    public static final DeferredItem<BlockItem> RECYCLER = block("matter_recycler", MOBlocks.RECYCLER);
    public static final DeferredItem<BlockItem> MATTER_PIPE = block("matter_pipe", MOBlocks.MATTER_PIPE);
    public static final DeferredItem<BlockItem> HEAVY_MATTER_PIPE = block("heavy_matter_pipe", MOBlocks.HEAVY_MATTER_PIPE);
    public static final DeferredItem<BlockItem> ANALYZER = block("matter_analyzer", MOBlocks.ANALYZER);
    public static final DeferredItem<BlockItem> PATTERN_STORAGE = block("pattern_storage", MOBlocks.PATTERN_STORAGE);
    public static final DeferredItem<BlockItem> PATTERN_MONITOR = block("pattern_monitor", MOBlocks.PATTERN_MONITOR);
    public static final DeferredItem<BlockItem> REPLICATOR = block("replicator", MOBlocks.REPLICATOR);
    public static final DeferredItem<BlockItem> NETWORK_PIPE = block("network_pipe", MOBlocks.NETWORK_PIPE);
    public static final DeferredItem<BlockItem> NETWORK_ROUTER = block("network_router", MOBlocks.NETWORK_ROUTER);
    public static final DeferredItem<BlockItem> NETWORK_SWITCH = block("network_switch", MOBlocks.NETWORK_SWITCH);
    public static final DeferredItem<BlockItem> GRAVITATIONAL_ANOMALY = block("gravitational_anomaly", MOBlocks.GRAVITATIONAL_ANOMALY);
    public static final DeferredItem<BlockItem> GRAVITATIONAL_STABILIZER = block("gravitational_stabilizer", MOBlocks.GRAVITATIONAL_STABILIZER);
    public static final DeferredItem<BlockItem> MACHINE_HULL = block("machine_hull", MOBlocks.MACHINE_HULL);
    public static final DeferredItem<BlockItem> FUSION_REACTOR_COIL = block("fusion_reactor_coil", MOBlocks.FUSION_REACTOR_COIL);
    public static final DeferredItem<BlockItem> FUSION_REACTOR_IO = block("fusion_reactor_io", MOBlocks.FUSION_REACTOR_IO);
    public static final DeferredItem<BlockItem> FUSION_REACTOR_CONTROLLER = block("fusion_reactor_controller", MOBlocks.FUSION_REACTOR_CONTROLLER);
    /** 1.7.10 SpacetimeEqualizer: worn on the chest, it cancels a gravitational anomaly's pull. */
    public static final DeferredItem<Item> SPACETIME_EQUALIZER = item("spacetime_equalizer", Item::new,
            p -> p.stacksTo(1).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).build()));
    // 1.7.10 PatternDrive("pattern_drive", 2)
    public static final DeferredItem<PatternDriveItem> PATTERN_DRIVE = item("pattern_drive", p -> new PatternDriveItem(2, p), p -> p);
    public static final DeferredItem<Item> NETWORK_FLASH_DRIVE = simple("network_flash_drive");

    // Energy (1.7.10: battery 2^19 FE 400/800, hc_battery 2^20 FE 4096/4096, creative 2^24 FE 8192/8192)
    public static final DeferredItem<BatteryItem> BATTERY = item("battery",
            p -> new BatteryItem(1 << 19, 400, 800, false, p), p -> p);
    public static final DeferredItem<BatteryItem> HC_BATTERY = item("hc_battery",
            p -> new BatteryItem(1 << 20, 4096, 4096, false, p), p -> p);
    public static final DeferredItem<BatteryItem> CREATIVE_BATTERY = item("creative_battery",
            p -> new BatteryItem(1 << 24, 8192, 8192, true, p), p -> p);

    // Upgrades (1.7.10 ItemUpgrade damage values 0-7)
    public static final DeferredItem<UpgradeItem> UPGRADE_BASE = upgrade("upgrade_base", Map.of());
    public static final DeferredItem<UpgradeItem> UPGRADE_SPEED = upgrade("upgrade_speed",
            Map.of(UpgradeType.SPEED, 0.75, UpgradeType.POWER_USAGE, 1.25, UpgradeType.FAIL, 1.25));
    public static final DeferredItem<UpgradeItem> UPGRADE_POWER = upgrade("upgrade_power",
            Map.of(UpgradeType.SPEED, 1.5, UpgradeType.POWER_USAGE, 0.75, UpgradeType.FAIL, 1.25));
    public static final DeferredItem<UpgradeItem> UPGRADE_FAILSAFE = upgrade("upgrade_failsafe",
            Map.of(UpgradeType.FAIL, 0.5, UpgradeType.SPEED, 1.25, UpgradeType.POWER_USAGE, 1.25));
    public static final DeferredItem<UpgradeItem> UPGRADE_RANGE = upgrade("upgrade_range",
            Map.of(UpgradeType.RANGE, 4.0, UpgradeType.POWER_USAGE, 1.5));
    public static final DeferredItem<UpgradeItem> UPGRADE_POWER_STORAGE = upgrade("upgrade_power_storage",
            Map.of(UpgradeType.POWER_STORAGE, 2.0));
    public static final DeferredItem<UpgradeItem> UPGRADE_HYPER_SPEED = upgrade("upgrade_hyper_speed",
            Map.of(UpgradeType.SPEED, 0.15, UpgradeType.POWER_USAGE, 2.0, UpgradeType.FAIL, 1.25));
    public static final DeferredItem<UpgradeItem> UPGRADE_MATTER_STORAGE = upgrade("upgrade_matter_storage",
            Map.of(UpgradeType.MATTER_STORAGE, 2.0));

    public static final DeferredItem<WrenchItem> TRITANIUM_WRENCH = item("tritanium_wrench", WrenchItem::new, p -> p);

    // Weapons (phase 5)
    public static final DeferredItem<PhaserItem> PHASER = item("phaser", PhaserItem::new, p -> p);
    public static final DeferredItem<PhaserRifleItem> PHASER_RIFLE = item("phaser_rifle", PhaserRifleItem::new, p -> p);
    public static final DeferredItem<PlasmaShotgunItem> PLASMA_SHOTGUN = item("plasma_shotgun", PlasmaShotgunItem::new, p -> p);
    public static final DeferredItem<IonSniperItem> ION_SNIPER = item("ion_sniper", IonSniperItem::new, p -> p);
    public static final DeferredItem<EnergyPackItem> ENERGY_PACK = item("energy_pack", EnergyPackItem::new, p -> p);
    /** 1.7.10 MatterContainer: a portable matter tank; needed for the plasma core recipe (its tank isn't ported yet). */
    public static final DeferredItem<Item> MATTER_CONTAINER = simple("matter_container");
    public static final List<DeferredItem<WeaponColorModuleItem>> COLOR_MODULES = colorModules();
    public static final DeferredItem<WeaponBarrelItem> BARREL_DAMAGE = barrel(WeaponBarrelItem.Type.DAMAGE);
    public static final DeferredItem<WeaponBarrelItem> BARREL_FIRE = barrel(WeaponBarrelItem.Type.FIRE);
    public static final DeferredItem<WeaponBarrelItem> BARREL_EXPLOSION = barrel(WeaponBarrelItem.Type.EXPLOSION);
    public static final DeferredItem<WeaponBarrelItem> BARREL_HEAL = barrel(WeaponBarrelItem.Type.HEAL);
    public static final DeferredItem<SniperScopeItem> SNIPER_SCOPE = item("sniper_scope", SniperScopeItem::new, p -> p);
    public static final DeferredItem<BlockItem> WEAPON_STATION = block("weapon_station", MOBlocks.WEAPON_STATION);

    // Androids (phase 6)
    public static final DeferredItem<AndroidPillItem> ANDROID_PILL_RED = item("android_pill_red", p -> new AndroidPillItem(AndroidPillItem.Type.RED, p), p -> p);
    public static final DeferredItem<AndroidPillItem> ANDROID_PILL_BLUE = item("android_pill_blue", p -> new AndroidPillItem(AndroidPillItem.Type.BLUE, p), p -> p);
    public static final DeferredItem<BlockItem> ANDROID_STATION = block("android_station", MOBlocks.ANDROID_STATION);
    public static final DeferredItem<BlockItem> CHARGING_STATION = block("charging_station", MOBlocks.CHARGING_STATION);
    public static final DeferredItem<BionicPartItem> ROGUE_ANDROID_HEAD = item("rogue_android_part_head", p -> new BionicPartItem(0, p), p -> p);
    public static final DeferredItem<BionicPartItem> ROGUE_ANDROID_ARMS = item("rogue_android_part_arms", p -> new BionicPartItem(1, p), p -> p);
    public static final DeferredItem<BionicPartItem> ROGUE_ANDROID_LEGS = item("rogue_android_part_legs", p -> new BionicPartItem(2, p), p -> p);
    public static final DeferredItem<BionicPartItem> ROGUE_ANDROID_CHEST = item("rogue_android_part_chest", p -> new BionicPartItem(3, p), p -> p);
    /** 1.7.10 TritaniumSpine: the "other" bionic slot, +2 max health and half the glitch time. */
    public static final DeferredItem<BionicPartItem> TRITANIUM_SPINE = item("tritanium_spine", p -> new BionicPartItem(4, 2, -0.5, p), p -> p);

    // Food (phase 7a; 1.7.10 ItemFood values)
    public static final DeferredItem<Item> EMERGENCY_RATION = item("emergency_ration", Item::new,
            p -> p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));
    /** 1.7.10 EarlGrayTea: 4 / 0.8, always drinkable, clears potion effects like milk, leaves the bottle. */
    public static final DeferredItem<Item> EARL_GRAY_TEA = item("earl_gray_tea", Item::new,
            p -> p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.8f).alwaysEdible().build(),
                    net.minecraft.world.item.component.Consumables.defaultDrink()
                            .onConsume(net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect.INSTANCE).build())
                    .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE).stacksTo(16));
    public static final DeferredItem<matteroverdrive.item.food.RomulanAleItem> ROMULAN_ALE = item("romulan_ale", matteroverdrive.item.food.RomulanAleItem::new,
            p -> p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build(),
                    net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)
                    .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE).stacksTo(16));
    public static final DeferredItem<BlockItem> HOLO_SIGN = block("holo_sign", MOBlocks.HOLO_SIGN);
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ROGUE_ANDROID_SPAWN_EGG = item("rogue_android_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.ROGUE_ANDROID.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> RANGED_ROGUE_ANDROID_SPAWN_EGG = item("ranged_rogue_android_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.RANGED_ROGUE_ANDROID.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> MUTANT_SCIENTIST_SPAWN_EGG = item("mutant_scientist_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.MUTANT_SCIENTIST.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FAILED_PIG_SPAWN_EGG = item("failed_pig_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.FAILED_PIG.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FAILED_COW_SPAWN_EGG = item("failed_cow_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.FAILED_COW.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FAILED_CHICKEN_SPAWN_EGG = item("failed_chicken_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.FAILED_CHICKEN.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FAILED_SHEEP_SPAWN_EGG = item("failed_sheep_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(MOEntities.FAILED_SHEEP.get()));

    public static final DeferredItem<AndroidPillItem> ANDROID_PILL_YELLOW = item("android_pill_yellow", p -> new AndroidPillItem(AndroidPillItem.Type.YELLOW, p), p -> p);

    private static DeferredItem<WeaponBarrelItem> barrel(WeaponBarrelItem.Type type) {
        return item("weapon_module_barrel_" + type.id(), p -> new WeaponBarrelItem(type, p), p -> p);
    }

    private static List<DeferredItem<WeaponColorModuleItem>> colorModules() {
        List<DeferredItem<WeaponColorModuleItem>> list = new ArrayList<>();
        for (int i = 0; i < WeaponColorModuleItem.NAMES.length; i++) {
            int color = WeaponColorModuleItem.COLORS[i];
            list.add(item("weapon_module_color_" + WeaponColorModuleItem.NAMES[i], p -> new WeaponColorModuleItem(color, p), p -> p));
        }
        return list;
    }

    private static DeferredItem<UpgradeItem> upgrade(String name, Map<UpgradeType, Double> stats) {
        return item(name, p -> new UpgradeItem(stats, p), p -> p);
    }

    private static DeferredItem<Item> simple(String name) {
        DeferredItem<Item> item = ITEMS.registerSimpleItem(name);
        TAB_ORDER.add(item);
        return item;
    }

    private static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory, UnaryOperator<Item.Properties> props) {
        DeferredItem<I> item = ITEMS.registerItem(name, factory, props);
        TAB_ORDER.add(item);
        return item;
    }

    private static DeferredItem<Item> armor(String name, ArmorType type) {
        return item(name, Item::new, p -> p.humanoidArmor(MOMaterials.TRITANIUM_ARMOR, type));
    }

    private static DeferredItem<BlockItem> block(String name, net.neoforged.neoforge.registries.DeferredBlock<?> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(name, block);
        TAB_ORDER.add(item);
        return item;
    }

    private MOItems() {}
}
