package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.monster.RangedRogueAndroid;
import matteroverdrive.entity.monster.RogueAndroid;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.HumanoidArm;

/**
 * 1.7.10 EntityRendererRougeAndroid / EntityRendererRangedRougeAndroid: a biped with the 1.7.10 android skins (64x32
 * melee, 96x64 ranged with a visor box lit in the android's level colour), 1.5x when legendary, the ranged one aiming.
 */
public class RogueAndroidRenderer<T extends RogueAndroid> extends HumanoidMobRenderer<T, RogueAndroidRenderer.State, HumanoidModel<RogueAndroidRenderer.State>> {
    public static final ModelLayerLocation MELEE = layer("rogue_android");
    public static final ModelLayerLocation RANGED = layer("ranged_rogue_android");
    public static final ModelLayerLocation VISOR = layer("ranged_rogue_android_visor");
    private static final ResourceLocation MELEE_TEXTURE = tex("android");
    private static final ResourceLocation RANGED_TEXTURE = tex("android_ranged");
    private final boolean ranged;

    public static class State extends HumanoidRenderState {
        int visorColor;
        boolean legendary;
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, name), "main");
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/" + name + ".png");
    }

    public static LayerDefinition meleeLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 32);
    }

    public static LayerDefinition rangedLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 96, 64);
    }

    /** 1.7.10 visorModel: an 8x8x8 box on the head at texture (64, 0); only it is drawn by this model. */
    public static LayerDefinition visorLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        var root = mesh.getRoot();
        for (String part : new String[] {"hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.01f)), PartPose.ZERO)
                .addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 96, 64);
    }

    public static RogueAndroidRenderer<RogueAndroid> melee(EntityRendererProvider.Context context) {
        return new RogueAndroidRenderer<>(context, MELEE, false);
    }

    public static RogueAndroidRenderer<RangedRogueAndroid> ranged(EntityRendererProvider.Context context) {
        return new RogueAndroidRenderer<>(context, RANGED, true);
    }

    private RogueAndroidRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, boolean ranged) {
        super(context, new HumanoidModel<>(context.bakeLayer(layer)), 0.5f);
        this.ranged = ranged;
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getEquipmentRenderer()));
        if (ranged) addLayer(new VisorLayer(this, new HumanoidModel<>(context.bakeLayer(VISOR))));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T android, State state, float partialTick) {
        super.extractRenderState(android, state, partialTick);
        state.visorColor = android.getVisorColor();
        state.legendary = android.isLegendary();
    }

    @Override
    public ResourceLocation getTextureLocation(State state) {
        return ranged ? RANGED_TEXTURE : MELEE_TEXTURE;
    }

    /** 1.7.10 setRenderPassModel: aimedBow for the ranged android. */
    @Override
    protected HumanoidModel.ArmPose getArmPose(T android, HumanoidArm arm) {
        return ranged && android.getMainArm() == arm && !android.getMainHandItem().isEmpty()
                ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.EMPTY;
    }

    @Override
    protected void scale(State state, PoseStack pose) {
        if (state.legendary) pose.scale(1.5f, 1.5f, 1.5f);
    }

    /** The visor at full brightness in the android's colour. */
    private static class VisorLayer extends RenderLayer<State, HumanoidModel<State>> {
        private final HumanoidModel<State> visor;

        VisorLayer(RenderLayerParent<State, HumanoidModel<State>> parent, HumanoidModel<State> visor) {
            super(parent);
            this.visor = visor;
        }

        @Override
        public void render(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, State state, float yRot, float xRot) {
            visor.setupAnim(state);
            visor.renderToBuffer(pose, buffers.getBuffer(matteroverdrive.compat.render.RenderTypes.entityTranslucentEmissive(RANGED_TEXTURE)), 0xF000F0,
                    OverlayTexture.NO_OVERLAY, ARGB.color(255, state.visorColor));
        }
    }
}
