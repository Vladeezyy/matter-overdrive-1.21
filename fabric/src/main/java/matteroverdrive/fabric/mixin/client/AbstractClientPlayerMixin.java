package matteroverdrive.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import matteroverdrive.client.WeaponClient;
import net.minecraft.client.player.AbstractClientPlayer;

/** NeoForge's ComputeFovModifierEvent: weapon zoom */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
    private float matteroverdrive$fov(float modifier) {
        return WeaponClient.fovModifier((AbstractClientPlayer) (Object) this, modifier);
    }
}
