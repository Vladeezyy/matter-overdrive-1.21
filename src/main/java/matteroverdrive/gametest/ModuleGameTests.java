package matteroverdrive.gametest;

import matteroverdrive.block.entity.WeaponStationBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.WeaponModule;
import matteroverdrive.item.weapon.WeaponStat;
import matteroverdrive.menu.WeaponStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Phase 5c checks: barrel and scope modules, the battery module, the weapon station. */
final class ModuleGameTests {
    static void addAll() {
        MOGameTests.add("barrel_damage_stats", 20, false, ModuleGameTests::damageBarrel);
        MOGameTests.add("module_compatibility", 20, false, ModuleGameTests::compatibility);
        MOGameTests.add("battery_module_powers_weapon", 20, false, ModuleGameTests::batteryModule);
        MOGameTests.add("sniper_scope_stats", 20, false, ModuleGameTests::scope);
        MOGameTests.add("weapon_station_installs_modules", 20, false, ModuleGameTests::station);
        MOGameTests.add("omni_tool", 20, false, ModuleGameTests::omniTool);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(new BlockPos(6, 1, 1)).getBottomCenter();
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    /** 1.7.10 OmniTool: digs the block it points at from afar, fires bolts, takes no sights or explosion/heal barrels. */
    private static void omniTool(GameTestHelper helper) {
        var omni = MOItems.OMNI_TOOL.get();
        check(helper, !omni.canInstall(WeaponModule.SLOT_SIGHTS, new ItemStack(MOItems.SNIPER_SCOPE.get()))
                && !omni.canInstall(WeaponModule.SLOT_BARREL, new ItemStack(MOItems.BARREL_EXPLOSION.get()))
                && omni.canInstall(WeaponModule.SLOT_BARREL, new ItemStack(MOItems.BARREL_FIRE.get())), "module rules");
        ItemStack tool = new ItemStack(omni);
        EnergyWeaponItem.setEnergy(tool, EnergyWeaponItem.CAPACITY);
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, tool);
        BlockPos stone = new BlockPos(6, 2, 7);
        helper.setBlock(stone, net.minecraft.world.level.block.Blocks.STONE);
        for (int i = 0; i < 200 && !helper.getLevel().getBlockState(helper.absolutePos(stone)).isAir(); i++) {
            omni.onUseTick(helper.getLevel(), player, tool, 200 - i);
        }
        check(helper, helper.getLevel().getBlockState(helper.absolutePos(stone)).isAir(), "stone 6 blocks away wasn't dug");
        omni.tryFire(player, tool, false);
        var bolts = helper.getLevel().getEntitiesOfClass(matteroverdrive.entity.PlasmaBolt.class, player.getBoundingBox().inflate(8));
        // 1.7.10 drained energy use x cooldown per shot: 512 / 18 = 28 FE/t, x 18 = 504
        check(helper, bolts.size() == 1 && EnergyWeaponItem.getEnergy(tool) == EnergyWeaponItem.CAPACITY - 504, "bolts " + bolts.size()
                + ", energy " + EnergyWeaponItem.getEnergy(tool));
        helper.succeed();
    }

