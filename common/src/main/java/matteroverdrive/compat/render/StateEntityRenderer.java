package matteroverdrive.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** 1.21.1 bridge for entity renderers written against the 1.21.2+ render-state EntityRenderer (non-living entities). */
public abstract class StateEntityRenderer<T extends Entity, S extends EntityRenderState> extends EntityRenderer<T> {
    protected StateEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public abstract S createRenderState();

    public void extractRenderState(T entity, S state, float partialTick) {
        state.extractBase(entity, partialTick);
    }

    public void submit(S state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {}

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        S state = createRenderState();
        extractRenderState(entity, state, partialTick);
        state.lightCoords = light;
        submit(state, pose, new SubmitNodeCollector(buffers), CameraRenderState.current());
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
