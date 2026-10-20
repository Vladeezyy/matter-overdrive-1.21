package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.client.PhaserBeamRenderer;
import matteroverdrive.client.WeaponClient;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    /** The stack renderArmWithItem is drawing (first person, main thread only) */
    @Unique
    private static ItemStack matteroverdrive$stack = ItemStack.EMPTY;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void matteroverdrive$beginArm(PlayerRenderState player, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState handsState, float partial, float pitch, InteractionHand hand, float swing,
                                          ItemStack stack, float equip, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, CallbackInfo ci) {
        matteroverdrive$stack = stack;
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void matteroverdrive$endArm(PlayerRenderState player, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState handsState, float partial, float pitch, InteractionHand hand, float swing,
                                        ItemStack stack, float equip, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, CallbackInfo ci) {
        matteroverdrive$stack = ItemStack.EMPTY;
    }

    /** NeoForge's IClientItemExtensions.applyForgeHandTransform: a weapon in use stays raised */
    @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$handTransform(PoseStack pose, HumanoidArm arm, float equip, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && matteroverdrive$stack.getItem() instanceof EnergyWeaponItem && WeaponClient.applyHandTransform(pose, player, arm, matteroverdrive$stack)) {
            ci.cancel();
        }
    }

    /** NeoForge's RenderHandEvent (main hand): the first person phaser beam */
    @Inject(method = "submitHandsWithItems", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            ordinal = 0))
    private void matteroverdrive$handBeam(float partial, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, PlayerRenderState player,
                                         net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState handsState, CallbackInfo ci) {
        PhaserBeamRenderer.renderHand(InteractionHand.MAIN_HAND, partial, pose, collector);
    }
}
