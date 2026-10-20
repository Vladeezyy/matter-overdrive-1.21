package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.client.PhaserBeamRenderer;
import matteroverdrive.client.TritaniumArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;

/** NeoForge's RenderPlayerEvent (players are submitted by LivingEntityRenderer.submit from their render state since 1.21.9) */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    /** RenderPlayerEvent.Pre: no skin overlay under the tritanium armor */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet.minecraft.client.renderer.state.level.CameraRenderState;)V",
            at = @At("HEAD"))
    private void matteroverdrive$beforeRender(LivingEntityRenderState state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof AvatarRenderState player) TritaniumArmorModel.hideSkinOverlay(player);
    }

    /** RenderPlayerEvent.Post: the phaser beam */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet.minecraft.client.renderer.state.level.CameraRenderState;)V",
            at = @At("TAIL"))
    private void matteroverdrive$afterRender(LivingEntityRenderState state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (state instanceof AvatarRenderState player && mc.level != null && mc.level.getEntity(player.id) instanceof Player entity) {
            PhaserBeamRenderer.renderPlayer(entity, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), pose, collector);
        }
    }
}
