package matteroverdrive.menu;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.starmap.BuildingItem;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.ShipItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 ContainerStarMap: 4 slots that are the selected planet's construction slots (2 buildings, 2 ships; the map's
 * own slots while no planet is selected), then the player's inventory. The screen places every slot itself.
 */
public class StarMapMenu extends AbstractContainerMenu {
    public static final int SLOTS = Planet.SLOT_COUNT;
    private final StarMapBlockEntity starMap;
    private final Player player;

    public StarMapMenu(int id, Inventory inventory, StarMapBlockEntity starMap) {
        super(MOMenus.STAR_MAP.get(), id);
        this.starMap = starMap;
        this.player = inventory.player;
        Container planetSlots = new PlanetSlots();
        for (int i = 0; i < SLOTS; i++) addSlot(new StarMapSlot(planetSlots, i));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 0, 0));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 0, 0));
    }

    public StarMapMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (StarMapBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public StarMapBlockEntity getStarMap() {
        return starMap;
    }

    private @Nullable Planet planet() {
        return starMap.getPlanet();
    }

    @Override
    public boolean stillValid(Player player) {
        return !starMap.isRemoved() && player.canInteractWithBlock(starMap.getBlockPos(), 4) && starMap.isUseableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** 1.7.10 TileEntityMachineStarMap.getInventory: the selected planet, else the map's own slots. */
    private class PlanetSlots implements Container {
        @Override
        public int getContainerSize() {
            return SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < SLOTS; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            Planet planet = planet();
            return planet != null ? planet.getStackInSlot(slot) : starMap.getInventory().getStack(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = stack.copyWithCount(Math.min(amount, stack.getCount()));
            setItem(slot, stack.getCount() > taken.getCount() ? stack.copyWithCount(stack.getCount() - taken.getCount()) : ItemStack.EMPTY);
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
            Planet planet = planet();
            if (planet != null) {
                planet.setStackInSlot(slot, stack);
                if (!player.level().isClientSide()) planet.markDirty();
            } else {
                starMap.getInventory().setStack(slot, stack);
            }
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            starMap.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return StarMapMenu.this.stillValid(player);
        }

        /** 1.7.10 isItemValidForSlot: on someone else's planet nothing; building slots first, then ship slots. */
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            Planet planet = planet();
            if (planet != null && !planet.isOwner(player)) return false;
            return slot < SLOTS / 2 ? stack.getItem() instanceof BuildingItem : stack.getItem() instanceof ShipItem;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < SLOTS; i++) setItem(i, ItemStack.EMPTY);
        }
    }

    /** 1.7.10 SlotStarMap: only the planet's owner takes from it. */
    private class StarMapSlot extends Slot {
        StarMapSlot(Container container, int index) {
            super(container, index, -1000, -1000);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            Planet planet = planet();
            return planet == null || planet.isOwner(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        /** 1.7.10 onItemPlaced / onItemPickup: construction (re)starts now. */
        @Override
        public void set(ItemStack stack) {
            restart(stack);
            super.set(stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            restart(stack);
            super.onTake(player, stack);
        }

        private void restart(ItemStack stack) {
            if (!player.level().isClientSide() && stack.getItem() instanceof matteroverdrive.starmap.Buildable buildable) {
                buildable.setBuildStart(stack, player.level().getGameTime());
            }
        }
    }
}
