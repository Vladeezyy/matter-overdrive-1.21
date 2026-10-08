package matteroverdrive.block.entity;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.item.ContractItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.menu.ContractMarketMenu;
import matteroverdrive.quest.Quest;
import matteroverdrive.quest.QuestStack;
import matteroverdrive.quest.Quests;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 1.7.10 TileEntityMachineContractMarket: while the redstone mode allows it, puts a new random contract (by the
 * contract weights, never one equal to a contract already there) into its 18 slots every 30 minutes + 5 minutes per
 * filled slot. No energy, no upgrades.
 */
public class ContractMarketBlockEntity extends MachineBlockEntity {
    public static final int QUEST_GENERATE_DELAY_MIN = 20 * 60 * 30;
    public static final int QUEST_GENERATE_DELAY_PER_SLOT = 20 * 60 * 5;
    public static final int CONTRACT_SLOTS = 18;

    private long lastGenerationTime;

    public ContractMarketBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.CONTRACT_MARKET.get(), pos, state, slots(), false, 0, 0, 0, 0, Set.of());
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        for (int i = 0; i < CONTRACT_SLOTS; i++) b.add(MachineInventory.Role.OTHER, r -> r.getItem() instanceof ContractItem, 64);
        return b;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (redstoneAllows && getTimeUntilNextQuest() <= 0) generateContract();
        return false;
    }

    /** 1.7.10 generateContract: a duplicate of a contract already in the market means no contract (try again next tick). */
    private void generateContract() {
        Quest quest = Quests.randomContract(level.random);
        QuestStack stack = quest.generate(level.random);
        for (int i = 0; i < inventory.size(); i++) {
            QuestStack other = ContractItem.getQuest(inventory.getStack(i));
            if (other != null && quest.areQuestStacksEqual(stack, other)) return;
        }
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                inventory.setStack(i, ContractItem.of(stack));
                break;
            }
        }
        addGenerationDelay();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** 1.7.10 addGenerationDelay: 30 minutes + 5 per filled slot. */
    public void addGenerationDelay() {
        int filled = 0;
        for (int i = 0; i < inventory.size(); i++) if (!inventory.getStack(i).isEmpty()) filled++;
        lastGenerationTime = (level == null ? 0 : level.getGameTime()) + QUEST_GENERATE_DELAY_MIN + (long) filled * QUEST_GENERATE_DELAY_PER_SLOT;
        setChanged();
    }

    public int getTimeUntilNextQuest() {
        return level == null ? 0 : (int) Math.max(0, lastGenerationTime - level.getGameTime());
    }

    /** The remaining time for the open menu (index DATA_COUNT; ticks / 20 fit a short). */
    public final ContainerData marketData = new ContainerData() {
        @Override
        public int get(int index) {
            return index < DATA_COUNT ? dataAccess.get(index) : getTimeUntilNextQuest() / 20;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT + 1;
        }
    };

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContractMarketMenu(id, inventory, this, marketData);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("last_generation_time", lastGenerationTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lastGenerationTime = input.getLongOr("last_generation_time", 0);
    }

    public ItemStack getContract(int slot) {
        return inventory.getStack(slot);
    }
}
