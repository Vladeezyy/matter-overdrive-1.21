package matteroverdrive.compat.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * The 1.21 vertex calls ({@code addVertex(pose, x, y, z).setColor(..).setUv(..)...}) on a 1.20.1 {@link VertexConsumer}.
 * A 1.20.1 buffer takes the elements only in its format's order and silently skips the others, while the 1.21 calls
 * come in any order: this collects them and {@link #end()} writes them in format order. The colour goes both before and
 * after the texture coordinates, so it lands in POSITION_COLOR_TEX and POSITION_TEX_COLOR alike (the other call is skipped).
 */
public final class Vertex {
    private static final Vertex INSTANCE = new Vertex();

    private VertexConsumer consumer;
    private float x, y, z;
    private boolean hasColor, hasUv, hasOverlay, hasLight, hasNormal;
    private int r, g, b, a, overlay, light;
    private float u, v, nx, ny, nz;

    private Vertex() {}

    /** Render thread only (one vertex at a time, like the 1.21 buffers) */
    public static Vertex of(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z) {
        Vector4f p = matrix.transform(new Vector4f(x, y, z, 1));
        Vertex vertex = INSTANCE;
        vertex.consumer = consumer;
        vertex.x = p.x();
        vertex.y = p.y();
        vertex.z = p.z();
        vertex.hasColor = vertex.hasUv = vertex.hasOverlay = vertex.hasLight = vertex.hasNormal = false;
        return vertex;
    }

    public static Vertex of(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z) {
        return of(consumer, pose.pose(), x, y, z);
    }

    public static Vertex of(VertexConsumer consumer, float x, float y, float z) {
        Vertex vertex = INSTANCE;
        vertex.consumer = consumer;
        vertex.x = x;
        vertex.y = y;
        vertex.z = z;
        vertex.hasColor = vertex.hasUv = vertex.hasOverlay = vertex.hasLight = vertex.hasNormal = false;
        return vertex;
    }

    public Vertex setColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        hasColor = true;
        return this;
    }

    public Vertex setColor(float r, float g, float b, float a) {
        return setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
    }

    public Vertex setColor(int argb) {
        return setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    public Vertex setUv(float u, float v) {
        this.u = u;
        this.v = v;
        hasUv = true;
        return this;
    }

    public Vertex setOverlay(int overlay) {
        this.overlay = overlay;
        hasOverlay = true;
        return this;
    }

    public Vertex setLight(int light) {
        this.light = light;
        hasLight = true;
        return this;
    }

    public Vertex setNormal(float nx, float ny, float nz) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        hasNormal = true;
        return this;
    }

    public Vertex setNormal(PoseStack.Pose pose, float nx, float ny, float nz) {
        return setNormal(pose.normal(), nx, ny, nz);
    }

    public Vertex setNormal(Matrix3f normal, float nx, float ny, float nz) {
        Vector3f n = normal.transform(new Vector3f(nx, ny, nz));
        return setNormal(n.x(), n.y(), n.z());
    }

    /** Writes the vertex in the buffer's element order */
    public void end() {
        VertexConsumer c = consumer;
        c.vertex(x, y, z);
        if (hasColor) c.color(r, g, b, a);
        if (hasUv) c.uv(u, v);
        if (hasColor && hasUv) c.color(r, g, b, a);
        if (hasOverlay) c.overlayCoords(overlay);
        if (hasLight) c.uv2(light);
        if (hasNormal) c.normal(nx, ny, nz);
        c.endVertex();
        consumer = null;
    }

    /** 1.21 {@code Tesselator.begin}: the shared buffer, begun */
    public static BufferBuilder begin(VertexFormat.Mode mode, com.mojang.blaze3d.vertex.VertexFormat format) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, format);
        return buffer;
    }
}
