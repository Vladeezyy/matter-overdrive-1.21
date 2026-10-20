package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import matteroverdrive.android.Android;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** The android hooks NeoForge fires from LivingEntity: jumps, falls and incoming damage */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** NeoForge's LivingJumpEvent */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void matteroverdrive$onJump(CallbackInfo ci) {
        Android.onJump((LivingEntity) (Object) this);
    }

    /** NeoForge's LivingFallEvent: the fall distance */
    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float matteroverdrive$onFall(float distance) {
        return Android.onFall((LivingEntity) (Object) this, distance);
    }

    /** NeoForge's LivingIncomingDamageEvent: on the server, for damage the entity is not immune to (below 0 cancels) */
    @WrapMethod(method = "hurt")
    private boolean matteroverdrive$onIncomingDamage(DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && !self.isInvulnerableTo(source)) {
            amount = Android.onIncomingDamage(self, source, amount);
            if (amount < 0) return false;
        }
        return original.call(source, amount);
    }
}
