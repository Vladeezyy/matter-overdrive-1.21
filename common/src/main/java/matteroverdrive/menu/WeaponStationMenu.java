package matteroverdrive.menu;

import matteroverdrive.block.entity.WeaponStationBlockEntity;
import matteroverdrive.init.MOMenus;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.WeaponModule;
import matteroverdrive.machine.MachineInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7.10 ContainerWeaponStation: the weapon slot plus five module slots (battery, colour, barrel, sights, other) that
 * read and write the modules installed in that weapon.
 */
public class WeaponStationMenu extends MachineMenu<WeaponStationBlockEntity> {
    public static final int WEAPON_X = 8, WEAPON_Y = 55;
    /** 1.7.10 getSlotPosition (the same for every weapon), scaled into the 225x186 machine frame. */
    public static final int[][] MODULE_POS = {{158, 77}, {70, 31}, {70, 77}, {142, 25}, {186, 54}};

    private int firstModuleSlot;

    public WeaponStationMenu(int id, Inventory inventory, WeaponStationBlockEntity machine, ContainerData data) {
        super(MOMenus.WEAPON_STATION.get(), id, inventory, machine, data);
    }

    public WeaponStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (WeaponStationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), clientData());
    }

    @Override
    protected void addMachineSlots(MachineInventory inv) {
        homeSlot(inv, WeaponStationBlockEntity.WEAPON, WEAPON_X, WEAPON_Y);
        firstModuleSlot = slots.size();
        Modules modules = new Modules();
        for (int i = 0; i < WeaponModule.SLOTS; i++) {
            addSlot(new ModuleSlot(modules, i, MODULE_POS[i][0], MODULE_POS[i][1]));
        }
    }

    public ItemStack getWeapon() {
        return machine.getWeapon();
    }

    /** Whether the weapon in the station has this module slot (1.7.10 greyed out the others). */
    public boolean supportsSlot(int moduleSlot) {
        return getWeapon().getItem() instanceof EnergyWeaponItem weapon && weapon.supportsSlot(moduleSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = firstModuleSlot + WeaponModule.SLOTS;
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, playerStart, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** The weapon's module slots seen as a container; every change rewrites the weapon in the station. */
    private class Modules implements Container {
        @Override
        public int getContainerSize() {
            return WeaponModule.SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return EnergyWeaponItem.getModules(getWeapon()).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            ItemStack weapon = getWeapon();
            return weapon.isEmpty() ? ItemStack.EMPTY : EnergyWeaponItem.getModule(weapon, slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack module = getItem(slot);
            if (module.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            setItem(slot, ItemStack.EMPTY);
            return module;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, 1);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            ItemStack weapon = getWeapon().copy();
            if (weapon.isEmpty()) return;
            EnergyWeaponItem.setModule(weapon, slot, stack);
            machine.getInventory().setStack(WeaponStationBlockEntity.WEAPON, weapon);
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

    public class ModuleSlot extends Slot {
        public final int moduleSlot;

        ModuleSlot(Container container, int moduleSlot, int x, int y) {
            super(container, moduleSlot, x, y);
            this.moduleSlot = moduleSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getWeapon().getItem() instanceof EnergyWeaponItem weapon && weapon.canInstall(moduleSlot, stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !getWeapon().isEmpty();
        }

        @Override
        public boolean isActive() {
            return page == Page.HOME;
        }
    }
}
