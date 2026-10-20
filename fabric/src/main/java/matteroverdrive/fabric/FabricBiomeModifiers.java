package matteroverdrive.fabric;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import matteroverdrive.MatterOverdrive;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * The NeoForge biome modifiers (data/matteroverdrive/neoforge/biome_modifier) applied with the Fabric BiomeModifications: the
 * same files, read from the mod jar. Supports neoforge:add_features and neoforge:add_spawns with biomes neoforge:any, neoforge:and,
 * neoforge:not, a tag or a biome id (what the mod uses; from the Rats port).
 */
final class FabricBiomeModifiers {
    private static final String DIR = "data/" + MatterOverdrive.MODID + "/neoforge/biome_modifier";

    private FabricBiomeModifiers() {}

    static void register() {
        Path dir = FabricLoader.getInstance().getModContainer(MatterOverdrive.MODID).orElseThrow().findPath(DIR).orElseThrow();
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException("Could not list " + DIR, e);
        }
        for (Path file : files) {
            JsonObject json;
            try (Reader reader = Files.newBufferedReader(file)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + file, e);
            }
            String name = file.getFileName().toString().replace(".json", "");
            apply(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, name), json);
        }
    }

    private static void apply(ResourceLocation id, JsonObject json) {
        if (json.has("neoforge:conditions")) {
            throw new IllegalStateException("Unsupported conditions in " + id);
        }
        Predicate<BiomeSelectionContext> biomes = biomes(json.get("biomes"));
        String type = json.get("type").getAsString();
        switch (type) {
            case "neoforge:add_features" -> {
                GenerationStep.Decoration step = GenerationStep.Decoration.valueOf(json.get("step").getAsString().toUpperCase(java.util.Locale.ROOT));
                List<ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature>> features = strings(json.get("features")).stream()
                        .map(f -> ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse(f))).toList();
                BiomeModifications.create(id).add(ModificationPhase.ADDITIONS, biomes, context -> {
                    features.forEach(feature -> context.getGenerationSettings().addFeature(step, feature));
                });
            }
            case "neoforge:add_spawns" -> {
                List<JsonObject> spawners = new ArrayList<>();
                JsonElement element = json.get("spawners");
                if (element.isJsonArray()) {
                    element.getAsJsonArray().forEach(e -> spawners.add(e.getAsJsonObject()));
                } else {
                    spawners.add(element.getAsJsonObject());
                }
                BiomeModifications.create(id).add(ModificationPhase.ADDITIONS, biomes, context -> {
                    spawners.forEach(spawner -> addSpawn(context, spawner));
                });
            }
            default -> throw new IllegalStateException("Unsupported biome modifier type in " + id + ": " + type);
        }
    }

    private static void addSpawn(BiomeModificationContext context, JsonObject spawner) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(spawner.get("type").getAsString()));
        context.getSpawnSettings().addSpawn(type.getCategory(), new MobSpawnSettings.SpawnerData(type, spawner.get("weight").getAsInt(),
                spawner.get("minCount").getAsInt(), spawner.get("maxCount").getAsInt()));
    }

    private static Predicate<BiomeSelectionContext> biomes(JsonElement biomes) {
        if (biomes.isJsonArray()) {
            Predicate<BiomeSelectionContext> any = context -> false;
            for (JsonElement each : biomes.getAsJsonArray()) any = any.or(biomes(each));
            return any;
        }
        if (biomes.isJsonObject()) {
            JsonObject object = biomes.getAsJsonObject();
            switch (object.get("type").getAsString()) {
                case "neoforge:any" -> {
                    return BiomeSelectors.all();
                }
                case "neoforge:and" -> {
                    Predicate<BiomeSelectionContext> all = context -> true;
                    for (JsonElement each : object.getAsJsonArray("values")) all = all.and(biomes(each));
                    return all;
                }
                case "neoforge:or" -> {
                    return biomes(object.get("values"));
                }
                case "neoforge:not" -> {
                    return biomes(object.get("value")).negate();
                }
                default -> throw new IllegalStateException("Unsupported biomes: " + biomes);
            }
        }
        String value = biomes.getAsString();
        if (value.startsWith("#")) {
            return BiomeSelectors.tag(TagKey.create(Registries.BIOME, ResourceLocation.parse(value.substring(1))));
        }
        return BiomeSelectors.includeByKey(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(value)));
    }

    private static List<String> strings(JsonElement element) {
        if (element.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (JsonElement e : (JsonArray) element) {
                list.add(e.getAsString());
            }
            return list;
        }
        return List.of(element.getAsString());
    }
}
