package matteroverdrive.client;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.block.entity.GravitationalStabilizerBlockEntity;
import matteroverdrive.client.starmap.HoloRenderTypes;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.client.gui.Font;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import matteroverdrive.compat.render.BlockEntityRenderState;
import matteroverdrive.compat.render.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 TileEntityRendererGravitationalStabilizer + GravitationalStabilizerBeamParticle: while the stabilizer holds an
 * anomaly, a block-wide physbeam (two crossed additive planes, the texture repeating every 2 blocks) in the beam colour
 * runs to the anomaly, glowing motes spiral along it (a 20% chance per tick, 80 ticks to the anomaly; derived from the
 * game time here instead of particle entities) and a holo screen on the back lists the anomaly's numbers.
 */
public class StabilizerRenderer implements matteroverdrive.compat.render.StateBlockEntityRenderer<GravitationalStabilizerBlockEntity, StabilizerRenderer.State> {
    private static final ResourceLocation BEAM = id("textures/fx/physbeam.png");
    private static final ResourceLocation MOTE = id("textures/fx/particles_additive.png");
    private static final ResourceLocation GLOW = id("textures/fx/holo_monitor_glow.png");
    private static final ResourceLocation BACK = id("textures/block/pattern_monitor_holo_back.png");
    private static final int HOLO = 0xA9E2FB;
    private static final int MOTE_LIFE = 80;
    private final Font font;

