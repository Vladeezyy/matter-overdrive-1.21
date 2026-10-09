package matteroverdrive.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** 1.21.1: containers aren't Iterable&lt;ItemStack&gt; yet (1.21.5+); lists every slot of one. */
public final class ContainerItems {
    private ContainerItems() {}

    public static List<ItemStack> of(Container container) {
        List<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) stacks.add(container.getItem(i));
        return stacks;
    }
}
