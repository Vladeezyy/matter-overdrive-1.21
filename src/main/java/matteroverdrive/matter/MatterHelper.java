package matteroverdrive.matter;

import matteroverdrive.item.MatterDustItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 MatterHelper.getMatterAmountFromItem: refined matter dust carries its own amount. */
public final class MatterHelper {
    public static int getMatter(MinecraftServer server, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof MatterDustItem dust) {
            return dust.isRefined() ? MatterDustItem.getMatter(stack) : 0;
        }
        return MatterRegistry.get(server, stack.getItem());
    }

    /** Whether an item can go into a decomposer (1.7.10 MatterSlot); works on either side. */
    public static boolean hasMatter(ItemStack stack) {
        if (stack.getItem() instanceof MatterDustItem dust) {
            return dust.isRefined() && MatterDustItem.getMatter(stack) > 0;
        }
        return MatterRegistry.getAnySide(stack.getItem()) > 0;
    }

    private MatterHelper() {}
}