    public StabilizerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    public static class State extends BlockEntityRenderState {
        @Nullable Vec3 target;
        Direction facing = Direction.NORTH;
        float r, g, b;
        long gameTime;
        long dayTime;
        float partial;
        long seed;
        BlockPos pos = BlockPos.ZERO;
        List<String> info = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GravitationalStabilizerBlockEntity stabilizer, State state, float partialTick, Vec3 camera,
                                   @Nullable Object crumbling) {
        matteroverdrive.compat.render.StateBlockEntityRenderer.super.extractRenderState(stabilizer, state, partialTick, camera, crumbling);
        var level = stabilizer.getLevel();
        state.target = null;
        state.info = List.of();
        if (level == null) return;
        state.facing = stabilizer.getBlockState().getValue(MachineBlock.FACING);
        state.pos = stabilizer.getBlockPos();
        state.seed = state.pos.asLong();
        state.gameTime = level.getGameTime();
        state.dayTime = level.getDayTime();
        state.partial = partialTick;
        if (!stabilizer.getBlockState().getValue(MachineBlock.ACTIVE)) return;
        BlockPos hit = GravitationalStabilizerBlockEntity.findAnomaly(level, state.pos, state.facing);
        if (hit == null || !(level.getBlockEntity(hit) instanceof GravitationalAnomalyBlockEntity anomaly)) return;
        state.target = Vec3.atCenterOf(hit).subtract(Vec3.atLowerCornerOf(state.pos));
        state.r = (float) stabilizer.getBeamColorR();
        state.g = (float) stabilizer.getBeamColorG();
        state.b = (float) stabilizer.getBeamColorB();
        // 1.7.10 TileEntityGravitationalAnomaly.addInfo (English, as in 1.7.10)
        DecimalFormat format = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        state.info = List.of("Mass: " + anomaly.getMass(), "Range: " + format.format(anomaly.getMaxRange()),
                "Brake Range: " + format.format(anomaly.getBlockBreakRange()), "Horizon: " + format.format(anomaly.getEventHorizon()),
                "Brake Lvl: " + format.format(anomaly.getRealMass() * 4 * anomaly.getSuppression()));
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.target == null) return;
        Vec3 from = new Vec3(0.5, 0.5, 0.5);
        Vec3 to = state.target;
        beam(pose, collector, from, to, color(state.r, state.g, state.b));
        motes(pose, collector, state, from, to, camera);
        screen(pose, collector, state);
    }

    /** Two planes along the beam, one block wide, crossed at right angles; u runs along the beam (distance / 2 tiles). */
    private static void beam(PoseStack pose, SubmitNodeCollector collector, Vec3 from, Vec3 to, int color) {
        Vec3 axis = to.subtract(from);
        float length = (float) axis.length();
        Vec3 dir = axis.normalize();
        Vec3 side1 = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 side2 = dir.cross(side1).normalize();
        collector.submitCustomGeometry(pose, HoloRenderTypes.textured(BEAM), (p, vc) -> {
            for (Vec3 side : new Vec3[] {side1, side2}) {
                Vec3 h = side.scale(0.5);
                vertex(vc, p, from.add(h), 0, 0, color);
                vertex(vc, p, from.subtract(h), 0, 1, color);
                vertex(vc, p, to.subtract(h), length / 2, 1, color);
                vertex(vc, p, to.add(h), length / 2, 0, color);
            }
        });
    }

    /**
     * GravitationalStabilizerBeamParticle: each mote rides the beam from the stabilizer to the anomaly in 80 ticks,
     * circling it (radius 0.3-0.45) around the beam's up axis, in the beam colour of the tick it was born.
     */
    private static void motes(PoseStack pose, SubmitNodeCollector collector, State state, Vec3 from, Vec3 to, CameraRenderState camera) {
        Vector3f dir = new Vector3f((float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z));
        Vector3f up = new Vector3f(0, 1, 0);   // the stabilizer only faces sideways: 1.7.10 getAboveSide is up
        Vector3f spiralAxis = dir.normalize(new Vector3f()).cross(up, new Vector3f());
        // billboard axes towards the camera
        Vec3 camPos = camera.pos;
        Vec3 origin = Vec3.atLowerCornerOf(state.pos);
        collector.submitCustomGeometry(pose, HoloRenderTypes.textured(MOTE), (p, vc) -> {
            for (int back = 0; back < MOTE_LIFE; back++) {
                long born = state.gameTime - back;
                long h = mix(state.seed ^ born * 0x9E3779B97F4A7C15L);
                if ((h & 0xFFFF) / 65536f >= 0.2f) continue;
                float age = back + state.partial;
                float percent = age / MOTE_LIFE;
                if (percent > 1) continue;
                int start = (int) ((h >>> 16) % MOTE_LIFE);
                float orbit = 0.3f + ((h >>> 24) & 0xFF) / 256f * 0.15f;
                float half = 0.1f * 0.75f * (0.5f + ((h >>> 32) & 0xFF) / 256f * 0.5f) * 2;
                float angle = (age + start) * 0.5f;
                Vector3f offset = spiralAxis.mul(Mth.sin(angle) * orbit, new Vector3f()).add(up.mul(Mth.cos(angle) * orbit, new Vector3f()));
                Vector3f at = new Vector3f((float) from.x, (float) from.y, (float) from.z).add(dir.mul(percent, new Vector3f())).add(offset);
                // colour of the tick it was born (1.7.10 setColor at spawn)
                double t = (state.dayTime - back) * 0.01;
                int color = color((float) matteroverdrive.util.MOMath.noise(0, state.pos.getY(), t),
                        (float) matteroverdrive.util.MOMath.noise(state.pos.getX(), 0, t),
                        (float) matteroverdrive.util.MOMath.noise(0, 0, state.pos.getZ() + t));
                Vec3 toCamera = camPos.subtract(origin.add(at.x, at.y, at.z)).normalize();
                Vec3 right = toCamera.cross(new Vec3(0, 1, 0));
                right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
                Vec3 upB = right.cross(toCamera).normalize();
                Vec3 c = new Vec3(at.x, at.y, at.z);
                Vec3 rr = right.scale(half), uu = upB.scale(half);
                float cell = 32f / 128f;
                vertex(vc, p, c.subtract(rr).subtract(uu), 0, cell, color);
                vertex(vc, p, c.add(rr).subtract(uu), cell, cell, color);
                vertex(vc, p, c.add(rr).add(uu), cell, 0, color);
                vertex(vc, p, c.subtract(rr).add(uu), 0, 0, color);
            }
        });
    }

    /** RenderUtils.beginDrawinngBlockScreen + drawScreenInfoWithGlobalAutoSize on the side opposite the beam. */
    private void screen(PoseStack pose, SubmitNodeCollector collector, State state) {
        Direction side = state.facing.getOpposite();
        pose.pushPose();
        pose.translate(0.5 + side.getStepX() * 0.55, 0.5, 0.5 + side.getStepZ() * 0.55);
        // local +z out of the face, +x to the viewer's right
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(side.getStepX(), side.getStepZ())));
        double noise = matteroverdrive.util.MOMath.noise(state.pos.getX(), state.dayTime * 0.01, state.pos.getZ()) * 0.5 + 0.5;
        screenPlane(pose, collector, GLOW, mul(HOLO, noise), 0);
        screenPlane(pose, collector, BACK, mul(HOLO, 0.05), 0.05f);
        pose.translate(0, 0, 0.08f);
        pose.scale(0.01f, -0.01f, 0.01f);
        int leftMargin = 10, sizeX = 80, sizeY = 80;
        int maxWidth = 0;
        for (String line : state.info) maxWidth = Math.max(maxWidth, font.width(line));
        float scale = maxWidth > 0 ? Mth.clamp((float) sizeX / maxWidth, 0.02f, 4) : 1;
        int lineHeight = (int) (font.lineHeight * scale);
        int height = 0;
        for (int i = 0; i < state.info.size(); i++) if (height + lineHeight < sizeY) height += lineHeight;
        int y = 0;
        for (String line : state.info) {
            if (y + lineHeight >= sizeY) break;
            pose.pushPose();
            pose.translate(leftMargin + sizeX / 2f - 50, y + lineHeight / 2f - height / 2f, 0);
            pose.scale(scale, scale, 1);
            collector.submitText(pose, -font.width(line) / 2f, -font.lineHeight / 2f, Component.literal(line).getVisualOrderText(), false,
                    Font.DisplayMode.POLYGON_OFFSET, 0xF000F0, 0xFF000000 | HOLO, 0, 0);
            pose.popPose();
            y += lineHeight;
        }
        pose.popPose();
    }

    private static void screenPlane(PoseStack pose, SubmitNodeCollector collector, ResourceLocation texture, int rgb, float z) {
        collector.submitCustomGeometry(pose, HoloRenderTypes.screen(texture), (p, vc) -> {
            vertex(vc, p, new Vec3(-0.5, 0.5, z), 0, 0, rgb);
            vertex(vc, p, new Vec3(-0.5, -0.5, z), 0, 1, rgb);
            vertex(vc, p, new Vec3(0.5, -0.5, z), 1, 1, rgb);
            vertex(vc, p, new Vec3(0.5, 0.5, z), 1, 0, rgb);
        });
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 at, float u, float v, int rgb) {
        vc.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setUv(u, v).setColor(0xFF000000 | rgb);
    }

    /** glColor3d clamps to 0..1: a negative noise channel adds nothing. */
    private static int color(float r, float g, float b) {
        return Mth.clamp((int) (r * 255), 0, 255) << 16 | Mth.clamp((int) (g * 255), 0, 255) << 8 | Mth.clamp((int) (b * 255), 0, 255);
    }

    private static int mul(int rgb, double m) {
        return color((float) ((rgb >> 16 & 255) / 255.0 * m), (float) ((rgb >> 8 & 255) / 255.0 * m), (float) ((rgb & 255) / 255.0 * m));
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(GravitationalStabilizerBlockEntity stabilizer) {
        BlockPos pos = stabilizer.getBlockPos();
        Direction facing = stabilizer.getBlockState().getValue(MachineBlock.FACING);
        return new AABB(pos).expandTowards(facing.getStepX() * GravitationalStabilizerBlockEntity.RANGE, 0,
                facing.getStepZ() * GravitationalStabilizerBlockEntity.RANGE).inflate(1);
    }
}
