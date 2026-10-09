package matteroverdrive.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.WeaponBarrelItem;
import matteroverdrive.item.weapon.WeaponModule;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * 1.21.1: the item model properties behind the 1.21.10 client item definitions (tools/backport_1_21_1.py turns those
 * into model overrides): {@code matteroverdrive:barrel} (the installed barrel module, (ordinal + 1) / 10),
 * {@code matteroverdrive:security_type} (type / 10) and {@code matteroverdrive:linked} (a linked matter scanner), plus
 * the constant layer tints from {@code item_tints.json}.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class BarrelProperty {
    public static final ResourceLocation BARREL = id("barrel");
    public static final ResourceLocation SECURITY_TYPE = id("security_type");
    public static final ResourceLocation LINKED = id("linked");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (Item weapon : new Item[] {MOItems.PHASER.get(), MOItems.OMNI_TOOL.get()}) {
                ItemProperties.register(weapon, BARREL, (stack, level, entity, seed) ->
                        EnergyWeaponItem.getModule(stack, WeaponModule.SLOT_BARREL).getItem() instanceof WeaponBarrelItem barrel
                                ? (barrel.getType().ordinal() + 1) / 10f : 0);
            }
            ItemProperties.register(MOItems.SECURITY_PROTOCOL.get(), SECURITY_TYPE, (stack, level, entity, seed) ->
                    stack.getOrDefault(MODataComponents.SECURITY_TYPE.get(), 0) / 10f);
            ItemProperties.register(MOItems.MATTER_SCANNER.get(), LINKED, (stack, level, entity, seed) ->
                    stack.has(MODataComponents.SCANNER_LINK.get()) ? 1 : 0);
        });
    }

    /** item_tints.json: item id -> the ARGB tint of each layer (-1 = none). */
    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item event) {
        try (InputStream in = BarrelProperty.class.getResourceAsStream("/assets/" + MatterOverdrive.MODID + "/item_tints.json")) {
            if (in == null) return;
            JsonObject tints = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : tints.entrySet()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey()));
                var layers = entry.getValue().getAsJsonArray();
                int[] colors = new int[layers.size()];
                for (int i = 0; i < colors.length; i++) colors[i] = layers.get(i).getAsInt();
                event.register((stack, layer) -> layer < colors.length ? colors[layer] : -1, item);
            }
        } catch (Exception e) {
            MatterOverdrive.LOGGER.error("Could not read item_tints.json", e);
        }
    }

    private BarrelProperty() {}
}
