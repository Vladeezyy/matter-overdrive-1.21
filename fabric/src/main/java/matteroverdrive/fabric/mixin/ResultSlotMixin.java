package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.quest.QuestEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

/** NeoForge's ItemCraftedEvent (fired from the same method) */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow
    @Final
    private Player player;
    @Shadow
    private int removeCount;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void matteroverdrive$onCrafted(ItemStack stack, CallbackInfo ci) {
        if (this.removeCount > 0) QuestEvents.onCrafted(this.player, stack);
    }
}
