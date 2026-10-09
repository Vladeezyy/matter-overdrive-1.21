package matteroverdrive.client;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.block.entity.PatternMonitorBlockEntity;
import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity;
import matteroverdrive.client.starmap.HoloRenderTypes;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.machine.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Sheets;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import matteroverdrive.compat.render.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import matteroverdrive.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * The 1.7.10 machine tile entity renderers: pattern storage drives (TileEntityRendererPatterStorage), the replicator's
 * output item (TileEntityRendererReplicator), the inscriber's moving rail / head and its item (TileEntityRendererInscriber)
 * and the monitors' holo screens (TileEntityRendererMonitor / PatternMonitor / ContractMarket).
 */
public final class MachineRenderers {
    public static final StandaloneModelKey<BlockStateModel> STORAGE_DRIVE = key("pattern_storage_drive");
    public static final StandaloneModelKey<BlockStateModel> INSCRIBER_RAIL = key("inscriber_rail");
    public static final StandaloneModelKey<BlockStateModel> INSCRIBER_HEAD = key("inscriber_head");

    private static StandaloneModelKey<BlockStateModel> key(String name) {
        return new StandaloneModelKey<>(new ModelDebugName() {
            @Override
            public String debugName() {
                return MatterOverdrive.MODID + ":" + name;
            }
        });
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(STORAGE_DRIVE, SimpleUnbakedStandaloneModel.blockStateModel(id("block/pattern_storage_drive")));
        event.register(INSCRIBER_RAIL, SimpleUnbakedStandaloneModel.blockStateModel(id("block/inscriber_rail")));
        event.register(INSCRIBER_HEAD, SimpleUnbakedStandaloneModel.blockStateModel(id("block/inscriber_head")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private static BlockStateModel model(StandaloneModelKey<BlockStateModel> key) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
    }

    /** The block model's facing rotation (blockstate y: north 0, east 90, south 180, west 270) about the block centre. */
    static float facingYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** 1.7.10 RenderUtils.rotateFromBlock, for the OBJ models (they face +z): south 0, west 90, north 180, east 270. */
    static float objFacingYaw(Direction facing) {
        return facingYaw(facing) + 180;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean active;
        double time;
        long seed;
        final ItemStackRenderState item = new ItemStackRenderState();
        boolean[] drives = new boolean[0];
        int count;
    }

    abstract static class Base<T extends MachineBlockEntity> implements matteroverdrive.compat.render.StateBlockEntityRenderer<T, State> {
        protected final ItemModelResolver items;

        Base(BlockEntityRendererProvider.Context context) {
            this.items = context.getItemModelResolver();
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(T machine, State state, float partialTick, Vec3 camera, @Nullable Object crumbling) {
            matteroverdrive.compat.render.StateBlockEntityRenderer.super.extractRenderState(machine, state, partialTick, camera, crumbling);
            var blockState = machine.getBlockState();
            state.facing = blockState.hasProperty(MachineBlock.FACING) ? blockState.getValue(MachineBlock.FACING) : Direction.NORTH;
            state.active = blockState.hasProperty(MachineBlock.ACTIVE) && blockState.getValue(MachineBlock.ACTIVE);
            state.time = machine.getLevel() == null ? 0 : machine.getLevel().getGameTime() + partialTick;
            state.seed = machine.getBlockPos().asLong();
        }

        /** Block centre at the floor, turned like the block model: the 1.7.10 renderers' frame (x / z centred). */
        protected void facingFrame(PoseStack pose, State state, double y) {
            pose.translate(0.5, y, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-modelYaw(state.facing)));
        }

        /** The blockstate's y rotation for this block's model. */
        protected float modelYaw(Direction facing) {
            return facingYaw(facing);
        }

        protected void part(PoseStack pose, SubmitNodeCollector collector, State state, StandaloneModelKey<BlockStateModel> key) {
            pose.pushPose();
            pose.translate(-0.5, 0, -0.5);   // the OBJ models are shifted to the block corner
            collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), model(key), 1, 1, 1, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /** 1.7.10: each drive slot with a drive shows a drive in the front, 3 rows in 2 columns. */
    public static class PatternStorage extends Base<PatternStorageBlockEntity> {
        public PatternStorage(BlockEntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        protected float modelYaw(Direction facing) {
            return objFacingYaw(facing);
        }

        @Override
        public void extractRenderState(PatternStorageBlockEntity storage, State state, float partialTick, Vec3 camera,
                                       @Nullable Object crumbling) {
            super.extractRenderState(storage, state, partialTick, camera, crumbling);
            state.drives = new boolean[PatternStorageBlockEntity.DRIVES];
            for (int i = 0; i < state.drives.length; i++) state.drives[i] = !storage.getInventory().getStack(i).isEmpty();
        }

        @Override
        public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            pose.pushPose();
            facingFrame(pose, state, 0.5);
            for (int i = 0; i < state.drives.length; i++) {
                if (!state.drives[i]) continue;
                pose.pushPose();
                pose.translate(i >= 3 ? -0.3 : 0.3, 0.1 - 0.2 * (i % 3), -0.2);
                part(pose, collector, state, STORAGE_DRIVE);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** 1.7.10: the output stack floats in the replicator like a dropped item (never ticked: bob 0.2, turned 90 degrees). */
    public static class Replicator extends Base<ReplicatorBlockEntity> {
        public Replicator(BlockEntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        protected float modelYaw(Direction facing) {
            return objFacingYaw(facing);
        }

        @Override
        public void extractRenderState(ReplicatorBlockEntity replicator, State state, float partialTick, Vec3 camera,
                                       @Nullable Object crumbling) {
            super.extractRenderState(replicator, state, partialTick, camera, crumbling);
            items.updateForTopItem(state.item, replicator.getInventory().getStack(ReplicatorBlockEntity.OUTPUT), ItemDisplayContext.GROUND,
                    replicator.getLevel(), null, (int) state.seed);
            // the machine is a full opaque block, so its own light is 0: light the item from in front of the opening
            state.lightCoords = net.minecraft.client.renderer.LevelRenderer.getLightColor(replicator.getLevel(),
                    replicator.getBlockPos().relative(state.facing));
        }

        @Override
        public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.item.isEmpty()) return;
            pose.pushPose();
            pose.translate(0.5, 0.25, 0.5);
            // 1.7.10 turned the item 90 degrees in world space (edge-on through a south window): face the window instead
            pose.mulPose(Axis.YP.rotationDegrees(-modelYaw(state.facing)));
            state.item.render(pose, collector.buffers(), state.lightCoords, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /**
     * 1.7.10: while inscribing the head moves to a new random spot every 20 ticks (gaussian -1..1, lerped): the rail
     * slides along z, the head along x on it; the item being inscribed lies on the bed.
     */
    public static class Inscriber extends Base<InscriberBlockEntity> {
        public Inscriber(BlockEntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        protected float modelYaw(Direction facing) {
            return objFacingYaw(facing);
        }

        @Override
        public void extractRenderState(InscriberBlockEntity inscriber, State state, float partialTick, Vec3 camera,
                                       @Nullable Object crumbling) {
            super.extractRenderState(inscriber, state, partialTick, camera, crumbling);
            ItemStack stack = inscriber.getInventory().getStack(InscriberBlockEntity.MAIN);
            if (stack.isEmpty()) stack = inscriber.getInventory().getStack(InscriberBlockEntity.OUTPUT);
            items.updateForTopItem(state.item, stack, ItemDisplayContext.GROUND, inscriber.getLevel(), null, (int) state.seed);
        }

        private static float target(long seed, long step, int axis) {
            if (step < 0) return 0;
            var random = net.minecraft.util.RandomSource.create(seed * 31 + step * 2 + axis);
            return Mth.clamp((float) random.nextGaussian(), -1, 1);
        }

        @Override
        public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            float lerpX = 0, lerpY = 0;
            if (state.active) {
                long step = (long) Math.floor(state.time / 20);
                float t = (float) (state.time / 20 - step);
                lerpX = Mth.lerp(t, target(state.seed, step - 1, 0), target(state.seed, step, 0));
                lerpY = Mth.lerp(t, target(state.seed, step - 1, 1), target(state.seed, step, 1));
            }
            double headX = 0.15 * lerpX + 0.02, headY = 0.1 * lerpY;
            pose.pushPose();
            facingFrame(pose, state, 0);
            pose.pushPose();
            pose.translate(0, 0.6, headX);
            part(pose, collector, state, INSCRIBER_RAIL);
            pose.popPose();
            pose.pushPose();
            pose.translate(headY, 0.84, headX - 0.06);
            part(pose, collector, state, INSCRIBER_HEAD);
            pose.popPose();
            if (!state.item.isEmpty()) {
                pose.pushPose();
                pose.translate(-0.23, 0.69, 0);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                state.item.render(pose, collector.buffers(), state.lightCoords, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /**
     * 1.7.10 TileEntityRendererMonitor: in front of the panel a flickering glow, a dark back and the holo picture in
     * COLOR_HOLO * 0.7 (screen blending), plus the pattern monitor's pattern count.
     */
    public static class Monitor<T extends MachineBlockEntity> extends Base<T> {
        private static final ResourceLocation GLOW = id("textures/fx/holo_monitor_glow.png");
        private static final ResourceLocation BACK = id("textures/block/pattern_monitor_holo_back.png");
        private final ResourceLocation holo;
        private final Font font;

        public Monitor(BlockEntityRendererProvider.Context context, String holo) {
            super(context);
            this.holo = id("textures/block/" + holo + ".png");
            this.font = context.getFont();
        }

        @Override
        public void extractRenderState(T machine, State state, float partialTick, Vec3 camera, @Nullable Object crumbling) {
            super.extractRenderState(machine, state, partialTick, camera, crumbling);
            state.count = machine instanceof PatternMonitorBlockEntity monitor ? monitor.getPatternCount() : -1;
        }

        private static void plane(PoseStack pose, SubmitNodeCollector collector, ResourceLocation texture, int rgb) {
            collector.submitCustomGeometry(pose, HoloRenderTypes.screen(texture), (p, vc) -> {
                float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
                vc.addVertex(p, 0, 0, 0).setUv(0, 0).setColor(r, g, b, 1);
                vc.addVertex(p, 0, 1, 0).setUv(0, 1).setColor(r, g, b, 1);
                vc.addVertex(p, 1, 1, 0).setUv(1, 1).setColor(r, g, b, 1);
                vc.addVertex(p, 1, 0, 0).setUv(1, 0).setColor(r, g, b, 1);
            });
        }

        private static int mul(int rgb, double m) {
            return Mth.clamp((int) ((rgb >> 16 & 255) * m), 0, 255) << 16 | Mth.clamp((int) ((rgb >> 8 & 255) * m), 0, 255) << 8
                    | Mth.clamp((int) ((rgb & 255) * m), 0, 255);
        }

        @Override
        public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            int holoColor = 0xA9E2FB;
            pose.pushPose();
            // the north-facing frame: screen 0.15 in front of the centre, x / y running right / down as seen from the front
            facingFrame(pose, state, 0.5);
            pose.translate(0, 0, 0.15);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.translate(-0.5, -0.5, 0);
            // 1.7.10 noise(x, worldTime * 0.01, z) * 0.5 + 0.5
            double noise = Mth.sin((float) (state.time * 0.01 + (state.seed & 0xFF))) * 0.5 * Mth.cos((float) (state.time * 0.023)) + 0.5;
            plane(pose, collector, GLOW, mul(holoColor, noise));
            pose.translate(0, 0, -0.05);
            plane(pose, collector, BACK, mul(holoColor, 0.05));
            pose.translate(0, 0, -0.05);
            plane(pose, collector, holo, mul(holoColor, 0.7));
            if (state.count >= 0) {
                String text = String.valueOf(state.count);
                double scale = Math.min(1, (double) font.width("10") / font.width(text));
                pose.translate(0.47, 0.33 + (font.lineHeight * 0.03) * (1 - scale) * 0.5, -0.001);
                pose.scale((float) (scale * 0.03), (float) (scale * 0.03), (float) (scale * 0.03));
                collector.submitText(pose, 0, 0, Component.literal(text).getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET,
                        0xF000F0, 0xFF78A1B3, 0, 0);
            }
            pose.popPose();
        }
    }

    private MachineRenderers() {}
}
