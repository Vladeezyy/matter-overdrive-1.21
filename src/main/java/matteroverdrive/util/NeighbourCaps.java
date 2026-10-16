package matteroverdrive.util;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity's view of one capability on its six neighbours, looked up once per side and kept by NeoForge's
 * {@link BlockCapabilityCache} (which drops a side when that neighbour changes). Machines that push energy or matter
 * every few ticks used {@code level.getCapability} each time: a block entity lookup and a provider search per side.
 */
public final class NeighbourCaps<T> {
    private final BlockCapability<T, Direction> capability;
    private final BlockEntity owner;
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<T, Direction>[] caches = new BlockCapabilityCache[6];

    public NeighbourCaps(BlockCapability<T, Direction> capability, BlockEntity owner) {
        this.capability = capability;
        this.owner = owner;
    }

    /** The capability of the neighbour on {@code side}, facing this block; null on the client or when there is none. */
    public @Nullable T get(Direction side) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return null;
        BlockCapabilityCache<T, Direction> cache = caches[side.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(capability, level, owner.getBlockPos().relative(side), side.getOpposite(),
                    () -> !owner.isRemoved(), () -> {});
            caches[side.ordinal()] = cache;
        }
        return cache.getCapability();
    }
}
