package matteroverdrive.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21.1 bridge for renderers written against the 1.21.9+ BlockEntityRenderer (render state + submit): each
 * render call extracts a fresh state and submits it into the frame's buffers.
 */
public interface StateBlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends BlockEntityRenderer<T> {
    S createRenderState();

    default void extractRenderState(T blockEntity, S state, float partialTick, Vec3 cameraPosition, Object breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state);
    }

    void submit(S state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera);

    @Override
    default void render(T blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        S state = createRenderState();
        state.lightCoords = light;
        CameraRenderState camera = CameraRenderState.current();
        extractRenderState(blockEntity, state, partialTick, camera.pos, null);
        state.lightCoords = light;
        submit(state, pose, new SubmitNodeCollector(buffers), camera);
    }

    /** 1.21.10 shouldRenderOffScreen() has no block entity argument. */
    default boolean shouldRenderOffScreen() {
        return false;
    }

    @Override
    default boolean shouldRenderOffScreen(T blockEntity) {
        return shouldRenderOffScreen();
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
