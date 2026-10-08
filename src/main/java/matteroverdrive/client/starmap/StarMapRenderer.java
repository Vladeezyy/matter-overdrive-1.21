package matteroverdrive.client.starmap;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.starmap.GalacticPosition;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.GalaxyClient;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.Quadrant;
import matteroverdrive.starmap.SpaceBody;
import matteroverdrive.starmap.Star;
import matteroverdrive.starmap.TravelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 TileEntityRendererStarMap (+ TileEntityRendererStation, StarMapRenderGalaxy / Quadrant / Star / Planet /
 * PlanetStats): the hologram beam rising from the table, the zoomed space body above it and its info panel on the side
 * facing the player (snapped to 90 degrees), or "ACCESS DENIED" for players who can't use the map.
 */
public class StarMapRenderer implements BlockEntityRenderer<StarMapBlockEntity, StarMapRenderer.State> {
    private static final ResourceLocation BEAM = tex("textures/fx/hologram_beam.png");
    private static final ResourceLocation PARTICLES = tex("textures/particle/particles_additive.png");
    private static final FontDescription ALT = new FontDescription.Resource(Minecraft.ALT_FONT);
    // 1.7.10 StarMapRendererAbstract icons on the 128 px additive particle sheet
    private static final float[] STAR_ICON = {0, 0, 32 / 128f, 32 / 128f}, SELECTED_ICON = {32 / 128f, 0, 64 / 128f, 32 / 128f},
            CURRENT_ICON = {64 / 128f, 0, 96 / 128f, 32 / 128f};
    private final Font font;

