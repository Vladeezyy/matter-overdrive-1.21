package matteroverdrive.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/** Bridge for entity renderers written against the 1.21.9+ submit(state, pose, collector, camera). */
public abstract class StateEntityRenderer<T extends Entity, S extends EntityRenderState> extends EntityRenderer<T, S> {
    protected StateEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void submit(S state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {}

    @Override
    public void render(S state, PoseStack pose, MultiBufferSource buffers, int light) {
        submit(state, pose, new SubmitNodeCollector(buffers), CameraRenderState.current());
        super.render(state, pose, buffers, light);
    }
}
