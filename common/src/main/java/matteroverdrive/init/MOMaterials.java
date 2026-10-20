package matteroverdrive.init;

import java.util.Map;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class MOMaterials {
    public static final TagKey<Item> TRITANIUM_REPAIR = TagKey.create(Registries.ITEM, id("tritanium_tool_materials"));

    /** 1.7.10: addToolMaterial("tritanium", 2, 3122, 6f, 2f, 14) — iron harvest level. */
    public static final ToolMaterial TRITANIUM_TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 3122, 6f, 2f, 14, TRITANIUM_REPAIR);

    public static final ResourceKey<EquipmentAsset> TRITANIUM_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, id("tritanium"));

    /** 1.7.10: addArmorMaterial("tritanium", 66, {4, 9, 7, 4}, 20) — helmet, chest, legs, boots. */
    public static final ArmorMaterial TRITANIUM_ARMOR = new ArmorMaterial(
            66,
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9, ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4, ArmorType.BODY, 9),
            20, SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, TRITANIUM_REPAIR, TRITANIUM_ASSET);

    /** 1.7.10 Tritanium_Armor2 layers: the boots use layer 2, everything else layer 1 (TritaniumArmorItem.getArmorTexture). */
    public static final net.minecraft.resources.Identifier ARMOR_LAYER_1 = net.minecraft.resources.Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/equipment/humanoid/tritanium.png");
    public static final net.minecraft.resources.Identifier ARMOR_LAYER_2 = net.minecraft.resources.Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/equipment/humanoid_leggings/tritanium.png");

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private MOMaterials() {}
}
