package matteroverdrive.init;

import java.util.Map;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public final class MOMaterials {
    public static final TagKey<Item> TRITANIUM_REPAIR = TagKey.create(Registries.ITEM, id("tritanium_tool_materials"));

    /** 1.7.10: addToolMaterial("tritanium", 2, 3122, 6f, 2f, 14) — iron harvest level. */
    public static final Tier TRITANIUM_TOOL = new SimpleTier(2, 3122, 6f, 2f, 14, () -> Ingredient.of(TRITANIUM_REPAIR));

    /** 1.7.10 armor durability factor (66). */
    public static final int TRITANIUM_ARMOR_DURABILITY = 66;

    /** 1.7.10: addArmorMaterial("tritanium", 66, {4, 9, 7, 4}, 20) — helmet, chest, legs, boots. */
    public static final ArmorMaterial TRITANIUM_ARMOR = new ArmorMaterial() {
        private static final Map<ArmorItem.Type, Integer> DEFENSE =
                Map.of(ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 9, ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 4);
        /** vanilla 1.20.1 ArmorMaterials.HEALTH_FUNCTION_FOR_TYPE */
        private static final Map<ArmorItem.Type, Integer> HEALTH =
                Map.of(ArmorItem.Type.HELMET, 11, ArmorItem.Type.CHESTPLATE, 16, ArmorItem.Type.LEGGINGS, 15, ArmorItem.Type.BOOTS, 13);

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return HEALTH.get(type) * TRITANIUM_ARMOR_DURABILITY;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return DEFENSE.get(type);
        }

        @Override
        public int getEnchantmentValue() {
            return 20;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(TRITANIUM_REPAIR);
        }

        @Override
        public String getName() {
            return MatterOverdrive.MODID + ":tritanium";
        }

        @Override
        public float getToughness() {
            return 0f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    };

    /** 1.7.10 Tritanium_Armor2 layers: the boots use layer 2, everything else layer 1 (TritaniumArmorItem.getArmorTexture). */
    public static final ResourceLocation ARMOR_LAYER_1 = id("textures/entity/equipment/humanoid/tritanium.png");
    public static final ResourceLocation ARMOR_LAYER_2 = id("textures/entity/equipment/humanoid_leggings/tritanium.png");

    /** A tool tier (Forge's ForgeTier) */
    private record SimpleTier(int level, int uses, float speed, float attackDamageBonus, int enchantmentValue,
                              java.util.function.Supplier<Ingredient> repair) implements Tier {
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
        public int getLevel() {
            return this.level;
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
        return new net.minecraft.resources.ResourceLocation(MatterOverdrive.MODID, path);
    }

    private MOMaterials() {}
}
