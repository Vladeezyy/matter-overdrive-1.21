package matteroverdrive.matternet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The matter network that a block belongs to: every block reachable through adjacent {@link MatterNetworkBlock}s.
 * 1.7.10 routed packets hop by hop through routers and switches; here the network is a shared bus that is
 * looked up when needed (an analysis finishes, a monitor refreshes or dispatches a task).
 */
public final class MatterNetwork {
    /** Upper bound on visited blocks, so a huge pipe network can't stall a tick. */
    private static final int MAX_BLOCKS = 4096;

    private final List<BlockEntity> nodes;

    private MatterNetwork(List<BlockEntity> nodes) {
        this.nodes = nodes;
    }

    /**
     * The network with a destination filter (1.7.10 packets carrying a filter only reached the listed connections):
     * null = every machine.
     */
    public static MatterNetwork of(Level level, BlockPos start, java.util.@org.jetbrains.annotations.Nullable List<BlockPos> filter) {
        MatterNetwork network = of(level, start);
        if (filter == null) return network;
        return new MatterNetwork(network.nodes.stream().filter(n -> filter.contains(n.getBlockPos())).toList());
    }

    public static MatterNetwork of(Level level, BlockPos start) {
        List<BlockEntity> nodes = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) nodes.add(be);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!seen.contains(next) && level.isLoaded(next) && level.getBlockState(next).getBlock() instanceof MatterNetworkBlock) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return new MatterNetwork(nodes);
    }

    public List<PatternStorageBlockEntity> storages() {
        return nodes.stream().filter(n -> n instanceof PatternStorageBlockEntity).map(n -> (PatternStorageBlockEntity) n).toList();
    }

    public List<ReplicatorBlockEntity> replicators() {
        return nodes.stream().filter(n -> n instanceof ReplicatorBlockEntity).map(n -> (ReplicatorBlockEntity) n).toList();
    }

    /** The first powered pattern storage that can take more progress for {@code item}. */
    public PatternStorageBlockEntity storageAccepting(Item item) {
        for (PatternStorageBlockEntity s : storages()) {
            if (s.canAccept(item)) return s;
        }
        return null;
    }
}
