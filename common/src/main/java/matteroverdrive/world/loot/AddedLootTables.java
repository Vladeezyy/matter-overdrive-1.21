package matteroverdrive.world.loot;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * The mod's 1.21 NeoForge global loot modifiers (data/neoforge/loot_modifiers/global_loot_modifiers.json, all of type
 * neoforge:add_table matched by neoforge:loot_table_id, alone or in minecraft:any_of), read from the mod jar. 1.20.1 Forge
 * has no add_table and Fabric no loot modifiers: both loaders give each matched table one more pool that rolls the added
 * table once, which is what add_table does.
 */
public final class AddedLootTables {
    private AddedLootTables() {}

    /** Target table → the tables added to it */
    public static Map<ResourceLocation, List<ResourceLocation>> load() {
        JsonObject global = read(matteroverdrive.platform.Platform.INSTANCE.modFile("data", "neoforge", "loot_modifiers", "global_loot_modifiers.json"));
        Map<ResourceLocation, List<ResourceLocation>> added = new HashMap<>();
        for (JsonElement entry : global.getAsJsonArray("entries")) {
            ResourceLocation id = new ResourceLocation(entry.getAsString());
            JsonObject json = read(matteroverdrive.platform.Platform.INSTANCE.modFile("data", id.getNamespace(), "loot_modifiers", id.getPath() + ".json"));
            if (!json.get("type").getAsString().equals("neoforge:add_table")) {
                throw new IllegalStateException("Unsupported loot modifier " + id + ": " + json.get("type"));
            }
            ResourceLocation table = new ResourceLocation(json.get("table").getAsString());
            for (JsonElement condition : json.getAsJsonArray("conditions")) {
                collectTables(condition.getAsJsonObject(), id, target -> added.computeIfAbsent(target, key -> new ArrayList<>()).add(table));
            }
        }
        return added;
    }

    private static void collectTables(JsonObject condition, ResourceLocation id, Consumer<ResourceLocation> target) {
        String type = condition.get("condition").getAsString();
        switch (type) {
            case "neoforge:loot_table_id" -> target.accept(new ResourceLocation(condition.get("loot_table_id").getAsString()));
            case "minecraft:any_of" -> condition.getAsJsonArray("terms").forEach(term -> collectTables(term.getAsJsonObject(), id, target));
            default -> throw new IllegalStateException("Unsupported condition in loot modifier " + id + ": " + type);
        }
    }

    private static JsonObject read(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return GsonHelper.parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
    }
}