    public StarMapRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    public static class State extends BlockEntityRenderState {
        int zoom;
        GalacticPosition position = GalacticPosition.NONE, destination = GalacticPosition.NONE;
        boolean usable;
        double time, noise;
        float distance;
        Vec3 blockCenter = Vec3.ZERO;
        @Nullable StarMapBlockEntity starMap;
        boolean hidden;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StarMapBlockEntity starMap, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(starMap, state, partialTick, camera, crumbling);
        state.zoom = starMap.getZoomLevel();
        state.position = starMap.getGalaxyPosition();
        state.destination = starMap.getDestination();
        Player player = Minecraft.getInstance().player;
        state.usable = player != null && starMap.isUseableByPlayer(player);
        state.time = starMap.getLevel() == null ? 0 : starMap.getLevel().getGameTime() + partialTick;
        var pos = starMap.getBlockPos();
        state.noise = (Mth.sin(pos.getX() * 0.3f) + Mth.cos(pos.getZ() * 0.3f) + Mth.sin(pos.getY() * 0.3f)) / 6 + 0.5;
        state.distance = (float) Math.max(0.1, camera.distanceTo(Vec3.atLowerCornerOf(pos)));
        state.blockCenter = Vec3.atCenterOf(pos);
        state.starMap = starMap;
        // the star map screen shows the hologram itself (1.7.10 skipped the world one while GuiStarMap was open)
        state.hidden = Minecraft.getInstance().screen instanceof matteroverdrive.client.screen.StarMapScreen;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(StarMapBlockEntity starMap) {
        var pos = starMap.getBlockPos();
        return new AABB(pos.getX() - 3, pos.getY(), pos.getZ() - 3, pos.getX() + 4, pos.getY() + 6, pos.getZ() + 4);
    }

    // --- context ---------------------------------------------------------------------------------------

    /** What the body renderers need: the camera, time, viewer distance and the map's selection. */
    public record Ctx(State state, PoseStack pose, HoloSink sink, Font font, float yaw, float pitch, Quaternionf orientation,
                      @Nullable Player player, Galaxy galaxy) {
        double time() {
            return state.time;
        }

        float distance() {
            return state.distance;
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera();
        Galaxy galaxy = GalaxyClient.getGalaxy();
        if (state.hidden) return;
        drawHoloLights(state, pose, collector);
        if (!state.usable) {
            drawAccessDenied(state, pose, collector);
            return;
        }
        if (galaxy == null) return;
        Ctx ctx = new Ctx(state, pose, HoloSink.of(collector), font, cam.getYRot(), cam.getXRot(), camera.orientation, mc.player, galaxy);
        SpaceBody body = activeBody(state, galaxy);
        Body renderer = body == null ? null : rendererFor(state.zoom, body);
        if (!renderHologram(ctx)) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.translate(0, renderer.height(), 0);
        // 1.7.10 drawHoloGuiInfo: the side facing the player, snapped to 90 degrees
        Vec3 viewer = cam.position();
        double dx = viewer.x - state.blockCenter.x, dz = viewer.z - state.blockCenter.z;
        double angle = Math.atan2(dz, dx);
        if (angle < 0) angle += Math.PI * 2;
        double yaw = Math.round((Math.PI / 2 - angle) * (180 / Math.PI) / 90d) * 90;
        pose.pushPose();
        pose.translate(0, -renderer.height() + 0.3, 0);
        pose.mulPose(Axis.YP.rotationDegrees((float) yaw));
        pose.translate(-1, 0, 0.8);
        pose.scale(0.01f, -0.01f, 0.01f);
        renderer.renderGuiInfo(ctx, body, 0.5f);
        pose.popPose();
        pose.popPose();
    }

    /** 1.7.10 renderHologramBase without the info panel: the zoomed body above the table. False when there's nothing to draw. */
    public static boolean renderHologram(Ctx ctx) {
        State state = ctx.state();
        SpaceBody body = activeBody(state, ctx.galaxy());
        // a planet the client hasn't loaded yet: ask for its star's planets
        if (body == null && state.zoom >= 2) GalaxyClient.requestPlanets(ctx.galaxy().getStar(state.destination));
        if (body == null) return false;
        Body renderer = rendererFor(state.zoom, body);
        if (renderer == null) return false;
        PoseStack pose = ctx.pose();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.translate(0, renderer.height(), 0);
        renderer.renderBody(ctx, body);
        pose.popPose();
        return true;
    }

    /** The zoomed body's info panel (the star map screen draws it at the bottom). */
    public static void renderGuiInfo(Ctx ctx, float opacity) {
        SpaceBody body = activeBody(ctx.state(), ctx.galaxy());
        Body renderer = body == null ? null : rendererFor(ctx.state().zoom, body);
        if (renderer != null) renderer.renderGuiInfo(ctx, body, opacity);
    }

    /** The render state for a star map outside the world renderer (the screen). */
    public static State stateOf(StarMapBlockEntity starMap, float partialTick, @Nullable Player player) {
        State state = new State();
        state.zoom = starMap.getZoomLevel();
        state.position = starMap.getGalaxyPosition();
        state.destination = starMap.getDestination();
        state.usable = player != null && starMap.isUseableByPlayer(player);
        state.time = starMap.getLevel() == null ? 0 : starMap.getLevel().getGameTime() + partialTick;
        state.distance = 1;
        state.blockCenter = Vec3.atCenterOf(starMap.getBlockPos());
        state.starMap = starMap;
        return state;
    }

    private static @Nullable SpaceBody activeBody(State state, Galaxy galaxy) {
        return switch (state.zoom) {
            case 0 -> galaxy;
            case 1 -> galaxy.getQuadrant(state.destination);
            case 2 -> galaxy.getStar(state.destination);
            default -> galaxy.getPlanet(state.destination);
        };
    }

    private static @Nullable Body rendererFor(int zoom, SpaceBody body) {
        if (body instanceof Galaxy) return GALAXY;
        if (body instanceof Quadrant) return QUADRANT;
        if (body instanceof Star) return STAR;
        if (body instanceof Planet) return zoom == 4 ? PLANET_STATS : zoom == 3 ? PLANET : null;
        return null;
    }

    // --- station base (1.7.10 TileEntityRendererStation) -------------------------------------------------------

    /** The beam: from the 9 px table top up 1 block, widening by 1 block on every side. */
    private static void drawHoloLights(State state, PoseStack pose, SubmitNodeCollector collector) {
        int color = Holo.mul(state.usable ? Galaxy.COLOR_HOLO : Galaxy.COLOR_HOLO_RED, 0.25f);
        float h = 1, top = 2 - 1;
        pose.pushPose();
        pose.translate(0, 9 / 16f, 0);
        collector.submitCustomGeometry(pose, HoloRenderTypes.textured(BEAM), (p, vc) -> {
            float r = Holo.r(color), g = Holo.g(color), b = Holo.b(color);
            float[][] q = {
                    {0, 0, 0, 1, 1, -top, h, -top, 1, 0, 1 + top, h, -top, 0, 0, 1, 0, 0, 0, 1},
                    {1, 0, 0, 1, 1, 1 + top, h, -top, 1, 0, 1 + top, h, 1 + top, 0, 0, 1, 0, 1, 0, 1},
                    {1, 0, 1, 1, 1, 1 + top, h, 1 + top, 1, 0, -top, h, 1 + top, 0, 0, 0, 0, 1, 0, 1},
                    {0, 0, 1, 1, 1, -top, h, 1 + top, 1, 0, -top, h, -top, 0, 0, 0, 0, 0, 0, 1}};
            for (float[] f : q) {
                for (int i = 0; i < 20; i += 5) vc.addVertex(p, f[i], f[i + 1], f[i + 2]).setUv(f[i + 3], f[i + 4]).setColor(r, g, b, 1);
            }
        });
        pose.popPose();
    }

    private void drawAccessDenied(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0.5, 0.8, 0.5);
        pose.mulPose(Axis.YN.rotationDegrees((float) (state.time * 0.5 + 1800 * state.noise)));
        pose.scale(0.02f, -0.02f, 0.02f);
        int color = Holo.mul(Galaxy.COLOR_HOLO_RED, 0.33f);
        String[] words = Component.translatable("gui.matteroverdrive.hologram.access_denied").getString().split(" ");
        // 1.7.10 drew it without culling: both sides (the back mirrored there; readable here)
        for (int side = 0; side < 2; side++) {
            if (side == 1) pose.mulPose(Axis.YP.rotationDegrees(180));
            for (int i = 0; i < words.length; i++) {
                var text = Component.literal(words[i]).getVisualOrderText();
                collector.submitText(pose, -font.width(text) / 2f, -32 + i * 10, text, false, Font.DisplayMode.NORMAL, 0xF000F0,
                        0xFF000000 | color, 0, 0);
            }
        }
        pose.popPose();
    }

    // --- shared drawing --------------------------------------------------------------------------------------

    static void text(Ctx ctx, String string, float x, float y, int rgb, float multiply, boolean alien) {
        var component = Component.literal(string);
        if (alien) component = component.withStyle(s -> s.withFont(ALT));
        ctx.sink().text(ctx.pose(), x, y, component.getVisualOrderText(), 0xFF000000 | Holo.mul(rgb, multiply));
    }

    static void icon(Ctx ctx, String name, float x, float y, int size, int rgb) {
        ctx.sink().icon(ctx.pose(), tex("textures/gui/holo/" + name + ".png"), x, y, size, size, rgb);
    }

    static int iconSize(String name) {
        return switch (name) {
            case "page_icon_star" -> 12;
            case "holo_factory" -> 14;
            case "arrow_right" -> 19;
            default -> 16;
        };
    }

    static void icon(Ctx ctx, String name, float x, float y, int rgb) {
        icon(ctx, name, x, y, iconSize(name), rgb);
    }

    static String percent(float value) {
        return NumberFormat.getPercentInstance().format(value);
    }

    // --- body renderers (1.7.10 ISpaceBodyHoloRenderer) -----------------------------------------------------------

    interface Body {
        double height();

        void renderBody(Ctx ctx, SpaceBody body);

        void renderGuiInfo(Ctx ctx, SpaceBody body, float opacity);
    }

    /** 1.7.10 StarMapRendererStars.renderStars: every star of the quadrant as a particle; the route to the selected star. */
    static void renderStars(Ctx ctx, Quadrant quadrant, double distanceMultiply, double starSizeMultiply) {
        State state = ctx.state();
        Star[] fromTo = new Star[2];
        ctx.sink().geometry(ctx.pose(), HoloRenderTypes.textured(PARTICLES), (p, vc) -> {
            for (Star star : quadrant.getStars()) {
                double x = star.getX() * distanceMultiply, y = star.getY() * distanceMultiply, z = star.getZ() * distanceMultiply;
                int color = star.getColor();
                float r = Holo.r(color), g = Holo.g(color), b = Holo.b(color);
                double size = 0.01;
                if (state.destination.is(star)) {
                    size = 0.035;
                    particle(ctx, p, vc, SELECTED_ICON, star.getSize() * 0.05 * starSizeMultiply, x, y, z, r, g, b);
                }
                if (state.position.is(star)) {
                    size = 0.035;
                    particle(ctx, p, vc, CURRENT_ICON, star.getSize() * 0.05 * starSizeMultiply, x, y, z, r, g, b);
                }
                if (ctx.player() != null && star.isClaimed(ctx.player()) == 3) size = 0.025;
                particle(ctx, p, vc, STAR_ICON, star.getSize() * size * starSizeMultiply, x, y, z, r, g, b);
            }
        });
        for (Star star : quadrant.getStars()) {
            if (state.position.is(star)) fromTo[0] = star;
            if (state.destination.is(star)) fromTo[1] = star;
        }
        if (fromTo[0] != null && fromTo[1] != null && fromTo[0] != fromTo[1]) {
            Star from = fromTo[0], to = fromTo[1];
            ctx.sink().geometry(ctx.pose(), HoloRenderTypes.LINE_TYPE, (p, vc) -> Holo.line(p, vc,
                    from.getX() * distanceMultiply, from.getY() * distanceMultiply, from.getZ() * distanceMultiply,
                    to.getX() * distanceMultiply, to.getY() * distanceMultiply, to.getZ() * distanceMultiply, Holo.mul(Galaxy.COLOR_HOLO, 0.3f)));
        }
    }

    private static void particle(Ctx ctx, com.mojang.blaze3d.vertex.PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc, float[] icon,
                                 double scale, double x, double y, double z, float r, float g, float b) {
        Holo.particle(p, vc, ctx.yaw(), ctx.pitch(), icon[0], icon[1], icon[2], icon[3], scale, x, y, z, r, g, b, 1);
    }

    static final Body GALAXY = new Body() {
        @Override
        public double height() {
            return 2.5;
        }

        @Override
        public void renderBody(Ctx ctx, SpaceBody body) {
            Galaxy galaxy = (Galaxy) body;
            for (Quadrant quadrant : galaxy.getQuadrants()) renderStars(ctx, quadrant, 2, 2);
            var level = Minecraft.getInstance().level;
            for (TravelEvent event : galaxy.getTravelEvents()) {
                if (!event.isValid(galaxy) || level == null) continue;
                Vec3 from = galaxy.getPlanet(event.getFrom()).getStar().getPosition(2);
                Vec3 to = galaxy.getPlanet(event.getTo()).getStar().getPosition(2);
                Vec3 dir = from.subtract(to);
                double percent = event.getPercent(level);
                PoseStack pose = ctx.pose();
                pose.pushPose();
                pose.translate(from.x + dir.x * percent, from.y + dir.y * percent, from.z + dir.z * percent);
                Vec3 n = dir.normalize();
                pose.mulPose(new Quaternionf().rotationTo(-1, 0, 0, (float) n.x, (float) n.y, (float) n.z));
                ctx.sink().geometry(pose, HoloRenderTypes.COLOR_TRIANGLES,
                        (p, vc) -> Holo.ship(p, vc, 0.02f, Holo.mul(Galaxy.COLOR_HOLO, 0.5f)));
                pose.popPose();
                ctx.sink().geometry(pose, HoloRenderTypes.LINE_TYPE,
                        (p, vc) -> Holo.line(p, vc, from.x, from.y, from.z, to.x, to.y, to.z, Holo.mul(Galaxy.COLOR_HOLO_PURPLE, 0.5f)));
            }
        }

        @Override
        public void renderGuiInfo(Ctx ctx, SpaceBody body, float opacity) {
            Galaxy galaxy = (Galaxy) body;
            if (ctx.player() == null) return;
            int owned = galaxy.getOwnedSystemCount(ctx.player()), enemy = galaxy.getEnemySystemCount(ctx.player());
            int free = galaxy.getStarCount() - owned - enemy;
            icon(ctx, "page_icon_star", 0, -30, Holo.mul(Galaxy.COLOR_HOLO_GREEN, opacity));
            text(ctx, "x" + owned, 24, -23, Galaxy.COLOR_HOLO_GREEN, opacity, false);
            icon(ctx, "page_icon_star", 64, -30, Holo.mul(Galaxy.COLOR_HOLO_RED, opacity));
            text(ctx, "x" + enemy, 88, -23, Galaxy.COLOR_HOLO_RED, opacity, false);
            icon(ctx, "page_icon_star", 128, -30, Holo.mul(Galaxy.COLOR_HOLO, opacity));
            text(ctx, "x" + free, 152, -23, Galaxy.COLOR_HOLO, opacity, false);
            var level = Minecraft.getInstance().level;
            for (int i = 0; i < galaxy.getTravelEvents().size(); i++) {
                TravelEvent event = galaxy.getTravelEvents().get(i);
                if (!event.isValid(galaxy) || level == null) continue;
                text(ctx, String.format("%s -> %s : %s", galaxy.getPlanet(event.getFrom()).getName(), galaxy.getPlanet(event.getTo()).getName(),
                        matteroverdrive.client.screen.AndroidSpawnerScreen.formatRemainingTime(event.getTimeRemaining(level) / 20f)),
                        0, -48 - i * 10, Galaxy.COLOR_HOLO, opacity, false);
            }
        }
    };

    static final Body QUADRANT = new Body() {
        @Override
        public double height() {
            return 0.3;
        }

        /** 1.7.10: x and z centred on the quadrant, y not (the quadrant sits above its corner). */
        @Override
        public void renderBody(Ctx ctx, SpaceBody body) {
            Quadrant quadrant = (Quadrant) body;
            double m = 5;
            ctx.pose().translate((-quadrant.getX() - quadrant.getSize() / 2) * m, -quadrant.getY() * m, (-quadrant.getZ() - quadrant.getSize() / 2) * m);
            renderStars(ctx, quadrant, m, m);
        }

        @Override
        public void renderGuiInfo(Ctx ctx, SpaceBody body, float opacity) {
            Star star = ctx.galaxy().getStar(ctx.state().destination);
            Star origin = ctx.galaxy().getStar(ctx.state().position);
            if (star == null) return;
            GalaxyClient.requestPlanets(star);
            int planetCount = star.getPlanets().size();
            int color = planetCount <= 0 ? Galaxy.COLOR_HOLO_RED : Galaxy.COLOR_HOLO;
            icon(ctx, "page_icon_planet", 8, -28, Holo.mul(color, opacity));
            text(ctx, "x" + planetCount, 28, -21, color, opacity, false);
            boolean known = ctx.player() != null && GalaxyClient.canSeeStarInfo(star, ctx.player());
            text(ctx, star.getName(), 0, -52, Galaxy.COLOR_HOLO, opacity, !known);
            icon(ctx, "icon_size", 48, -28, Holo.mul(Galaxy.COLOR_HOLO, opacity));
            text(ctx, percent(star.getSize()), 68, -23, Galaxy.COLOR_HOLO, opacity, false);
            if (origin != null) {
                text(ctx, String.format("Distance: %s LY", new DecimalFormat("#").format(origin.getPosition().distanceTo(star.getPosition())
                        * Galaxy.GALAXY_SIZE_TO_LY)), 0, -42, Galaxy.COLOR_HOLO, opacity, false);
            }
        }
    };

    static final Body STAR = new Body() {
        private final Random random = new Random();

        @Override
        public double height() {
            return 1.5;
        }

        @Override
        public void renderBody(Ctx ctx, SpaceBody body) {
            Star star = (Star) body;
            GalaxyClient.requestPlanets(star);
            PoseStack pose = ctx.pose();
            double time = ctx.time();
            float distance = ctx.distance();
            pose.pushPose();
            pose.scale(star.getSize(), star.getSize(), star.getSize());
            int yellow = Galaxy.COLOR_HOLO_YELLOW;
            ctx.sink().geometry(pose, HoloRenderTypes.textured(PARTICLES), (p, vc) -> particle(ctx, p, vc, STAR_ICON,
                    star.getSize(), 0, 0, 0, Holo.r(yellow) * 0.1f, Holo.g(yellow) * 0.1f, Holo.b(yellow) * 0.1f));
            int sphereColor = Holo.mul(star.getColor() & 0xFFFFFF, 0.25f * (1f / distance));
            float s = (float) (0.9 + Math.sin(time * 0.01) * 0.1);
            pose.scale(s, s, s);
            ctx.sink().geometry(pose, HoloRenderTypes.LINE_TYPE, (p, vc) -> Holo.wireSphere(p, vc, 0.5, 16, 12, sphereColor));
            // 1.7.10 also drew the sphere's points (glPointSize(10 / distance))
            ctx.sink().geometry(pose, HoloRenderTypes.textured(PARTICLES), (p, vc) -> {
                for (int j = 1; j < 12; j++) {
                    double phi = Math.PI * j / 12;
                    for (int i = 0; i < 16; i++) {
                        double t = 2 * Math.PI * i / 16;
                        particle(ctx, p, vc, STAR_ICON, 0.02, Math.sin(phi) * Math.sin(t) * 0.5, Math.sin(phi) * Math.cos(t) * 0.5, Math.cos(phi) * 0.5,
                                Holo.r(sphereColor), Holo.g(sphereColor), Holo.b(sphereColor));
                    }
                }
            });
            long worldTime = (long) time;
            if (worldTime % 120 > 80) {
                double t = (worldTime % 120 - 80) / 40d;
                int pulse = Holo.mul(Galaxy.COLOR_HOLO_YELLOW, (float) easeIn(1 - t, 0, 0.1, 1));
                float grow = (float) (1 + easeIn(t, 0, 10, 1));
                pose.scale(grow, grow, grow);
                ctx.sink().geometry(pose, HoloRenderTypes.LINE_TYPE, (p, vc) -> Holo.wireSphere(p, vc, 0.5, 16, 12, pulse));
            }
            pose.popPose();
            int planetID = 0;
            for (Planet planet : star.getPlanets()) {
                float sizeMultiply = ctx.state().destination.is(planet) ? 1.2f : 1;
                int planetColor = planet.getGuiColor(ctx.player());
                random.setSeed(planet.getSeed());
                pose.pushPose();
                double axisRotation = random.nextInt(30) - 15;
                pose.mulPose(Axis.XP.rotationDegrees((float) axisRotation));
                double radius = planet.getOrbit() * 2 + (star.getSize() / 2 + 0.1);
                int orbitColor = Holo.mul(planetColor, 0.1f);
                ctx.sink().geometry(pose, HoloRenderTypes.LINE_TYPE, (p, vc) -> {
                    for (int i = 0; i < 32; i++) {
                        double a0 = Math.PI * 2 / 32 * i, a1 = Math.PI * 2 / 32 * (i + 1);
                        Holo.line(p, vc, Math.sin(a0) * radius, 0, Math.cos(a0) * radius, Math.sin(a1) * radius, 0, Math.cos(a1) * radius, orbitColor);
                    }
                });
                pose.translate(Math.sin(time * 0.001 + 10 * planetID) * radius, 0, Math.cos(time * 0.001 + 10 * planetID) * radius);
                if (ctx.state().destination.is(planet)) {
                    ctx.sink().geometry(pose, HoloRenderTypes.textured(PARTICLES), (p, vc) -> particle(ctx, p, vc, SELECTED_ICON,
                            planet.getSize() * 0.15f * sizeMultiply, 0, 0, 0, Holo.r(planetColor), Holo.g(planetColor), Holo.b(planetColor)));
                }
                if (ctx.state().position.is(planet)) {
                    ctx.sink().geometry(pose, HoloRenderTypes.textured(PARTICLES), (p, vc) -> particle(ctx, p, vc, CURRENT_ICON,
                            planet.getSize() * 0.25f, 0, 0, 0, Holo.r(planetColor), Holo.g(planetColor), Holo.b(planetColor)));
                }
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees((float) -axisRotation));
                drawPlanetInfo(ctx, planet);
                pose.popPose();
                int wire = Holo.mul(planetColor, 0.3f * (1f / distance));
                pose.mulPose(Axis.XP.rotationDegrees(100));
                pose.mulPose(Axis.ZP.rotationDegrees((float) (time * 2)));
                float planetSize = planet.getSize();
                ctx.sink().geometry(pose, HoloRenderTypes.LINE_TYPE, (p, vc) -> Holo.wireSphere(p, vc,
                        planetSize * 0.1f * sizeMultiply, (int) (16 + planetSize * 2), (int) (8 + planetSize * 2), wire));
                planetID++;
                pose.popPose();
            }
        }

        private void drawPlanetInfo(Ctx ctx, Planet planet) {
            PoseStack pose = ctx.pose();
            pose.translate(0, planet.getSize() * 0.13f + 0.05f, 0);
            pose.mulPose(ctx.orientation());
            pose.scale(0.005f, -0.005f, 0.005f);
            Player player = ctx.player();
            boolean known = player != null && GalaxyClient.canSeePlanetInfo(planet, player);
            int color = planet.getGuiColor(player);
            Font font = ctx.font();
            var name = Component.literal(planet.getName());
            if (!known) name = name.withStyle(s -> s.withFont(ALT));
            text(ctx, name.getString(), -font.width(name) / 2f, 0, color, 1, !known);
            if (known && player != null && planet.isHomeworld(player)) {
                text(ctx, "[Home]", -font.width("[Home]") / 2f, -10, 0xFFAA00, 1, false);
            } else if (!known && planet.hasOwner()) {
                Player owner = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getPlayerByUUID(planet.getOwnerUUID());
                if (owner != null) {
                    String info = "[" + owner.getDisplayName().getString() + "]";
                    text(ctx, info, -font.width(info) / 2f, -10, 0xFFAA00, 1, false);
                }
            }
        }

        @Override
        public void renderGuiInfo(Ctx ctx, SpaceBody body, float opacity) {
            Planet planet = ctx.galaxy().getPlanet(ctx.state().destination);
            if (planet == null) return;
            boolean known = ctx.player() != null && GalaxyClient.canSeePlanetInfo(planet, ctx.player());
            text(ctx, planet.getName(), 72, -42, Galaxy.COLOR_HOLO, opacity, !known);
            icon(ctx, "icon_size", 72, -28, Holo.mul(Galaxy.COLOR_HOLO, opacity));
            text(ctx, percent(planet.getSize()), 92, -23, Galaxy.COLOR_HOLO, opacity, false);
            // a made-up bar chart from the planet's seed
            random.setSeed(planet.getSeed());
            int color = Holo.mul(Galaxy.COLOR_HOLO, opacity);
            double[] heights = new double[10];
            for (int i = 0; i < 10; i++) heights[i] = 64 * (0.5 * random.nextGaussian() + 1d) / 2d;
            double step = 64d / 10d;
            for (int i = 0; i < 10; i++) {
                float x = (float) (step * i), y = -10, w = (float) (step - 1), h = (float) heights[i];
                ctx.sink().fill(ctx.pose(), x, y, x + w, y - h, color);
            }
        }
    };