    /** 1.7.10 damage barrel: damage x1.5, energy x0.5. */
    private static void damageBarrel(GameTestHelper helper) {
        ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
        EnergyWeaponItem.setEnergy(rifle, EnergyWeaponItem.CAPACITY);
        EnergyWeaponItem.setModule(rifle, WeaponModule.SLOT_BARREL, new ItemStack(MOItems.BARREL_DAMAGE.get()));
        check(helper, EnergyWeaponItem.modifyStat(WeaponStat.DAMAGE, rifle, 8) == 12, "damage " + EnergyWeaponItem.modifyStat(WeaponStat.DAMAGE, rifle, 8));
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, rifle);
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, false);
        // 1024 / 11 = 93 FE/t, x0.5 = 46, x 11 ticks
        check(helper, EnergyWeaponItem.getEnergy(rifle) == 32000 - 46 * 11, "energy " + EnergyWeaponItem.getEnergy(rifle));
        helper.succeed();
    }

    /** 1.7.10 supportsModule: explosion/heal barrels and no sights on the phaser; the shotgun takes no scope. */
    private static void compatibility(GameTestHelper helper) {
        var phaser = MOItems.PHASER.get();
        var rifle = MOItems.PHASER_RIFLE.get();
        var shotgun = MOItems.PLASMA_SHOTGUN.get();
        ItemStack scope = new ItemStack(MOItems.SNIPER_SCOPE.get());
        ItemStack explosion = new ItemStack(MOItems.BARREL_EXPLOSION.get());
        check(helper, phaser.canInstall(WeaponModule.SLOT_BARREL, explosion), "phaser rejects explosion barrel");
        check(helper, !rifle.canInstall(WeaponModule.SLOT_BARREL, explosion), "rifle takes explosion barrel");
        check(helper, !phaser.canInstall(WeaponModule.SLOT_SIGHTS, scope), "phaser takes a scope");
        check(helper, rifle.canInstall(WeaponModule.SLOT_SIGHTS, scope), "rifle rejects the scope");
        check(helper, !shotgun.canInstall(WeaponModule.SLOT_SIGHTS, scope), "shotgun takes a scope");
        check(helper, !rifle.canInstall(WeaponModule.SLOT_BARREL, scope), "scope fits the barrel slot");
        check(helper, rifle.canInstall(WeaponModule.SLOT_BATTERY, new ItemStack(MOItems.BATTERY.get())), "battery slot rejects a battery");
        check(helper, !rifle.canInstall(WeaponModule.SLOT_BATTERY, new ItemStack(Items.REDSTONE)), "battery slot takes redstone");
        helper.succeed();
    }

    /** 1.7.10 EnergyWeapon: with a battery module the weapon stores and spends the battery's energy. */
    private static void batteryModule(GameTestHelper helper) {
        ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
        EnergyWeaponItem.setModule(rifle, WeaponModule.SLOT_BATTERY, new ItemStack(MOItems.HC_BATTERY.get()));
        check(helper, EnergyWeaponItem.getCapacity(rifle) == 1 << 20, "capacity " + EnergyWeaponItem.getCapacity(rifle));
        var handler = rifle.getCapability(Capabilities.EnergyStorage.ITEM);
        int inserted = handler.receiveEnergy(100, false);
        check(helper, inserted == 100, "inserted " + inserted);
        ItemStack battery = EnergyWeaponItem.getModule(rifle, WeaponModule.SLOT_BATTERY);
        check(helper, MOItems.HC_BATTERY.get().getEnergy(battery) == 100, "battery holds " + MOItems.HC_BATTERY.get().getEnergy(battery));
        check(helper, !rifle.has(matteroverdrive.init.MODataComponents.ENERGY.get()), "weapon stored the energy itself");
        EnergyWeaponItem.setEnergy(rifle, 5000);
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, rifle);
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, false);
        check(helper, EnergyWeaponItem.getEnergy(rifle) == 5000 - 93 * 11, "energy " + EnergyWeaponItem.getEnergy(rifle));
        helper.succeed();
    }

    /** 1.7.10 sniper scope: zoom 0.85, range x1.5, accuracy x0.8 then x0.4 when zoomed. */
    private static void scope(GameTestHelper helper) {
        ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
        var item = MOItems.PHASER_RIFLE.get();
        int range = item.getRange(rifle);
        ServerPlayer player = player(helper);
        float accuracy = item.getAccuracy(rifle, player, true);
        EnergyWeaponItem.setModule(rifle, WeaponModule.SLOT_SIGHTS, new ItemStack(MOItems.SNIPER_SCOPE.get()));
        check(helper, item.getRange(rifle) == Math.round(range * 1.5f), "range " + item.getRange(rifle));
        check(helper, item.getZoom(rifle) == 0.85f, "zoom " + item.getZoom(rifle));
        float scoped = item.getAccuracy(rifle, player, true);
        check(helper, Math.abs(scoped - accuracy * 0.8f * 0.4f) < 1e-5, "accuracy " + scoped + " vs " + accuracy);
        helper.succeed();
    }

    /** The station's module slots edit the weapon in it; shift-click puts a module in its slot. */
    private static void station(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, MOBlocks.WEAPON_STATION.get());
        var station = (WeaponStationBlockEntity) helper.getBlockEntity(pos, WeaponStationBlockEntity.class);
        station.getInventory().setStack(WeaponStationBlockEntity.WEAPON, new ItemStack(MOItems.PHASER_RIFLE.get()));
        ServerPlayer player = player(helper);
        var menu = new WeaponStationMenu(1, player.getInventory(), station, new net.minecraft.world.inventory.SimpleContainerData(11));
        player.getInventory().setItem(0, new ItemStack(MOItems.BARREL_FIRE.get()));
        int hotbarSlot = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (menu.slots.get(i).container == player.getInventory() && menu.slots.get(i).getContainerSlot() == 0) hotbarSlot = i;
        }
        menu.quickMoveStack(player, hotbarSlot);
        ItemStack weapon = station.getWeapon();
        check(helper, EnergyWeaponItem.getModule(weapon, WeaponModule.SLOT_BARREL).is(MOItems.BARREL_FIRE.get()),
                "barrel slot holds " + EnergyWeaponItem.getModule(weapon, WeaponModule.SLOT_BARREL));
        check(helper, player.getInventory().getItem(0).isEmpty(), "module still in the hotbar");
        // explosion barrels don't fit the rifle
        player.getInventory().setItem(0, new ItemStack(MOItems.BARREL_EXPLOSION.get()));
        menu.quickMoveStack(player, hotbarSlot);
        check(helper, !player.getInventory().getItem(0).isEmpty(), "rifle took an explosion barrel");
        helper.succeed();
    }
}
