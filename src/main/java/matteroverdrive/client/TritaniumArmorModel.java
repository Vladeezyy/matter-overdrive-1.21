package matteroverdrive.client;

import java.util.EnumMap;
import java.util.Map;

import matteroverdrive.MatterOverdrive;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 1.7.10 ModelTritaniumArmor (Techne, 64x64 Tritanium_Armor2 textures): the biped boxes plus a visor, chest and back
 * plates with their supports, shoulder pads and boot toes. Like 1.7.10 TritaniumArmor.getArmorModel the helmet shows
 * only the head, the chestplate the body and arms, the leggings only the legs and the boots the legs (inflated by
 * 0.5) with the toes; the boots use layer 2, everything else layer 1. One baked model per slot: armor is drawn later
 * from the submitted model, so a shared model's visibility can't change between slots.
 * <p>
 * The model sits on the skin (inflate 0) like 1.7.10, where skins had no outer layer. A modern skin's outer layer
 * (hat +0.5, jacket / sleeves / pants +0.25) would cover it, so a player's overlay is hidden under each worn piece:
 * the helmet hides the hat, the chestplate the jacket and sleeves, the leggings and boots the pants.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = MatterOverdrive.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class TritaniumArmorModel {
    public static final ResourceLocation LAYER_1 = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/equipment/humanoid/tritanium.png");
    public static final ResourceLocation LAYER_2 = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/entity/equipment/humanoid_leggings/tritanium.png");
    private static final Map<EquipmentSlot, HumanoidModel<HumanoidRenderState>> MODELS = new EnumMap<>(EquipmentSlot.class);

    public static LayerDefinition createLayer(float expand) {
        CubeDeformation d = new CubeDeformation(expand);
        MeshDefinition mesh = HumanoidModel.createMesh(d, 0);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("visor_upper", CubeListBuilder.create().texOffs(42, 8).addBox(-4, -8.4f, 0.5f, 8, 2, 3, d),
                PartPose.rotation(0.8464847f, 0, 0));
        head.addOrReplaceChild("visor_front", CubeListBuilder.create().texOffs(44, 0).addBox(-4, -6, -6, 8, 6, 2, d), PartPose.ZERO);
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(0, 32).addBox(-4, 0, -5, 8, 6, 3, d), PartPose.ZERO);
        body.addOrReplaceChild("chestplate_support_left", CubeListBuilder.create().texOffs(0, 41).addBox(-4, 1, -7.5f, 2, 4, 2, d),
                PartPose.rotation(0.7504916f, 0, 0));
        body.addOrReplaceChild("chestplate_support_right", CubeListBuilder.create().texOffs(14, 41).addBox(2, 1, -7.5f, 2, 4, 2, d),
                PartPose.rotation(0.7504916f, 0, 0));
        body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(42, 33).addBox(-4, 0, 2, 8, 6, 3, d), PartPose.ZERO);
        body.addOrReplaceChild("backplate_support", CubeListBuilder.create().texOffs(44, 42).addBox(-4, 3, 4, 8, 4, 2, d),
                PartPose.rotation(-0.3828089f, 0, 0));
        PartDefinition leftArm = root.getChild("left_arm");
        leftArm.addOrReplaceChild("shoulder_left", CubeListBuilder.create().texOffs(0, 47).addBox(4.5f, -4.2f, -1, 2, 3, 4, d),
                PartPose.offsetAndRotation(0, 4, -1, 0, 0, -0.5159565f));
        leftArm.addOrReplaceChild("shoulder_l1", CubeListBuilder.create().texOffs(0, 55).addBox(3, -0.2f, -2, 2, 2, 4, d), PartPose.ZERO);
        PartDefinition rightArm = root.getChild("right_arm");
        rightArm.addOrReplaceChild("shoulder_r1", CubeListBuilder.create().texOffs(14, 55).addBox(-4.5f, -0.2f, -2, 2, 2, 4, d), PartPose.ZERO);
        rightArm.addOrReplaceChild("shoulder_right", CubeListBuilder.create().texOffs(14, 47).addBox(-4, -1, -2, 2, 3, 4, d),
                PartPose.rotation(0, 0, 0.5159542f));
        root.getChild("left_leg").addOrReplaceChild("foot_left", CubeListBuilder.create().texOffs(29, 59).addBox(-1, 11, -3, 2, 1, 1, d), PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("foot_right", CubeListBuilder.create().texOffs(36, 59).addBox(-1, 11, -3, 2, 1, 1, d), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The baked model for one slot, with 1.7.10's part visibility. */
    public static HumanoidModel<HumanoidRenderState> forSlot(EquipmentSlot slot) {
        return MODELS.computeIfAbsent(slot, s -> {
            HumanoidModel<HumanoidRenderState> model = new HumanoidModel<>(createLayer(s == EquipmentSlot.FEET ? 0.5f : 0).bakeRoot());
            model.setAllVisible(false);
            model.head.visible = s == EquipmentSlot.HEAD;
            model.hat.visible = false;   // 1.7.10 bipedHeadwear.isHidden
            model.body.visible = s == EquipmentSlot.CHEST;
            model.rightArm.visible = model.leftArm.visible = s == EquipmentSlot.CHEST;
            model.rightLeg.visible = model.leftLeg.visible = s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET;
            model.leftLeg.getChild("foot_left").visible = model.rightLeg.getChild("foot_right").visible = s == EquipmentSlot.FEET;
            return model;
        });
    }

    /** Client extensions of the four tritanium armor pieces. */
    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
            // the poses come from the render state in setupAnim; don't copy the vanilla model's visibility
            return forSlot(slot(stack));
        }

        @Override
        public ResourceLocation getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, ResourceLocation _default) {
            return slot(stack) == EquipmentSlot.FEET ? LAYER_2 : LAYER_1;
        }
    };

    @net.neoforged.bus.api.SubscribeEvent
    static void hideSkinOverlay(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre<?> event) {
        var state = event.getRenderState();
        if (isTritanium(state.headEquipment)) state.showHat = false;
        if (isTritanium(state.chestEquipment)) {
            state.showJacket = false;
            state.showLeftSleeve = false;
            state.showRightSleeve = false;
        }
        if (isTritanium(state.legsEquipment) || isTritanium(state.feetEquipment)) {
            state.showLeftPants = false;
            state.showRightPants = false;
        }
    }

    private static boolean isTritanium(ItemStack stack) {
        return stack.is(matteroverdrive.init.MOItems.TRITANIUM_HELMET.get()) || stack.is(matteroverdrive.init.MOItems.TRITANIUM_CHESTPLATE.get())
                || stack.is(matteroverdrive.init.MOItems.TRITANIUM_LEGGINGS.get()) || stack.is(matteroverdrive.init.MOItems.TRITANIUM_BOOTS.get());
    }

    private static EquipmentSlot slot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null ? EquipmentSlot.CHEST : equippable.slot();
    }

    private TritaniumArmorModel() {}
}
