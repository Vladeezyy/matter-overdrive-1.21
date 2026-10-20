package matteroverdrive.menu;

import java.util.List;

import matteroverdrive.block.entity.PatternMonitorBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity.Task;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.matter.ItemPattern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** 1.7.10 GuiPatternMonitor: a pattern grid and the hotbar; patterns arrive by {@link matteroverdrive.network.PatternListPayload}. */
public class PatternMonitorMenu extends MachineMenu<PatternMonitorBlockEntity> {
    private List<ItemPattern> clientPatterns = List.of();
    private List<Task> clientQueue = List.of();
    private int version;

    public PatternMonitorMenu(int id, Inventory inventory, PatternMonitorBlockEntity machine, ContainerData data) {
        super(MOMenus.PATTERN_MONITOR.get(), id, inventory, machine, data);
    }

    public PatternMonitorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (PatternMonitorBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {}

    @Override
    protected boolean showMainInventory() {
        return false;
    }

    public void setClientPatterns(List<ItemPattern> patterns, List<Task> queue) {
        this.clientPatterns = List.copyOf(patterns);
        this.clientQueue = List.copyOf(queue);
        version++;
    }

    public List<ItemPattern> getClientPatterns() {
        return clientPatterns;
    }

    public List<Task> getClientQueue() {
        return clientQueue;
    }

    /** Changes whenever new data arrived, so the screen can rebuild its grid. */
    public int getVersion() {
        return version;
    }
}
