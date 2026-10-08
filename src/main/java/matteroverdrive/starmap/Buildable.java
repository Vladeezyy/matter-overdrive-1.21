package matteroverdrive.starmap;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.7.10 IBuildable: built in a planet's construction slot over its build length; owned by a player. */
public interface Buildable {
    boolean canBuild(ItemStack stack, Planet planet, List<Component> info);

    int getBuildLength(ItemStack stack, Planet planet);

    long getBuildStart(ItemStack stack);

    void setBuildStart(ItemStack stack, long buildStart);

    boolean isReadyToBuild(Level level, ItemStack stack, Planet planet);

    @Nullable UUID getOwnerID(ItemStack stack);

    void setOwner(ItemStack stack, UUID owner);

    default boolean isOwner(ItemStack stack, Player player) {
        return player.getUUID().equals(getOwnerID(stack));
    }

    default long getRemainingBuildTimeTicks(ItemStack stack, Planet planet, Level level) {
        return getBuildStart(stack) + getBuildLength(stack, planet) - level.getGameTime();
    }
}
