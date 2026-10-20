package matteroverdrive.fabric;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import matteroverdrive.MatterOverdrive;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The {@code matteroverdrive:matter} item data map on Fabric: the same {@code data/<namespace>/data_maps/item/matter.json}
 * files, merged as NeoForge does (packs in order, {@code "replace": true} drops what lower packs gave, then
 * {@code "remove"}). Tags are resolved on first use after a reload, when the server's tags are bound.
 */
final class FabricMatterValues {
    private static final String PATH = "data_maps/item/matter.json";
    /** Entries in load order: "#tag" or item id → value */
    private static volatile Map<String, Integer> entries = Map.of();
    private static volatile List<String> removed = List.of();
    private static volatile @Nullable Map<Item, Integer> resolved;

    private FabricMatterValues() {}

    static void register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "matter_values");
            }

            @Override
            public java.util.Collection<ResourceLocation> getFabricDependencies() {
                return List.of(net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys.TAGS);
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                load(manager);
            }
        });
    }

    private static void load(ResourceManager manager) {
        Map<String, Integer> values = new LinkedHashMap<>();
        List<String> remove = new ArrayList<>();
        for (String namespace : manager.getNamespaces()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, PATH);
            for (Resource resource : manager.getResourceStack(id)) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("replace") && json.get("replace").getAsBoolean()) values.clear();
                    if (json.has("values")) {
                        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("values").entrySet()) {
                            JsonElement value = entry.getValue();
                            if (value.isJsonObject()) {
                                JsonObject object = value.getAsJsonObject();
                                if (object.has("replace") && object.get("replace").getAsBoolean()) values.remove(entry.getKey());
                                value = object.get("value");
                            }
                            values.put(entry.getKey(), value.getAsInt());
                        }
                    }
                    if (json.has("remove")) json.getAsJsonArray("remove").forEach(e -> remove.add(e.getAsString()));
                } catch (Exception e) {
                    MatterOverdrive.LOGGER.error("Could not read matter values {} from {}", id, resource.sourcePackId(), e);
                }
            }
        }
        entries = values;
        removed = remove;
        resolved = null;
    }

    static @Nullable Integer get(Item item) {
        Map<Item, Integer> map = resolved;
        if (map == null) {
            map = resolve();
            resolved = map;
        }
        return map.get(item);
    }

    private static Map<Item, Integer> resolve() {
        Map<Item, Integer> map = new HashMap<>();
        // in file order, later entries over earlier ones (as NeoForge resolves them)
        entries.forEach((key, value) -> {
            if (key.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(key.substring(1)));
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) map.put(holder.value(), value);
            } else {
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(key)).ifPresent(item -> map.put(item, value));
            }
        });
        for (String key : removed) {
            if (key.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(key.substring(1)));
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) map.remove(holder.value());
            } else {
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(key)).ifPresent(map::remove);
            }
        }
        return map;
    }
}
