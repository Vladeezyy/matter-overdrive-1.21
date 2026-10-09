package matteroverdrive.client.guide;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.network.GuideRecipePayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Crafting grids for the guide's recipe elements: the client has no recipes in 1.21, so the server sends them. */
public final class GuideRecipes {
    private static final Map<Item, List<List<ItemStack>>> GRIDS = new HashMap<>();
    private static final Set<Item> ASKED = new HashSet<>();

    static void request(Item item) {
        if (ASKED.add(item)) PacketDistributor.sendToServer(new GuideRecipePayload.Request(item));
    }

    static @Nullable List<List<ItemStack>> get(Item item) {
        return GRIDS.get(item);
    }

    public static void receive(GuideRecipePayload.Grid grid) {
        GRIDS.put(grid.item(), grid.slots());
    }

    private GuideRecipes() {}
}
