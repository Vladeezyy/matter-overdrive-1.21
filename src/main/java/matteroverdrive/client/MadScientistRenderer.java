package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.MadScientist;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;

/** 1.7.10 EntityRendererMadScientist: the villager model with the mad scientist texture. */
public class MadScientistRenderer extends MobRenderer<MadScientist, VillagerRenderState, VillagerModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/mad_scientist.png");

    public MadScientistRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(MadScientist entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isUnhappy = entity.getUnhappyCounter() > 0;
    }
}
