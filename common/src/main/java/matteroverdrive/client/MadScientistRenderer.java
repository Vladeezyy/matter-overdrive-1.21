package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.MadScientist;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

/** 1.7.10 EntityRendererMadScientist: the villager model with the mad scientist texture. */
public class MadScientistRenderer extends MobRenderer<MadScientist, VillagerModel<MadScientist>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/mad_scientist.png");

    public MadScientistRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(MadScientist entity) {
        return TEXTURE;
    }
}
