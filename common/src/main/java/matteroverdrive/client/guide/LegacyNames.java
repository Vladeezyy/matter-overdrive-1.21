package matteroverdrive.client.guide;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7.10 guide shortcodes name items by their 1.7.10 registry name and damage value: the matching items now. */
public final class LegacyNames {
    static String file(String id) {
        return id;
    }

    static Item[] upgrades() {
        return new Item[] {MOItems.UPGRADE_BASE.get(), MOItems.UPGRADE_SPEED.get(), MOItems.UPGRADE_POWER.get(), MOItems.UPGRADE_FAILSAFE.get(),
                MOItems.UPGRADE_RANGE.get(), MOItems.UPGRADE_POWER_STORAGE.get(), MOItems.UPGRADE_HYPER_SPEED.get(), MOItems.UPGRADE_MATTER_STORAGE.get()};
    }

    /** 1.7.10 GuideElementAbstract.shortCodeToStack: mod (default "mo"), name, damage. */
    public static @Nullable ItemStack stack(String mod, String name, int damage) {
        Item item = switch (name) {
            case "recycler" -> BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "matter_recycler"));
            case "upgrade" -> upgrades()[Math.clamp(damage, 0, 7)];
            case "rouge_android_part" -> new Item[] {MOItems.ROGUE_ANDROID_HEAD.get(), MOItems.ROGUE_ANDROID_ARMS.get(), MOItems.ROGUE_ANDROID_LEGS.get(),
                    MOItems.ROGUE_ANDROID_CHEST.get()}[Math.clamp(damage, 0, 3)];
            case "android_pill" -> new Item[] {MOItems.ANDROID_PILL_RED.get(), MOItems.ANDROID_PILL_BLUE.get(), MOItems.ANDROID_PILL_YELLOW.get()}[
                    Math.clamp(damage, 0, 2)];
            case "weapon_module_barrel" -> new Item[] {MOItems.BARREL_DAMAGE.get(), MOItems.BARREL_FIRE.get(), MOItems.BARREL_EXPLOSION.get(),
                    MOItems.BARREL_HEAL.get()}[Math.clamp(damage, 0, 3)];
            case "isolinear_circuit" -> BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID,
                    "isolinear_circuit_mk" + (Math.clamp(damage, 0, 3) + 1)));
            default -> {
                String namespace = mod.equals("mo") ? MatterOverdrive.MODID : mod;
                ResourceLocation id = ResourceLocation.tryBuild(namespace, name);
                yield id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
            }
        };
        return item == null || item == Items.AIR ? null : new ItemStack(item);
    }

    private LegacyNames() {}
}
