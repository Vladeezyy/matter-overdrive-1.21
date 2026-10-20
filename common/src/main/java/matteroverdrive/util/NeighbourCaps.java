package matteroverdrive.util;

import matteroverdrive.platform.Platform;
import matteroverdrive.platform.transfer.StorageKind;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity's view of one kind of storage on its six neighbours, looked up once per side and kept by the loader's
 * cache (NeoForge BlockCapabilityCache, Fabric BlockApiCache, both drop a side when that neighbour changes). Machines
 * that push energy or matter every few ticks used a full lookup each time: a block entity lookup and a provider
 * search per side.
 */
public final class NeighbourCaps<T> {
    private final StorageKind<T> kind;
    private final BlockEntity owner;
    @SuppressWarnings("unchecked")
    private final Platform.BlockLookup<T>[] caches = new Platform.BlockLookup[6];

    public NeighbourCaps(StorageKind<T> kind, BlockEntity owner) {
        this.kind = kind;
        this.owner = owner;
    }

    /** The storage of the neighbour on {@code side}, facing this block; null on the client or when there is none. */
    public @Nullable T get(Direction side) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return null;
        Platform.BlockLookup<T> cache = caches[side.ordinal()];
        if (cache == null) {
            cache = Platform.INSTANCE.blockLookup(kind, level, owner.getBlockPos().relative(side), side.getOpposite(), () -> !owner.isRemoved());
            caches[side.ordinal()] = cache;
        }
        return cache.get();
    }
}
