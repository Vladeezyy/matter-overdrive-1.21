package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.starmap.GalaxyServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;

/** NeoForge's LevelEvent.Save: the galaxy is saved with the overworld */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void matteroverdrive$onSave(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (!skipSave) GalaxyServer.onLevelSave((ServerLevel) (Object) this);
    }
}
