package matteroverdrive.menu;

import matteroverdrive.block.entity.ContractMarketBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/** 1.7.10 GuiContractMarket: 18 contract slots in two rows of nine (small slots), the time to the next contract. */
public class ContractMarketMenu extends MachineMenu<ContractMarketBlockEntity> {
    private final ContainerData data;

    public ContractMarketMenu(int id, Inventory inventory, ContractMarketBlockEntity machine, ContainerData data) {
        super(MOMenus.CONTRACT_MARKET.get(), id, inventory, machine, data);
        this.data = data;
    }

    public ContractMarketMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (ContractMarketBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(MachineBlockEntity.DATA_COUNT + 1));
    }

    /** 1.7.10: x = 45 + column * 18, y = ySize - 124 - row * 18 (the second row above the first). */
    @Override
    protected void addMachineSlots(MachineInventory inv) {
        for (int i = 0; i < ContractMarketBlockEntity.CONTRACT_SLOTS; i++) {
            homeSlot(inv, i, 46 + (i % 9) * 18, height() - 124 - (i / 9) * 18 + 1);
        }
    }

    public int getSecondsUntilNextQuest() {
        return data.get(MachineBlockEntity.DATA_COUNT);
    }
}
