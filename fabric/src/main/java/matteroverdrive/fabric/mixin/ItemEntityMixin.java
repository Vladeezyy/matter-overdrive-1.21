package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.matter.MatterEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** NeoForge's ItemEntityPickupEvent.Pre: on the server, once the item may be picked up */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    private int pickupDelay;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void matteroverdrive$onPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide() && this.pickupDelay <= 0 && !self.isRemoved()) MatterEvents.onPickup(player, self);
    }
}
