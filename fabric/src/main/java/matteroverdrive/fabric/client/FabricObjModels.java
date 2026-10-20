package matteroverdrive.fabric.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import matteroverdrive.MatterOverdrive;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * NeoForge's {@code neoforge:obj} model loader for the mod's models on Fabric: the same JSON files (model, flip_v,
 * textures, visibility, display, overrides) and Wavefront files (v / vt / vn / f, g groups, usemtl, the mtl's
 * {@code map_Kd #texture} and {@code neoforge_TintIndex}). The models bake to plain vanilla models once per resource
 * reload, so drawing them costs what a JSON model costs.
 */
public final class FabricObjModels {
    private FabricObjModels() {}

    /** One face: positions, texture coordinates and normals (indices into the file's lists), its group and material */
    private record Face(int[] positions, int[] uvs, int[] normals, String group, String material) {}

    /** A parsed OBJ file */
    private record ObjFile(List<Vector3f> positions, List<float[]> uvs, List<Vector3f> normals, List<Face> faces,
                           Map<String, String> materialTextures, Map<String, Integer> materialTints) {}

    /** What the JSON adds to the OBJ file */
    private record ObjModel(ObjFile obj, boolean flipV, Map<String, Boolean> visibility) {}

    static void register() {
        // before baking, not on load: vanilla resolves parents of JSON models (item models of OBJ blocks) as BlockModels
        PreparableModelLoadingPlugin.register(FabricObjModels::load, (models, context) ->
                context.modifyModelBeforeBake().register((model, ctx) -> {
                    // the model or the nearest parent with OBJ geometry (NeoForge: children inherit the custom geometry)
                    for (BlockModel json = model instanceof BlockModel block ? block : null; json != null; json = json.parent) {
                        ObjModel obj = models.get(ResourceLocation.tryParse(json.name));
                        if (obj != null) return new Unbaked((BlockModel) model, obj);
                    }
                    return model;
                }));
    }

    /** Finds the mod's OBJ model JSONs and reads their OBJ files (off the main thread, on each resource reload) */
    private static CompletableFuture<Map<ResourceLocation, ObjModel>> load(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, ObjModel> models = new HashMap<>();
            Map<ResourceLocation, ObjFile> files = new HashMap<>();
            var found = manager.listResources("models", id -> id.getNamespace().equals(MatterOverdrive.MODID) && id.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("loader") || !json.get("loader").getAsString().equals("neoforge:obj")) continue;
                    ResourceLocation file = ResourceLocation.parse(json.get("model").getAsString());
                    ObjFile obj = files.get(file);
                    if (obj == null) {
                        obj = readObj(manager, file);
                        files.put(file, obj);
                    }
                    Map<String, Boolean> visibility = new HashMap<>();
                    if (json.has("visibility")) {
                        for (Map.Entry<String, JsonElement> part : json.getAsJsonObject("visibility").entrySet()) visibility.put(part.getKey(), part.getValue().getAsBoolean());
                    }
                    boolean flipV = json.has("flip_v") && json.get("flip_v").getAsBoolean();
                    // models/block/x.json → block/x, the id vanilla loads the model by
                    String path = entry.getKey().getPath();
                    ResourceLocation id = entry.getKey().withPath(path.substring("models/".length(), path.length() - ".json".length()));
                    models.put(id, new ObjModel(obj, flipV, visibility));
                } catch (Exception e) {
                    MatterOverdrive.LOGGER.error("Could not load the OBJ model {}", entry.getKey(), e);
                }
            }
            return models;
        }, executor);
    }

    private static ObjFile readObj(ResourceManager manager, ResourceLocation file) throws IOException {
        List<Vector3f> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        Map<String, String> textures = new HashMap<>();
        Map<String, Integer> tints = new HashMap<>();
        String group = "";
        String material = "";
        try (BufferedReader reader = manager.openAsReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                    case "vt" -> uvs.add(new float[] {Float.parseFloat(parts[1]), parts.length > 2 ? Float.parseFloat(parts[2]) : 0});
                    case "vn" -> normals.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                    case "g", "o" -> group = parts.length > 1 ? parts[1] : "";
                    case "usemtl" -> material = parts[1];
                    case "mtllib" -> readMtl(manager, file.withPath(file.getPath().substring(0, file.getPath().lastIndexOf('/') + 1) + parts[1]), textures, tints);
                    case "f" -> {
                        int count = parts.length - 1;
                        int[] p = new int[count];
                        int[] t = new int[count];
                        int[] n = new int[count];
                        for (int i = 0; i < count; i++) {
                            String[] index = parts[i + 1].split("/");
                            p[i] = index(index[0], positions.size());
                            t[i] = index.length > 1 && !index[1].isEmpty() ? index(index[1], uvs.size()) : -1;
                            n[i] = index.length > 2 && !index[2].isEmpty() ? index(index[2], normals.size()) : -1;
                        }
                        faces.add(new Face(p, t, n, group, material));
                    }
                    default -> {
                    }
                }
            }
        }
        return new ObjFile(positions, uvs, normals, faces, textures, tints);
    }

    /** 1-based, or negative from the end */
    private static int index(String value, int size) {
        int index = Integer.parseInt(value);
        return index < 0 ? size + index : index - 1;
    }

    private static void readMtl(ResourceManager manager, ResourceLocation file, Map<String, String> textures, Map<String, Integer> tints) throws IOException {
        try (BufferedReader reader = manager.openAsReader(file)) {
            String material = "";
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "newmtl" -> material = parts[1];
                    case "map_Kd" -> textures.put(material, parts[1]);
                    case "neoforge_TintIndex" -> tints.put(material, Integer.parseInt(parts[1]));
                    default -> {
                    }
                }
            }
        }
    }

    /** The JSON model (textures, display, overrides) with the OBJ geometry */
    private record Unbaked(BlockModel json, ObjModel obj) implements UnbakedModel {
        @Override
        public void resolveDependencies(UnbakedModel.Resolver resolver) {
            this.json.resolveDependencies(resolver);
        }

        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state) {
            ObjFile file = this.obj.obj();
            Map<String, TextureAtlasSprite> materialSprites = new HashMap<>();
            Function<String, TextureAtlasSprite> spriteOf = material -> materialSprites.computeIfAbsent(material,
                    m -> sprites.apply(this.json.getMaterial(file.materialTextures().getOrDefault(m, "#texture"))));
            // the block state's rotation turns the model about the block's centre
            Matrix4f transform = new Matrix4f();
            if (!state.getRotation().equals(com.mojang.math.Transformation.identity())) {
                transform.translate(0.5f, 0.5f, 0.5f).mul(state.getRotation().getMatrix()).translate(-0.5f, -0.5f, -0.5f);
            }
            List<BakedQuad> unculled = new ArrayList<>();
            Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) culled.put(direction, new ArrayList<>());
            for (Face face : file.faces()) {
                if (!this.obj.visibility().getOrDefault(face.group(), true)) continue;
                TextureAtlasSprite sprite = spriteOf.apply(face.material());
                int tint = file.materialTints().getOrDefault(face.material(), -1);
                // triangles become quads with a repeated vertex; larger polygons a fan of them
                for (int start = 1; start + 1 < face.positions().length; start += 2) {
                    int[] corners = start + 2 < face.positions().length ? new int[] {0, start, start + 1, start + 2} : new int[] {0, start, start + 1, start + 1};
                    this.quad(file, face, corners, sprite, tint, transform, unculled, culled);
                }
            }
            // 1.21.2+: item overrides are the item model's (ItemModel), not the baked block model's
            return new SimpleBakedModel(unculled, culled, this.json.hasAmbientOcclusion(), this.json.getGuiLight().lightLikeBlock(), true,
                    sprites.apply(this.json.getMaterial("particle")), this.json.getTransforms());
        }

        private void quad(ObjFile file, Face face, int[] corners, TextureAtlasSprite sprite, int tint, Matrix4f transform,
                          List<BakedQuad> unculled, Map<Direction, List<BakedQuad>> culled) {
            Vector3f[] points = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                Vector4f p = transform.transform(new Vector4f(file.positions().get(face.positions()[corners[i]]), 1));
                points[i] = new Vector3f(p.x, p.y, p.z);
            }
            Vector3f faceNormal = new Vector3f(points[1]).sub(points[0]).cross(new Vector3f(points[2]).sub(points[0]));
            if (faceNormal.lengthSquared() < 1.0E-12f) return;
            faceNormal.normalize();
            int[] data = new int[32];
            for (int i = 0; i < 4; i++) {
                int corner = corners[i];
                float u = 0;
                float v = 0;
                if (face.uvs()[corner] >= 0) {
                    float[] uv = file.uvs().get(face.uvs()[corner]);
                    u = uv[0];
                    v = this.obj.flipV() ? 1 - uv[1] : uv[1];
                }
                Vector3f normal = faceNormal;
                if (face.normals()[corner] >= 0) {
                    normal = transform.transformDirection(new Vector3f(file.normals().get(face.normals()[corner]))).normalize();
                }
                int offset = i * 8;
                data[offset] = Float.floatToRawIntBits(points[i].x);
                data[offset + 1] = Float.floatToRawIntBits(points[i].y);
                data[offset + 2] = Float.floatToRawIntBits(points[i].z);
                data[offset + 3] = -1;
                data[offset + 4] = Float.floatToRawIntBits(sprite.getU(u));
                data[offset + 5] = Float.floatToRawIntBits(sprite.getV(v));
                data[offset + 6] = 0;
                data[offset + 7] = ((int) (normal.x * 127) & 0xFF) | ((int) (normal.y * 127) & 0xFF) << 8 | ((int) (normal.z * 127) & 0xFF) << 16;
            }
            Direction direction = Direction.getApproximateNearest(faceNormal.x, faceNormal.y, faceNormal.z);
            BakedQuad quad = new BakedQuad(data, tint, direction, sprite, true, 0);
            Direction cull = cullFace(points, direction);
            if (cull != null) culled.get(cull).add(quad);
            else unculled.add(quad);
        }

        /** NeoForge's automatic culling: a quad lying in a face of the block, facing out of it, is hidden by a full neighbour there */
        private static Direction cullFace(Vector3f[] points, Direction facing) {
            float plane = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
            for (Vector3f point : points) {
                float value = switch (facing.getAxis()) {
                    case X -> point.x;
                    case Y -> point.y;
                    case Z -> point.z;
                };
                if (Math.abs(value - plane) > 1.0E-4f) return null;
            }
            return facing;
        }
    }
}
