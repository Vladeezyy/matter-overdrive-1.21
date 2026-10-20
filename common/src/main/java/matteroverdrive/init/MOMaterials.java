package matteroverdrive.init;

import java.util.List;
import java.util.Map;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

public final class MOMaterials {
    public static final Registrar<ArmorMaterial> ARMOR_MATERIALS = Registrar.create(Registries.ARMOR_MATERIAL, MatterOverdrive.MODID);

    public static final TagKey<Item> TRITANIUM_REPAIR = TagKey.create(Registries.ITEM, id("tritanium_tool_materials"));

    /** 1.7.10: addToolMaterial("tritanium", 2, 3122, 6f, 2f, 14) — iron harvest level. */
    public static final Tier TRITANIUM_TOOL = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 3122, 6f, 2f, 14, () -> Ingredient.of(TRITANIUM_REPAIR));

    /** 1.7.10 armor durability factor (66). */
    public static final int TRITANIUM_ARMOR_DURABILITY = 66;

    /** 1.7.10: addArmorMaterial("tritanium", 66, {4, 9, 7, 4}, 20) — helmet, chest, legs, boots. */
    public static final RegistryEntry<ArmorMaterial, ArmorMaterial> TRITANIUM_ARMOR = ARMOR_MATERIALS.register("tritanium", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 9, ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 4, ArmorItem.Type.BODY, 9),
            20, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(TRITANIUM_REPAIR),
            List.of(new ArmorMaterial.Layer(id("tritanium"))), 0f, 0f));

    /** 1.7.10 Tritanium_Armor2 layers: the boots use layer 2, everything else layer 1 (TritaniumArmorItem.getArmorTexture). */
    public static final ResourceLocation ARMOR_LAYER_1 = id("textures/entity/equipment/humanoid/tritanium.png");
    public static final ResourceLocation ARMOR_LAYER_2 = id("textures/entity/equipment/humanoid_leggings/tritanium.png");

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
