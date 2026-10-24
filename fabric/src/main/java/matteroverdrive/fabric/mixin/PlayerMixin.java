package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.android.Android;
import matteroverdrive.platform.PlayerDataStore;
import matteroverdrive.quest.QuestEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * NeoForge's PlayerTickEvent.Post and LivingDamageEvent.Post for players, and the player data (1.20.1 Fabric API has
 * no data attachments: the store lives here and is saved with the player)
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerDataStore.Holder {
    @Unique
    private static final String MATTEROVERDRIVE_DATA = "matteroverdrive:data";
    @Unique
    private final PlayerDataStore matteroverdrive$data = new PlayerDataStore();

    @Override
    public PlayerDataStore matteroverdrive$playerData() {
        return matteroverdrive$data;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void matteroverdrive$afterTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        QuestEvents.onPlayerTick(self);
        Android.onPlayerTick(self);
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void matteroverdrive$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide && !self.isInvulnerableTo(source)) Android.onDamaged(self, amount);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void matteroverdrive$save(CompoundTag tag, CallbackInfo ci) {
        tag.put(MATTEROVERDRIVE_DATA, matteroverdrive$data.serializeNBT(((Player) (Object) this).level().registryAccess()));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void matteroverdrive$load(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(MATTEROVERDRIVE_DATA)) {
            matteroverdrive$data.deserializeNBT(((Player) (Object) this).level().registryAccess(), tag.getCompound(MATTEROVERDRIVE_DATA));
        }
    }
}
