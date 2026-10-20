package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import matteroverdrive.client.WeaponClient;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** NeoForge's IClientItemExtensions.getArmPose: weapons aim down the line of sight while in use */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState$HandState;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void matteroverdrive$armPose(PlayerRenderState state, PlayerRenderState.HandState handState, InteractionHand hand,
                                                CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(state.id) instanceof Player player && player.getItemInHand(hand).getItem() instanceof EnergyWeaponItem) {
            HumanoidModel.ArmPose pose = WeaponClient.armPose(player, hand);
            if (pose != null) cir.setReturnValue(pose);
        }
    }
}
