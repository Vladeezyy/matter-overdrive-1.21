package matteroverdrive.client.starmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;

/** Geometry helpers for the 1.7.10 hologram code (Tessellator / GLU sphere / RenderUtils). Colours are 0xRRGGBB. */
final class Holo {
    private Holo() {}

    static float r(int rgb) {
        return (rgb >> 16 & 255) / 255f;
    }

    static float g(int rgb) {
        return (rgb >> 8 & 255) / 255f;
    }

    static float b(int rgb) {
        return (rgb & 255) / 255f;
    }

    /** 1.7.10 Color.multiplyWithoutAlpha / applyColorWithMultipy as a packed colour. */
    static int mul(int rgb, float m) {
        int rr = Mth.clamp((int) ((rgb >> 16 & 255) * m), 0, 255), gg = Mth.clamp((int) ((rgb >> 8 & 255) * m), 0, 255),
                bb = Mth.clamp((int) ((rgb & 255) * m), 0, 255);
        return rr << 16 | gg << 8 | bb;
    }

    /** 1.7.10 RenderUtils.tessalateParticle: a camera facing quad from the viewer's yaw / pitch, in the current frame. */
    static void particle(PoseStack.Pose p, VertexConsumer vc, float yaw, float pitch, float u0, float v0, float u1, float v1, double scale,
                         double x, double y, double z, float r, float g, float b, float a) {
        float f1 = Mth.cos(yaw * Mth.DEG_TO_RAD), f2 = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float f3 = -f2 * Mth.sin(pitch * Mth.DEG_TO_RAD), f4 = f1 * Mth.sin(pitch * Mth.DEG_TO_RAD), f5 = Mth.cos(pitch * Mth.DEG_TO_RAD);
        float s = (float) scale, px = (float) x, py = (float) y, pz = (float) z;
        vc.addVertex(p, px - f1 * s - f3 * s, py - f5 * s, pz - f2 * s - f4 * s).setUv(u1, v1).setColor(r, g, b, a);
        vc.addVertex(p, px - f1 * s + f3 * s, py + f5 * s, pz - f2 * s + f4 * s).setUv(u1, v0).setColor(r, g, b, a);
        vc.addVertex(p, px + f1 * s + f3 * s, py + f5 * s, pz + f2 * s + f4 * s).setUv(u0, v0).setColor(r, g, b, a);
        vc.addVertex(p, px + f1 * s - f3 * s, py - f5 * s, pz + f2 * s - f4 * s).setUv(u0, v1).setColor(r, g, b, a);
    }

    static void line(PoseStack.Pose p, VertexConsumer vc, double x1, double y1, double z1, double x2, double y2, double z2, int rgb) {
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len;
        dy /= len;
        dz /= len;
        vc.addVertex(p, (float) x1, (float) y1, (float) z1).setColor(r(rgb), g(rgb), b(rgb), 1).setNormal(p, dx, dy, dz);
        vc.addVertex(p, (float) x2, (float) y2, (float) z2).setColor(r(rgb), g(rgb), b(rgb), 1).setNormal(p, dx, dy, dz);
    }

    /** GLU Sphere (z axis through the poles) drawn with glPolygonMode(GL_LINE): its slice and stack edges. */
    static void wireSphere(PoseStack.Pose p, VertexConsumer vc, double radius, int slices, int stacks, int rgb) {
        for (int j = 0; j <= stacks; j++) {
            double phi = Math.PI * j / stacks, ringZ = Math.cos(phi) * radius, ringR = Math.sin(phi) * radius;
            for (int i = 0; i < slices; i++) {
                double t0 = 2 * Math.PI * i / slices, t1 = 2 * Math.PI * (i + 1) / slices;
                if (j > 0 && j < stacks) {
                    line(p, vc, Math.sin(t0) * ringR, Math.cos(t0) * ringR, ringZ, Math.sin(t1) * ringR, Math.cos(t1) * ringR, ringZ, rgb);
                }
                if (j < stacks) {
                    double phi1 = Math.PI * (j + 1) / stacks, z1 = Math.cos(phi1) * radius, r1 = Math.sin(phi1) * radius;
                    line(p, vc, Math.sin(t0) * ringR, Math.cos(t0) * ringR, ringZ, Math.sin(t0) * r1, Math.cos(t0) * r1, z1, rgb);
                }
            }
        }
    }

