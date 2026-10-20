package matteroverdrive.init;

import java.util.Map;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public final class MOMaterials {
    public static final TagKey<Item> TRITANIUM_REPAIR = TagKey.create(Registries.ITEM, id("tritanium_tool_materials"));

    /** 1.7.10: addToolMaterial("tritanium", 2, 3122, 6f, 2f, 14) — iron harvest level. */
    public static final ToolMaterial TRITANIUM_TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 3122, 6f, 2f, 14, TRITANIUM_REPAIR);

    /** 1.21.2 - 1.21.3: the equipment model id (models/equipment/tritanium.json). */
    public static final ResourceLocation TRITANIUM_ASSET = id("tritanium");

    /** 1.7.10: addArmorMaterial("tritanium", 66, {4, 9, 7, 4}, 20) — helmet, chest, legs, boots. */
    public static final ArmorMaterial TRITANIUM_ARMOR = new ArmorMaterial(
            66,
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9, ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4, ArmorType.BODY, 9),
            20, SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, TRITANIUM_REPAIR, TRITANIUM_ASSET);

    /** 1.7.10 Tritanium_Armor2 layers: the boots use layer 2, everything else layer 1 (TritaniumArmorItem.getArmorTexture). */
    public static final net.minecraft.resources.ResourceLocation ARMOR_LAYER_1 = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/equipment/humanoid/tritanium.png");
    public static final net.minecraft.resources.ResourceLocation ARMOR_LAYER_2 = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/equipment/humanoid_leggings/tritanium.png");

    /** A tool tier (NeoForge's SimpleTier) */
    private record SimpleTier(net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> incorrectBlocksForDrops, int uses, float speed,
                              float attackDamageBonus, int enchantmentValue, java.util.function.Supplier<Ingredient> repair) implements Tier {
        @Override
        public int getUses() {
            return this.uses;
        }

        @Override
        public float getSpeed() {
            return this.speed;
        }

        @Override
        public float getAttackDamageBonus() {
            return this.attackDamageBonus;
        }

        @Override
        public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return this.incorrectBlocksForDrops;
        }

        @Override
        public int getEnchantmentValue() {
            return this.enchantmentValue;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repair.get();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private MOMaterials() {}
}
