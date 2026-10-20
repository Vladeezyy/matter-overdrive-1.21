package matteroverdrive.fabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import matteroverdrive.matter.MatterEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** NeoForge's AnvilUpdateEvent: a result of the mod's replaces vanilla's */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    private int repairItemCountCost;
    @Shadow
    @Final
    private DataSlot cost;

    private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access, net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition slots) {
        super(type, containerId, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void matteroverdrive$onAnvil(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        if (left.isEmpty()) return;
        var result = MatterEvents.onAnvil(left, this.inputSlots.getItem(1));
        if (result == null) return;
        this.resultSlots.setItem(0, result.output());
        this.cost.set(result.cost());
        this.repairItemCountCost = result.materialCost();
        ci.cancel();
    }
}
