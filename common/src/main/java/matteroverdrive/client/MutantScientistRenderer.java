package matteroverdrive.client;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.monster.MutantScientist;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** 1.7.10 EntityRendererMutantScientist: the hulking scientist model, no shadow. */
public class MutantScientistRenderer extends MobRenderer<MutantScientist, LivingEntityRenderState, HulkingScientistModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, "mutant_scientist"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MatterOverdrive.MODID,
            "textures/entity/hulking_scientist.png");

    public MutantScientistRenderer(EntityRendererProvider.Context context) {
        super(context, new HulkingScientistModel(context.bakeLayer(LAYER)), 0);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
