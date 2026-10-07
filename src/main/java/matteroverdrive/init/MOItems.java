package matteroverdrive.init;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import matteroverdrive.MatterOverdrive;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
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
    public static final DeferredItem<Item> MATTER_DUST = simple("matter_dust");
    public static final DeferredItem<Item> MATTER_DUST_REFINED = simple("matter_dust_refined");
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
