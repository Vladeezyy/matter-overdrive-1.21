package matteroverdrive.platform.transfer;

import net.minecraft.world.item.ItemStack;

/** An {@link IItemHandler} whose slots can be set directly (NeoForge's {@code IItemHandlerModifiable}) */
public interface IItemHandlerModifiable extends IItemHandler {
    void setStackInSlot(int slot, ItemStack stack);
}
