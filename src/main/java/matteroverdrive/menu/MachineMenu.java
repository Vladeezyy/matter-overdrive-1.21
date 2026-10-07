package matteroverdrive.menu;

import matteroverdrive.item.UpgradeItem;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.RedstoneMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Container for a machine GUI, laid out like the 1.7.10 MOGuiMachine (225x186): machine slots on the Home page,
 * upgrade slots on the Upgrades page, the player's inventory at the bottom.
 */
public abstract class MachineMenu<T extends MachineBlockEntity> extends AbstractContainerMenu {
    public enum Page { HOME, UPGRADES, CONFIG }

    public static final int WIDTH = 225;
    public static final int HEIGHT = 186;
    public static final int BUTTON_REDSTONE = 0;

    protected final T machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final int machineSlotCount;
    /** Client-side page shown by the screen; slots on other pages are inactive. */
    public Page page = Page.HOME;

    protected MachineMenu(MenuType<?> type, int id, Inventory playerInventory, T machine, ContainerData data) {
        super(type, id);
        this.machine = machine;
        this.data = data;
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
        MachineInventory inv = machine.getInventory();

        addMachineSlots(inv);
        int upgradeIndex = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.spec(i).role() == MachineInventory.Role.UPGRADE) {
                addSlot(new PageSlot(inv, i, 79 + (upgradeIndex % 5) * 24, 55 + (upgradeIndex / 5) * 24, Page.UPGRADES));
                upgradeIndex++;
            }
        }
        machineSlotCount = slots.size();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerSlot(playerInventory, col + row * 9 + 9, 46 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 46 + col * 18, HEIGHT - 26));
        }
        addDataSlots(data);
    }

    /** Client constructor helper: the client copy of the machine and an empty data mirror. */
    protected static ContainerData clientData() {
        return new SimpleContainerData(MachineBlockEntity.DATA_COUNT);
    }

    /** Adds the machine's own slots (Home page) with {@link #homeSlot}. */
    protected abstract void addMachineSlots(MachineInventory inv);

    protected Slot homeSlot(MachineInventory inv, int index, int x, int y) {
        return addSlot(new PageSlot(inv, index, x, y, Page.HOME));
    }

    public T getMachine() {
        return machine;
    }

    public int getEnergy() {
        return data.get(0) | data.get(1) << 16;
    }

    public int getCapacity() {
        return data.get(2) | data.get(3) << 16;
    }

    public int getMatter() {
        return data.get(7) | data.get(8) << 16;
    }

    public int getMatterCapacity() {
        return data.get(9) | data.get(10) << 16;
    }

    public float getProgress() {
        return data.get(4) / 1000f;
    }

    public boolean isActive() {
        return data.get(5) != 0;
    }

    public RedstoneMode getRedstoneMode() {
        return RedstoneMode.values()[Math.floorMod(data.get(6), RedstoneMode.values().length)];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_REDSTONE) {
            machine.cycleRedstoneMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, machine.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlotCount;
        int playerEnd = slots.size();
        if (index < machineSlotCount) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            // Upgrades go to upgrade slots, everything else to the machine slots that accept it.
            boolean moved = false;
            for (int i = 0; i < machineSlotCount && !stack.isEmpty(); i++) {
                PageSlot target = (PageSlot) slots.get(i);
                boolean upgradeSlot = target.role() == MachineInventory.Role.UPGRADE;
                if (upgradeSlot != stack.getItem() instanceof UpgradeItem) continue;
                if (target.role() == MachineInventory.Role.OUTPUT || !target.mayPlace(stack)) continue;
                moved |= moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** A slot of the machine inventory that only shows on one page. */
    public class PageSlot extends ResourceHandlerSlot {
        private final Page slotPage;
        private final MachineInventory inv;
        private final int index;

        PageSlot(MachineInventory inv, int index, int x, int y, Page page) {
            super(inv, inv::set, index, x, y);
            this.inv = inv;
            this.index = index;
            this.slotPage = page;
        }

        public MachineInventory.Role role() {
            return inv.spec(index).role();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return role() != MachineInventory.Role.OUTPUT && inv.isValid(index, ItemResource.of(stack));
        }

        @Override
        public boolean isActive() {
            return page == slotPage;
        }
    }

    /** Main player inventory rows: only on the Home page, as in 1.7.10 (the hotbar is always shown). */
    public class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isActive() {
            return page == Page.HOME;
        }
    }
}
