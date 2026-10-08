package matteroverdrive.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * A triangulated Wavefront OBJ (positions + uvs) drawn as custom geometry, like 1.7.10 IModelCustom.renderAll.
 * Triangles go out as degenerate quads (the holo pipelines take quads); v is flipped for the 1.7.10 textures.
 */
public final class ObjMesh {
    private final float[] positions;
    private final float[] uvs;

    private ObjMesh(float[] positions, float[] uvs) {
        this.positions = positions;
        this.uvs = uvs;
    }

    public static ObjMesh load(ResourceLocation location) {
        List<float[]> v = new ArrayList<>(), vt = new ArrayList<>();
        List<Float> pos = new ArrayList<>(), uv = new ArrayList<>();
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> v.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                    case "vt" -> vt.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
                    case "f" -> {
                        // fan-triangulate, then emit each triangle
                        for (int i = 2; i + 1 < parts.length; i++) {
                            for (String corner : new String[] {parts[1], parts[i], parts[i + 1]}) {
                                String[] idx = corner.split("/");
                                float[] p = v.get(Integer.parseInt(idx[0]) - 1);
                                float[] t = idx.length > 1 && !idx[1].isEmpty() ? vt.get(Integer.parseInt(idx[1]) - 1) : new float[2];
                                pos.add(p[0]);
                                pos.add(p[1]);
                                pos.add(p[2]);
                                uv.add(t[0]);
                                uv.add(1 - t[1]);
                            }
                        }
                    }
                    default -> {}
                }
            }
        } catch (IOException | RuntimeException e) {
            MatterOverdrive.LOGGER.error("Could not load {}", location, e);
        }
        float[] p = new float[pos.size()], t = new float[uv.size()];
        for (int i = 0; i < p.length; i++) p[i] = pos.get(i);
        for (int i = 0; i < t.length; i++) t[i] = uv.get(i);
        return new ObjMesh(p, t);
    }

    /** Every triangle in the given colour (ARGB). */
    public void render(VertexConsumer vc, PoseStack.Pose pose, int color) {
        for (int tri = 0; tri < positions.length / 9; tri++) {
            for (int corner = 0; corner < 4; corner++) {
                int i = tri * 3 + Math.min(corner, 2);
                vc.addVertex(pose, positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]).setUv(uvs[i * 2], uvs[i * 2 + 1]).setColor(color);
            }
        }
    }
}
