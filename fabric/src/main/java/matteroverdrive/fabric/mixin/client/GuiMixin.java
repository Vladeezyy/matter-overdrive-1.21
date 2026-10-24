package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import matteroverdrive.client.android.AndroidHud;
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

    /** 1.20.1 draws the food and air icons inline in renderPlayerHealth: food at v 27, air bubbles at v 18 of the icons */
    @WrapWithCondition(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private boolean matteroverdrive$foodAndAir(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        if (v == 27) return !AndroidHud.hides(AndroidHud.VanillaLayer.FOOD);
        if (v == 18) return !AndroidHud.hides(AndroidHud.VanillaLayer.AIR);
        return true;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
    private boolean matteroverdrive$tabList(net.minecraft.client.gui.components.PlayerTabOverlay overlay, GuiGraphics graphics, int width,
                                            net.minecraft.world.scores.Scoreboard scoreboard, net.minecraft.world.scores.Objective objective) {
        return !AndroidHud.hides(AndroidHud.VanillaLayer.TAB_LIST);
    }
}
