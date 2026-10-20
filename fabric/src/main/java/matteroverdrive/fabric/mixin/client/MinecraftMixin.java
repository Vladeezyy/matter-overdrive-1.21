package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import matteroverdrive.client.WeaponClient;
import net.minecraft.client.Minecraft;

/** NeoForge's InteractionKeyMappingTriggered for attacks: a weapon in use takes the attack key (no swing either) */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$startAttack(CallbackInfoReturnable<Boolean> cir) {
        if (WeaponClient.cancelsAttack()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$continueAttack(boolean leftClick, CallbackInfo ci) {
        if (leftClick && WeaponClient.cancelsAttack()) ci.cancel();
    }
}
