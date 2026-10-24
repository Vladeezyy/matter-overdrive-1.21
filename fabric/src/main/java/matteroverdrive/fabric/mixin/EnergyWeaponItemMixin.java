package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;

import matteroverdrive.item.weapon.EnergyWeaponItem;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** NeoForge's shouldCauseReequipAnimation: charging or heating a weapon in hand does not re-equip it */
@Mixin(EnergyWeaponItem.class)
public abstract class EnergyWeaponItemMixin implements FabricItem {
    @Override
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return ((EnergyWeaponItem) (Object) this).shouldCauseReequipAnimation(oldStack, newStack, false);
    }
}
