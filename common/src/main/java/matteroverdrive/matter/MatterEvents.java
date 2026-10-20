package matteroverdrive.matter;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.network.MatterValuesPayload;
import matteroverdrive.network.PatternListPayload;
import matteroverdrive.network.PatternRequestPayload;
import matteroverdrive.platform.Platform;

/** Matter on both loaders: the loaders call these from their own events */
public final class MatterEvents {
    /** 1.7.10 EntityHandler.onEntityItemPickup: portable decomposers in the hotbar eat the listed items first. */
    public static void onPickup(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.item.ItemEntity itemEntity) {
        var item = itemEntity.getItem();
        if (player.level().isClientSide() || item.isEmpty()) return;
        if (MatterHelper.hasMatter(item)) {
            for (int i = 0; i < 9 && !item.isEmpty(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof matteroverdrive.item.PortableDecomposerItem) {
                    matteroverdrive.item.PortableDecomposerItem.decompose(player.level().getServer(), stack, item);
                }
            }
        }
        // then the quests (1.7.10 extendedProperties.onEvent), which may take items too (cocktail ingredients)
        matteroverdrive.quest.QuestEvents.onEvent(player, new matteroverdrive.quest.QuestEvents.Pickup(itemEntity));
        item = itemEntity.getItem();
        if (item.isEmpty()) itemEntity.discard();
        else itemEntity.setItem(item);
    }

    /** An anvil result: output, level cost, material cost */
    public record AnvilResult(net.minecraft.world.item.ItemStack output, int cost, int materialCost) {}

    /** 1.7.10 PlayerEventHandler.onAnvilRepair: decomposer + an item adds that item to its list (3 levels, 1 item). */
    @org.jetbrains.annotations.Nullable
    public static AnvilResult onAnvil(net.minecraft.world.item.ItemStack left, net.minecraft.world.item.ItemStack right) {
        if (!(left.getItem() instanceof matteroverdrive.item.PortableDecomposerItem) || right.isEmpty()) return null;
        var output = left.copy();
        if (!matteroverdrive.item.PortableDecomposerItem.addToList(output, right)) return null;
        return new AnvilResult(output, 3, 1);
    }

    /** Recipes and matter values were (re)loaded on the server: recalculate on next use. */
    public static void onServerDataLoaded() {
        MatterRegistry.invalidate();
    }

    /** Datapacks synced to these players (all of them after a reload, one on login) */
    public static void onDatapackSync(net.minecraft.server.MinecraftServer server, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
        var payload = new MatterValuesPayload(MatterRegistry.values(server), MatterRegistry.estimated(server));
        // only clients that have this mod's channel (not vanilla clients or game-test mock players)
        players.stream().filter(player -> Platform.INSTANCE.canSend(player, MatterValuesPayload.TYPE))
                .forEach(player -> Platform.INSTANCE.sendToPlayer(player, payload));
    }

    private MatterEvents() {}
}
