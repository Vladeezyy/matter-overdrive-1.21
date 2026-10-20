package matteroverdrive.menu;

import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidData;
import matteroverdrive.block.entity.AndroidStationBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.item.BatteryItem;
import matteroverdrive.item.android.BionicPartItem;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 ContainerAndroidStation: the player's android slots (head, arms, legs, chest, other, battery) in the large
 * android station GUI (364x240), stats on the left, the player's inventory at the bottom.
 */
public class AndroidStationMenu extends MachineMenu<AndroidStationBlockEntity> {
    /** 1.7.10 GuiAndroidStation slot elements (20x20 holo slots, item inset by 2). */
    public static final int[][] PART_POS = {{222, 134}, {222, 162}, {222, 190}, {322, 134}, {322, 162}, {322, 190}};

    private int firstPartSlot;

    public AndroidStationMenu(int id, Inventory inventory, AndroidStationBlockEntity machine, ContainerData data) {
        super(MOMenus.ANDROID_STATION.get(), id, inventory, machine, data);
    }

    public AndroidStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (AndroidStationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    public int width() {
        return 364;
    }

    @Override
    public int height() {
        return 240;     // 1.7.10: 250, which doesn't fit a 240-high scaled screen
    }

    @Override
    protected int inventoryY() {
        return 150;
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        firstPartSlot = slots.size();
        Parts parts = new Parts(playerInventory.player);
        for (int i = 0; i < AndroidData.SLOTS; i++) {
            addSlot(new PartSlot(parts, i));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && Android.isAndroid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slot instanceof PartSlot) {
            if (!moveItemStackTo(stack, firstPartSlot + AndroidData.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, firstPartSlot, firstPartSlot + AndroidData.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** The android inventory of the player using the station. */
    private static class Parts implements Container {
        private final Player player;

        Parts(Player player) {
            this.player = player;
        }

        private AndroidData data() {
            return Android.get(player);
        }

        @Override
        public int getContainerSize() {
            return AndroidData.SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < AndroidData.SLOTS; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return data().getStack(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            ItemStack taken = stack.split(amount);
            setItem(slot, stack);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            data().setStack(slot, stack);
            Android.sync(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {}
    }

    /** 1.7.10 BionicSlot / EnergySlot: a part of the slot's type, or a battery. */
    public class PartSlot extends Slot {
        public final int part;

        PartSlot(Container container, int part) {
            super(container, part, PART_POS[part][0], PART_POS[part][1]);
            this.part = part;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (part == AndroidData.SLOT_BATTERY) return stack.getItem() instanceof BatteryItem;
            return stack.getItem() instanceof BionicPartItem bionic && bionic.getSlot() == part;
        }

        @Override
        public boolean isActive() {
            return page == Page.HOME;
        }
    }
}