    /** 1.7.10 MOMathHelper.easeIn: c * (t / d)^2 + b. */
    static double easeIn(double t, double b, double c, double d) {
        t /= d;
        return c * t * t + b;
    }

    static class PlanetBody implements Body {
        protected final Random random = new Random();

        @Override
        public double height() {
            return 1.5;
        }

        @Override
        public void renderBody(Ctx ctx, SpaceBody body) {
            renderPlanet(ctx, (Planet) body);
        }

        static float clampedSize(Planet planet) {
            return Math.min(Math.max(planet.getSize(), 1f), 2.2f) * 0.5f;
        }

        protected void renderPlanet(Ctx ctx, Planet planet) {
            PoseStack pose = ctx.pose();
            float size = clampedSize(planet);
            float distance = ctx.distance();
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(10));
            pose.mulPose(Axis.YP.rotationDegrees((float) (ctx.time() * 0.1)));
            ctx.sink().geometry(pose, HoloRenderTypes.DEPTH, (p, vc) -> Holo.solidSphere(p, vc, size * 0.99f, 64, 32, 0));
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90));
            int wire = Holo.mul(planet.getGuiColor(ctx.player()), 0.2f * (1f / distance));
            ctx.sink().geometry(pose, HoloRenderTypes.LINE_TYPE, (p, vc) -> Holo.wireSphere(p, vc, size, 64, 32, wire));
            pose.popPose();
            // 1.7.10 drawBuildings: a cube per building at a random spot of the surface
            random.setSeed(planet.getSeed());
            for (int i = 0; i < planet.getBuildings().size(); i++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees((float) (random.nextDouble() * 360)));
                pose.mulPose(Axis.ZP.rotationDegrees((float) (random.nextDouble() * 360)));
                pose.translate(size - 0.04, 0, 0);
                int cube = Holo.mul(Galaxy.COLOR_HOLO, 1f / distance);
                ctx.sink().geometry(pose, HoloRenderTypes.COLOR_QUADS, (p, vc) -> Holo.cube(p, vc, 0.1f, 0.1f, 0.1f, cube));
                pose.popPose();
            }
            pose.popPose();
            drawShips(ctx, planet, size);
        }

        /** 1.7.10 drawShips: each ship circles the planet on its own path (the ship icons come with phase 7u). */
        protected void drawShips(Ctx ctx, Planet planet, float planetSize) {
            random.setSeed(planet.getSeed());
            int pathColor = Holo.mul(planet.getGuiColor(ctx.player()), 0.2f);
            for (int i = 0; i < planet.getFleet().size(); i++) {
                double direction = random.nextDouble() * 2 - 1;
                double startingAngle = random.nextDouble() * Math.PI * 2;
                double phi = startingAngle + Math.copySign(ctx.time() * 0.005, direction);
                double theta = random.nextDouble() * Math.PI * 2;
                double radius = random.nextDouble() * 0.3 + 0.1 + planetSize;
                ctx.sink().geometry(ctx.pose(), HoloRenderTypes.LINE_TYPE, (p, vc) -> {
                    for (int s = 0; s < 7; s++) {
                        double a = phi - Math.copySign(0.1 * s, direction), b = phi - Math.copySign(0.1 * (s + 1), direction);
                        Holo.line(p, vc, Math.sin(a) * Math.sin(theta) * radius, Math.sin(a) * Math.cos(theta) * radius, Math.cos(a) * radius,
                                Math.sin(b) * Math.sin(theta) * radius, Math.sin(b) * Math.cos(theta) * radius, Math.cos(b) * radius, pathColor);
                    }
                });
            }
        }

        @Override
        public void renderGuiInfo(Ctx ctx, SpaceBody body, float opacity) {
            Planet planet = (Planet) body;
            Player player = ctx.player();
            boolean known = player != null && GalaxyClient.canSeePlanetInfo(planet, player);
            Font font = ctx.font();
            int x = 0, y = -16;
            if (known) {
                int factoryCount = planet.getFactoryCount();
                int color = factoryCount <= 0 ? Galaxy.COLOR_HOLO_RED : Galaxy.COLOR_HOLO;
                icon(ctx, "holo_factory", x, y, Holo.mul(color, opacity));
                String factoryInfo = factoryCount + "/" + planet.getBuildingSpaces();
                x += 18;
                text(ctx, factoryInfo, x, y + 6, color, opacity, false);
                int fleetCount = planet.getFleetCount();
                color = fleetCount <= 0 ? Galaxy.COLOR_HOLO_RED : Galaxy.COLOR_HOLO;
                String fleetInfo = fleetCount + "/" + planet.getFleetSpaces();
                x += font.width(factoryInfo) + 8;
                icon(ctx, "icon_shuttle", x, y, Holo.mul(color, opacity));
                x += 18;
                text(ctx, fleetInfo, x, y + 6, color, opacity, false);
                x += font.width(fleetInfo) + 8;
            }
            icon(ctx, "icon_size", x, y, Holo.mul(Galaxy.COLOR_HOLO, opacity));
            x += 18;
            text(ctx, percent(planet.getSize()), x, y + 6, Galaxy.COLOR_HOLO, opacity, false);
            if (known) {
                x = -2;
                y -= 20;
                float happiness = planet.getHappiness();
                int color = lerp(Galaxy.COLOR_HOLO_RED, Galaxy.COLOR_HOLO, Mth.clamp(happiness, 0, 1));
                icon(ctx, "smile", x, y, Holo.mul(color, opacity));
                x += 18;
                text(ctx, percent(happiness), x, y + 6, color, opacity, false);
                x += font.width(percent(happiness)) + 4;
                icon(ctx, "sort_random", x, y, Holo.mul(Galaxy.COLOR_HOLO, opacity));
                x += 18;
                text(ctx, String.format("%,d", planet.getPopulation()), x, y + 6, Galaxy.COLOR_HOLO, opacity, false);
                x = -3;
                y -= 20;
                int power = planet.getPowerProduction();
                color = power < 0 ? Galaxy.COLOR_HOLO_RED : Galaxy.COLOR_HOLO;
                icon(ctx, "battery", x, y, Holo.mul(color, opacity));
                x += 18;
                text(ctx, power + "mRF", x, y + 6, color, opacity, false);
            }
        }

        static int lerp(int from, int to, float t) {
            int r = (int) Mth.lerp(t, from >> 16 & 255, to >> 16 & 255), g = (int) Mth.lerp(t, from >> 8 & 255, to >> 8 & 255),
                    b = (int) Mth.lerp(t, from & 255, to & 255);
            return r << 16 | g << 8 | b;
        }
    }

    static final Body PLANET = new PlanetBody();

    /** 1.7.10 StarMapRenderPlanetStats: the player's planet and the selected one side by side, an arrow between them. */
    static final Body PLANET_STATS = new PlanetBody() {
        @Override
        public void renderBody(Ctx ctx, SpaceBody body) {
            Planet to = (Planet) body;
            Planet from = ctx.galaxy().getPlanet(ctx.state().position);
            PoseStack pose = ctx.pose();
            if (from != null && from != to) {
                Quaternionf yaw = Axis.YN.rotationDegrees(ctx.yaw());
                org.joml.Vector3f a = yaw.transform(new org.joml.Vector3f(clampedSize(from) + 0.25f, 0, 0));
                pose.pushPose();
                pose.translate(a.x, a.y, a.z);
                renderPlanet(ctx, from);
                pose.popPose();
                pose.pushPose();
                pose.mulPose(ctx.orientation());
                pose.scale(0.01f, -0.01f, 0.01f);
                icon(ctx, "arrow_right", -9, -9, Galaxy.COLOR_HOLO);
                pose.popPose();
                org.joml.Vector3f b = yaw.transform(new org.joml.Vector3f(-(clampedSize(to) + 0.25f), 0, 0));
                pose.pushPose();
                pose.translate(b.x, b.y, b.z);
                renderPlanet(ctx, to);
                pose.popPose();
            } else {
                renderPlanet(ctx, to);
            }
        }

        @Override
        public void renderGuiInfo(Ctx ctx, SpaceBody body, float opacity) {
            Planet planet = (Planet) body;
            int y = 0;
            for (var ship : planet.getFleet()) {
                text(ctx, ship.getHoverName().getString(), 36, y - 10, Galaxy.COLOR_HOLO, 1, false);
                y -= 16;
            }
        }
    };
}
