package matteroverdrive.fabric.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
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
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
        // 1.21.5+: a JSON model's geometry is its own object; the OBJ models keep the JSON's textures, display and
        // parent and get the OBJ geometry (children without elements inherit it, as on NeoForge)
        PreparableModelLoadingPlugin.register((state, executor) -> load(state.resourceManager(), executor), (models, context) ->
                context.modifyModelOnLoad().register((model, ctx) -> {
                    ObjModel obj = models.get(ctx.id());
                    return obj != null && model instanceof CuboidModel json ? new CuboidModel(new Geometry(obj), json.guiLight(),
                            json.ambientOcclusion(), json.transforms(), json.textureSlots(), json.parent()) : model;
                }));
    }

    /** Finds the mod's OBJ model JSONs and reads their OBJ files (off the main thread, on each resource reload) */
    private static CompletableFuture<Map<Identifier, ObjModel>> load(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Identifier, ObjModel> models = new HashMap<>();
            Map<Identifier, ObjFile> files = new HashMap<>();
            var found = manager.listResources("models", id -> id.getNamespace().equals(MatterOverdrive.MODID) && id.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("loader") || !json.get("loader").getAsString().equals("neoforge:obj")) continue;
                    Identifier file = Identifier.parse(json.get("model").getAsString());
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
                    Identifier id = entry.getKey().withPath(path.substring("models/".length(), path.length() - ".json".length()));
                    models.put(id, new ObjModel(obj, flipV, visibility));
                } catch (Exception e) {
                    MatterOverdrive.LOGGER.error("Could not load the OBJ model {}", entry.getKey(), e);
                }
            }
            return models;
        }, executor);
    }

    private static ObjFile readObj(ResourceManager manager, Identifier file) throws IOException {
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

    private static void readMtl(ResourceManager manager, Identifier file, Map<String, String> textures, Map<String, Integer> tints) throws IOException {
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

    /** The OBJ geometry, baked with the texture slots of the model that uses it */
    private record Geometry(ObjModel obj) implements UnbakedGeometry {
        @Override
        public QuadCollection bake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name) {
            ObjFile file = this.obj.obj();
            ModelBaker.Interner interner = baker.interner();
            Map<String, Material.Baked> materials = new HashMap<>();
            Function<String, Material.Baked> materialOf = material -> materials.computeIfAbsent(material, m -> {
                String slot = file.materialTextures().getOrDefault(m, "#texture");
                return baker.materials().resolveSlot(slots, slot.startsWith("#") ? slot.substring(1) : slot, name);
            });
            Map<String, BakedQuad.MaterialInfo> infos = new HashMap<>();
            // the block state's rotation turns the model about the block's centre
            Matrix4f transform = new Matrix4f();
            if (!state.transformation().equals(com.mojang.math.Transformation.IDENTITY)) {
                transform.translate(0.5f, 0.5f, 0.5f).mul(state.transformation().getMatrix()).translate(-0.5f, -0.5f, -0.5f);
            }
            var quads = new QuadCollection.Builder();
            for (Face face : file.faces()) {
                if (!this.obj.visibility().getOrDefault(face.group(), true)) continue;
                BakedQuad.MaterialInfo info = infos.computeIfAbsent(face.material(), m -> {
                    Material.Baked material = materialOf.apply(m);
                    Transparency transparency = material.forceTranslucent() ? Transparency.TRANSLUCENT
                            : material.sprite().contents().computeTransparency(0, 0, 1, 1);
                    return interner.materialInfo(BakedQuad.MaterialInfo.of(material, transparency, file.materialTints().getOrDefault(m, -1), true, 0));
                });
                // triangles become quads with a repeated vertex; larger polygons a fan of them
                for (int start = 1; start + 1 < face.positions().length; start += 2) {
                    int[] corners = start + 2 < face.positions().length ? new int[] {0, start, start + 1, start + 2} : new int[] {0, start, start + 1, start + 1};
                    this.quad(file, face, corners, info, transform, interner, quads);
                }
            }
            return quads.build();
        }

        private void quad(ObjFile file, Face face, int[] corners, BakedQuad.MaterialInfo info, Matrix4f transform, ModelBaker.Interner interner,
                          QuadCollection.Builder quads) {
            Vector3f[] points = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                Vector4f p = transform.transform(new Vector4f(file.positions().get(face.positions()[corners[i]]), 1));
                points[i] = new Vector3f(p.x, p.y, p.z);
            }
            Vector3f faceNormal = new Vector3f(points[1]).sub(points[0]).cross(new Vector3f(points[2]).sub(points[0]));
            if (faceNormal.lengthSquared() < 1.0E-12f) return;
            faceNormal.normalize();
            TextureAtlasSprite sprite = info.sprite();
            long[] uvs = new long[4];
            for (int i = 0; i < 4; i++) {
                int corner = corners[i];
                float u = 0;
                float v = 0;
                if (face.uvs()[corner] >= 0) {
                    float[] uv = file.uvs().get(face.uvs()[corner]);
                    u = uv[0];
                    v = this.obj.flipV() ? 1 - uv[1] : uv[1];
                }
                uvs[i] = UVPair.pack(sprite.getU(u), sprite.getV(v));
            }
            // 26.x quads carry no vertex normals: shading follows the quad's direction
            Direction direction = Direction.getApproximateNearest(faceNormal.x, faceNormal.y, faceNormal.z);
            BakedQuad quad = new BakedQuad(interner.vector(points[0]), interner.vector(points[1]), interner.vector(points[2]), interner.vector(points[3]),
                    uvs[0], uvs[1], uvs[2], uvs[3], direction, info);
            Direction cull = cullFace(points, direction);
            if (cull != null) quads.addCulledFace(cull, quad);
            else quads.addUnculledFace(quad);
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
