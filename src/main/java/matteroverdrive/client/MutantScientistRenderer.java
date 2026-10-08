package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.monster.MutantScientist;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;

/** 1.7.10 EntityRendererMutantScientist: the hulking scientist model, no shadow. */
public class MutantScientistRenderer extends MobRenderer<MutantScientist, LivingEntityRenderState, HulkingScientistModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "mutant_scientist"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID,
            "textures/entity/hulking_scientist.png");

    public MutantScientistRenderer(EntityRendererProvider.Context context) {
        super(context, new HulkingScientistModel(context.bakeLayer(LAYER)), 0);
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
