package matteroverdrive.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import matteroverdrive.entity.monster.MutantScientist;
import net.minecraft.util.Mth;

/**
 * 1.7.10 ModelHulkingScientist: a hunched brute walking on bent legs, arms hanging from the tilted chest. The 1.7.10
 * parts set {@code mirror} after adding their boxes, so nothing is mirrored.
 */
public class HulkingScientistModel extends HierarchicalModel<MutantScientist> {
    private final ModelPart root;
    private final ModelPart lowerBody;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart lowerRightArm;
    private final ModelPart lowerLeftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart lowerRightLeg;
    private final ModelPart lowerLeftLeg;

    public HulkingScientistModel(ModelPart root) {
        this.root = root;
        lowerBody = root.getChild("lower_body");
        body = lowerBody.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        lowerRightArm = rightArm.getChild("lower_right_arm");
        lowerLeftArm = leftArm.getChild("lower_left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        lowerRightLeg = rightLeg.getChild("lower_right_leg");
        lowerLeftLeg = leftLeg.getChild("lower_left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition lowerBody = root.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(40, 0).addBox(-4, -7, -2, 8, 9, 4),
                PartPose.offset(0, 11, 0));
        PartDefinition body = lowerBody.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 48).addBox(-6, -7, -3, 12, 9, 7),
                PartPose.offsetAndRotation(0, -7, -2, 0.6723132f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -8, -4, 6, 8, 6),
                PartPose.offset(0, -6, -1));
        head.addOrReplaceChild("lower_jaw", CubeListBuilder.create().texOffs(0, 14).addBox(-4, 0, 0, 8, 3, 4), PartPose.offset(0, -2, -5.5f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1, -5, 0, 2, 3, 1), PartPose.offset(0, 0, -5));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 35).addBox(-3, -2, -2, 4, 10, 3),
                PartPose.offsetAndRotation(-7, -4, 1, -0.0436332f, 0, 0.3351032f));
        rightArm.addOrReplaceChild("lower_right_arm", CubeListBuilder.create().texOffs(38, 51).addBox(-1.5f, 0, -2, 4, 10, 3),
                PartPose.offsetAndRotation(-1.5f, 7, 0.5f, -1.041993f, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(14, 35).addBox(-1, -2, -2, 4, 10, 3),
                PartPose.offsetAndRotation(7, -4, 1, -0.0436332f, 0, -0.3351032f));
        leftArm.addOrReplaceChild("lower_left_arm", CubeListBuilder.create().texOffs(38, 38).addBox(-1.5f, 0, -2, 4, 10, 3),
                PartPose.offsetAndRotation(0.5f, 7, 0.5f, -1.041996f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 21).addBox(-2, 0, -2, 4, 10, 4),
                PartPose.offsetAndRotation(-3, 11, 1, -0.9f, 0.418879f, 0));
        rightLeg.addOrReplaceChild("lower_right_leg", CubeListBuilder.create().texOffs(48, 26).addBox(-1.5f, 0, 0, 4, 8, 4),
                PartPose.offsetAndRotation(-0.5f, 10, -2, 1, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 21).addBox(-2, 0, -2, 4, 10, 4),
                PartPose.offsetAndRotation(3, 11, 1, -0.7f, -0.418879f, 0));
        leftLeg.addOrReplaceChild("lower_left_leg", CubeListBuilder.create().texOffs(32, 26).addBox(-1.5f, 0, 0, 4, 8, 4),
                PartPose.offsetAndRotation(-0.5f, 10, -2, 1, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(MutantScientist entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float swing = limbSwing * 0.6662f;
        float amount = limbSwingAmount;
        body.xRot = Mth.cos(swing + Mth.PI) * 1.6f * amount * 0.3f + 0.5f;
        lowerBody.xRot = Mth.sin(swing) * 1.6f * amount * 0.3f - 0.1f;
        lowerBody.y = Mth.sin(swing) * 1.6f * amount * 3 + 11;

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD - 0.3f;

        rightArm.xRot = Mth.cos(swing + Mth.HALF_PI) * 1.6f * amount * 0.8f;
        leftArm.xRot = Mth.cos(swing) * 1.6f * amount * 0.8f;
        lowerRightArm.xRot = Mth.cos(swing + Mth.PI) * 0.2f * amount - 1;
        lowerLeftArm.xRot = Mth.cos(swing) * 0.2f * amount - 1;

        rightLeg.xRot = Mth.cos(swing) * 0.7f * amount - 0.7f;
        rightLeg.y = Mth.sin(swing) * 1.4f * amount * 3 + 11;
        leftLeg.xRot = Mth.cos(swing + Mth.PI) * 0.7f * amount - 0.7f;
        leftLeg.y = Mth.sin(swing) * 1.4f * amount * 3 + 11;
        lowerRightLeg.xRot = Mth.cos(swing) * 0.8f * amount + 1;
        lowerLeftLeg.xRot = Mth.cos(swing + Mth.PI) * 0.8f * amount + 1;

        body.yRot = Mth.sin(swing) * 0.2f * amount;
    }
}
