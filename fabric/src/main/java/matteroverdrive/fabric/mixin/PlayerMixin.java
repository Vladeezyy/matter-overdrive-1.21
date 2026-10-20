package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.android.Android;
import matteroverdrive.quest.QuestEvents;
import net.minecraft.world.entity.player.Player;

/** NeoForge's PlayerTickEvent.Post */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void matteroverdrive$afterTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        QuestEvents.onPlayerTick(self);
        Android.onPlayerTick(self);
    }
}