    /** A filled GLU-style sphere as quads (the planet's black core). */
    static void solidSphere(PoseStack.Pose p, VertexConsumer vc, double radius, int slices, int stacks, int rgb) {
        for (int j = 0; j < stacks; j++) {
            double phi0 = Math.PI * j / stacks, phi1 = Math.PI * (j + 1) / stacks;
            for (int i = 0; i < slices; i++) {
                double t0 = 2 * Math.PI * i / slices, t1 = 2 * Math.PI * (i + 1) / slices;
                vertex(p, vc, radius, phi0, t0, rgb);
                vertex(p, vc, radius, phi1, t0, rgb);
                vertex(p, vc, radius, phi1, t1, rgb);
                vertex(p, vc, radius, phi0, t1, rgb);
            }
        }
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer vc, double radius, double phi, double theta, int rgb) {
        vc.addVertex(p, (float) (Math.sin(phi) * Math.sin(theta) * radius), (float) (Math.sin(phi) * Math.cos(theta) * radius),
                (float) (Math.cos(phi) * radius)).setColor(r(rgb), g(rgb), b(rgb), 1);
    }

    /** 1.7.10 RenderUtils.drawCube(sizeX, sizeY, sizeZ) from the origin. */
    static void cube(PoseStack.Pose p, VertexConsumer vc, float sx, float sy, float sz, int rgb) {
        float r = r(rgb), g = g(rgb), b = b(rgb);
        float[][] faces = {
                {0, 0, 0, sx, 0, 0, sx, 0, sz, 0, 0, sz}, {sx, sy, 0, 0, sy, 0, 0, sy, sz, sx, sy, sz},
                {0, 0, 0, 0, sy, 0, sx, sy, 0, sx, 0, 0}, {0, 0, sz, sx, 0, sz, sx, sy, sz, 0, sy, sz},
                {0, 0, 0, 0, 0, sz, 0, sy, sz, 0, sy, 0}, {sx, 0, 0, sx, sy, 0, sx, sy, sz, sx, 0, sz}};
        for (float[] f : faces) {
            for (int i = 0; i < 12; i += 3) vc.addVertex(p, f[i], f[i + 1], f[i + 2]).setColor(r, g, b, 1);
        }
    }

    /** 1.7.10 RenderUtils.drawShip: four triangles. */
    static void ship(PoseStack.Pose p, VertexConsumer vc, float size, int rgb) {
        float r = r(rgb), g = g(rgb), b = b(rgb);
        float[] v = {-size, 0, 0, size, 0, -size, size, 0, size,
                -size, 0, 0, size, 0, size, size, size, 0,
                -size, 0, 0, size, size, 0, size, 0, -size,
                size, 0, -size, size, size, 0, size, 0, size};
        for (int i = 0; i < v.length; i += 3) vc.addVertex(p, v[i], v[i + 1], v[i + 2]).setColor(r, g, b, 1);
    }

    /** A textured quad from (x, y) of the given size in the current frame (1.7.10 RenderUtils.renderIcon). */
    static void icon(PoseStack.Pose p, VertexConsumer vc, float x, float y, float w, float h, int rgb) {
        float r = r(rgb), g = g(rgb), b = b(rgb);
        vc.addVertex(p, x, y + h, 0).setUv(0, 1).setColor(r, g, b, 1);
        vc.addVertex(p, x + w, y + h, 0).setUv(1, 1).setColor(r, g, b, 1);
        vc.addVertex(p, x + w, y, 0).setUv(1, 0).setColor(r, g, b, 1);
        vc.addVertex(p, x, y, 0).setUv(0, 0).setColor(r, g, b, 1);
    }
}
