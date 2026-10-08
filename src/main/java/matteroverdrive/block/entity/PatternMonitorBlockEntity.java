package matteroverdrive.block.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matteroverdrive.block.entity.ReplicatorBlockEntity.Task;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.matter.ItemPattern;
import matteroverdrive.matternet.MatterNetwork;
import matteroverdrive.menu.PatternMonitorMenu;
import matteroverdrive.network.PatternListPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 TileEntityMachinePatternMonitor: shows every pattern stored on its matter network and queues
 * replication requests, which it hands to idle replicators on the same network.
 */
public class PatternMonitorBlockEntity extends MachineBlockEntity {
    public static final int DISPATCH_DELAY = 20;
    public static final int MAX_QUEUE = 16;

    private final List<Task> queue = new ArrayList<>();

    public PatternMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.PATTERN_MONITOR.get(), pos, state, withFilterSlot(MachineInventory.builder()), false, 0, 0, 0, 0, Set.of());
    }

    /** All patterns on the network, one per item (the most complete one wins). */
    public List<ItemPattern> networkPatterns() {
        Map<Item, ItemPattern> byItem = new LinkedHashMap<>();
        for (PatternStorageBlockEntity storage : MatterNetwork.of(getLevel(), getBlockPos(), getNetworkFilter()).storages()) {
            for (ItemPattern p : storage.getPatterns()) {
                byItem.merge(p.item().value(), p, (a, b) -> a.progress() >= b.progress() ? a : b);
            }
        }
        return new ArrayList<>(byItem.values());
    }

    public List<Task> getQueue() {
        return List.copyOf(queue);
    }

    /** Queues requests from the GUI; only patterns that are really on the network count, at most 64 each. */
    public void request(List<Task> requests) {
        List<ItemPattern> available = networkPatterns();
        for (Task req : requests) {
            for (ItemPattern p : available) {
                if (p.item().value() == req.pattern().item().value() && queue.size() < MAX_QUEUE) {
                    queue.add(new Task(p, Math.max(1, Math.min(64, req.count()))));
                }
            }
        }
        setChanged();
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (queue.isEmpty() || getLevel().getGameTime() % DISPATCH_DELAY != 0) return !queue.isEmpty();
        for (ReplicatorBlockEntity replicator : MatterNetwork.of(getLevel(), getBlockPos(), getNetworkFilter()).replicators()) {
            if (replicator.isIdle()) {
                replicator.setTask(queue.remove(0));
                setChanged();
                break;
            }
        }
        return !queue.isEmpty();
    }

    public void sendPatterns(ServerPlayer player, int containerId) {
        PacketDistributor.sendToPlayer(player, new PatternListPayload(containerId, networkPatterns(), getQueue()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var list = output.list("queue", Task.CODEC);
        queue.forEach(list::add);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        queue.clear();
        input.listOrEmpty("queue", Task.CODEC).stream().forEach(queue::add);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PatternMonitorMenu(id, inventory, this, dataAccess);
    }
}
