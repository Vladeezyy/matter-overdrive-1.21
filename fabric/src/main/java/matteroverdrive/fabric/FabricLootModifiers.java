package matteroverdrive.fabric;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import matteroverdrive.MatterOverdrive;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;

/**
 * NeoForge's global loot modifiers of the mod (data/neoforge/loot_modifiers/global_loot_modifiers.json, all of type
 * neoforge:add_table matched by neoforge:loot_table_id, alone or in minecraft:any_of): on Fabric the matched tables get
 * one more pool that rolls the added table once, which is what add_table does.
 */
final class FabricLootModifiers {
    private FabricLootModifiers() {}

    static void register() {
        var mod = FabricLoader.getInstance().getModContainer(MatterOverdrive.MODID).orElseThrow();
        JsonObject global = read(mod.findPath("data/neoforge/loot_modifiers/global_loot_modifiers.json").orElseThrow());
        Map<ResourceKey<LootTable>, List<ResourceKey<LootTable>>> added = new HashMap<>();
        for (JsonElement entry : global.getAsJsonArray("entries")) {
            ResourceLocation id = new net.minecraft.resources.ResourceLocation(entry.getAsString());
            JsonObject json = read(mod.findPath("data/" + id.getNamespace() + "/loot_modifiers/" + id.getPath() + ".json").orElseThrow());
            if (!json.get("type").getAsString().equals("neoforge:add_table")) {
                throw new IllegalStateException("Unsupported loot modifier " + id + ": " + json.get("type"));
            }
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, new net.minecraft.resources.ResourceLocation(json.get("table").getAsString()));
            for (JsonElement condition : json.getAsJsonArray("conditions")) {
                collectTables(condition.getAsJsonObject(), id, target -> added.computeIfAbsent(target, key -> new java.util.ArrayList<>()).add(table));
            }
        }
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            List<ResourceKey<LootTable>> tables = added.get(key);
            if (tables == null) return;
            for (ResourceKey<LootTable> table : tables) builder.withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(table)));
        });
    }

    private static void collectTables(JsonObject condition, ResourceLocation id, java.util.function.Consumer<ResourceKey<LootTable>> target) {
        String type = condition.get("condition").getAsString();
        switch (type) {
            case "neoforge:loot_table_id" -> target.accept(ResourceKey.create(Registries.LOOT_TABLE, new net.minecraft.resources.ResourceLocation(condition.get("loot_table_id").getAsString())));
            case "minecraft:any_of" -> condition.getAsJsonArray("terms").forEach(term -> collectTables(term.getAsJsonObject(), id, target));
            default -> throw new IllegalStateException("Unsupported condition in loot modifier " + id + ": " + type);
        }
    }

    private static JsonObject read(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
    }
}
