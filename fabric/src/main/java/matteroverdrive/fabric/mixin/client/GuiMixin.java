package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.client.android.AndroidHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/** NeoForge's RenderGuiLayerEvent.Pre for the vanilla layers an android's HUD replaces */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$hearts(GuiGraphicsExtractor graphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth,
                                        int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.HEALTH)) ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$food(GuiGraphicsExtractor graphics, Player player, int y, int x, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.FOOD)) ci.cancel();
    }

    @Inject(method = "renderAirBubbles", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$air(GuiGraphicsExtractor graphics, Player player, int y, int x, int vehicleRows, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.AIR)) ci.cancel();
    }

    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$tabList(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (AndroidHud.hides(AndroidHud.VanillaLayer.TAB_LIST)) ci.cancel();
    }
}
