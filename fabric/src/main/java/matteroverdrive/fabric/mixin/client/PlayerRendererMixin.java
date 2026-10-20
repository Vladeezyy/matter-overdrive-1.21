package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import matteroverdrive.client.WeaponClient;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

/** NeoForge's IClientItemExtensions.getArmPose: weapons aim down the line of sight while in use */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void matteroverdrive$armPose(net.minecraft.world.entity.Avatar player, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (stack.getItem() instanceof EnergyWeaponItem) {
            HumanoidModel.ArmPose pose = WeaponClient.armPose(player, hand);
            if (pose != null) cir.setReturnValue(pose);
        }
    }
}
