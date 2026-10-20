package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import matteroverdrive.client.android.AndroidHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** NeoForge's RenderGuiLayerEvent.Pre for the vanilla layers an android's HUD replaces */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$hearts(GuiGraphics graphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth,
                                        int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.HEALTH)) ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$food(GuiGraphics graphics, Player player, int y, int x, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.FOOD)) ci.cancel();
    }

    /** The air bubbles are the only sprites renderPlayerHealth draws itself */
    @WrapWithCondition(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private boolean matteroverdrive$air(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        return !AndroidHud.hides(AndroidHud.VanillaLayer.AIR);
    }

    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$tabList(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.TAB_LIST)) ci.cancel();
    }
}
