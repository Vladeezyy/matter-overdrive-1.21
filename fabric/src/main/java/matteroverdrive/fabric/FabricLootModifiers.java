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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;

/**
 * NeoForge's global loot modifiers of the mod (data/matteroverdrive/loot_modifiers/*.json, all of type
 * neoforge:add_table matched by neoforge:loot_table_id, alone or in minecraft:any_of): on Fabric the matched tables get
 * one more pool that rolls the added table once, which is what add_table does.
 */
final class FabricLootModifiers {
    private FabricLootModifiers() {}

    static void register() {
        var mod = FabricLoader.getInstance().getModContainer(MatterOverdrive.MODID).orElseThrow();
        // 26.x NeoForge loads every file of data/<ns>/loot_modifiers (the global list file is gone)
        List<Path> files;
        try (var list = Files.list(mod.findPath("data/" + MatterOverdrive.MODID + "/loot_modifiers").orElseThrow())) {
            files = list.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException("Could not list the loot modifiers", e);
        }
        Map<ResourceKey<LootTable>, List<ResourceKey<LootTable>>> added = new HashMap<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            Identifier id = Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, name.substring(0, name.length() - ".json".length()));
            JsonObject json = read(file);
            if (!json.get("type").getAsString().equals("neoforge:add_table")) {
                throw new IllegalStateException("Unsupported loot modifier " + id + ": " + json.get("type"));
            }
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(json.get("table").getAsString()));
            // 26.3: one typed condition (was a list of "condition" objects)
            collectTables(json.getAsJsonObject("condition"), id, target -> added.computeIfAbsent(target, key -> new java.util.ArrayList<>()).add(table));
        }
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            List<ResourceKey<LootTable>> tables = added.get(key);
            if (tables == null) return;
            // 26.3: loot tables are registry entries, the nested entry takes the holder
            var lootTables = registries.lookupOrThrow(Registries.LOOT_TABLE);
            for (ResourceKey<LootTable> table : tables) {
                lootTables.get(table).ifPresentOrElse(holder -> builder.withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(holder))),
                        () -> MatterOverdrive.LOGGER.warn("Loot modifier table {} is missing", table));
            }
        });
    }

    private static void collectTables(JsonObject condition, Identifier id, java.util.function.Consumer<ResourceKey<LootTable>> target) {
        String type = condition.get("type").getAsString();
        switch (type) {
            case "neoforge:loot_table_id" -> target.accept(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(condition.get("loot_table_id").getAsString())));
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
