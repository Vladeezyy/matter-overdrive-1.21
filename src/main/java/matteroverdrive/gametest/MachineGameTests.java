package matteroverdrive.gametest;

import java.util.List;

import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.block.entity.SolarPanelBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOItems;
import matteroverdrive.machine.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Phase 2 checks: machines, energy, upgrades, the inscriber recipe. */
final class MachineGameTests {
    private static final BlockPos A = new BlockPos(1, 1, 1);
    private static final BlockPos B = new BlockPos(2, 1, 1);

    static void addAll() {
        MOGameTests.add("solar_panel_generates_at_noon", 60, true, 6000, MachineGameTests::solarGeneratesAtNoon);
        MOGameTests.add("solar_panel_idle_at_night", 60, true, 18000, MachineGameTests::solarIdleAtNight);
        MOGameTests.add("solar_panel_powers_neighbour", 40, false, MachineGameTests::solarPowersNeighbour);
        MOGameTests.add("inscriber_makes_mk2", 400, false, MachineGameTests::inscriberMakesMk2);
        MOGameTests.add("inscriber_needs_energy", 40, false, MachineGameTests::inscriberNeedsEnergy);
        MOGameTests.add("battery_charges_machine", 20, false, MachineGameTests::batteryChargesMachine);
        MOGameTests.add("upgrade_multipliers", 20, false, MachineGameTests::upgradeMultipliers);
        MOGameTests.add("machine_drop_keeps_energy", 20, false, MachineGameTests::dropKeepsEnergy);
        MOGameTests.add("machine_item_keeps_matter_and_owner", 20, false, MachineGameTests::itemKeepsMatterAndOwner);
    }

    private static void solarGeneratesAtNoon(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.SOLAR_PANEL.get());
        helper.runAfterDelay(20, () -> {
            SolarPanelBlockEntity panel = helper.getBlockEntity(A, SolarPanelBlockEntity.class);
            // 1.7.10: round(16 * cos(0)) = 16 FE/t at noon
            helper.assertTrue(panel.getChargeAmount() == SolarPanelBlockEntity.CHARGE_AMOUNT,
                    Component.literal("charge at noon was " + panel.getChargeAmount()));
            helper.assertTrue(panel.getEnergy().getEnergy() > 0, Component.literal("no energy after 20 ticks of noon"));
            helper.assertTrue(panel.isActive(), Component.literal("panel not active at noon"));
            helper.succeed();
        });
    }

    private static void solarIdleAtNight(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.SOLAR_PANEL.get());
        helper.runAfterDelay(20, () -> {
            SolarPanelBlockEntity panel = helper.getBlockEntity(A, SolarPanelBlockEntity.class);
            helper.assertTrue(panel.getEnergy().getEnergy() == 0, Component.literal("panel generated at midnight"));
            helper.succeed();
        });
    }

    private static void solarPowersNeighbour(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.SOLAR_PANEL.get());
        helper.setBlock(B, MOBlocks.INSCRIBER.get());
        helper.getBlockEntity(A, SolarPanelBlockEntity.class).getEnergy().set(10000);
        helper.runAfterDelay(5, () -> {
            int received = helper.getBlockEntity(B, InscriberBlockEntity.class).getEnergy().getEnergy();
            helper.assertTrue(received > 0, Component.literal("inscriber received no energy from the panel"));
            helper.succeed();
        });
    }

    private static void inscriberMakesMk2(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.INSCRIBER.get());
        InscriberBlockEntity inscriber = helper.getBlockEntity(A, InscriberBlockEntity.class);
        inscriber.getEnergy().set(inscriber.getEnergy().getCapacity());
        inscriber.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 2));
        inscriber.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 2));
        // 1.7.10 recipe: 64000 FE over 300 ticks.
        helper.runAfterDelay(320, () -> {
            ItemStack out = inscriber.getInventory().getStack(InscriberBlockEntity.OUTPUT);
            helper.assertTrue(out.is(MOItems.ISOLINEAR_CIRCUIT_MK2.get()) && out.getCount() == 1,
                    Component.literal("output after 320 ticks: " + out));
            helper.assertTrue(inscriber.getInventory().getStack(InscriberBlockEntity.MAIN).getCount() == 1,
                    Component.literal("main input not consumed once"));
            int used = inscriber.getEnergy().getCapacity() - inscriber.getEnergy().getEnergy();
            // 64000 / 300 = 213 FE/t (integer, as in 1.7.10); the second craft has started by now
            helper.assertTrue(used >= 213 * 300 && used <= 213 * 321, Component.literal("energy used: " + used));
            helper.succeed();
        });
    }

    private static void inscriberNeedsEnergy(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.INSCRIBER.get());
        InscriberBlockEntity inscriber = helper.getBlockEntity(A, InscriberBlockEntity.class);
        inscriber.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get()));
        inscriber.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT));
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(inscriber.isActive(), Component.literal("inscriber active without energy"));
            helper.assertTrue(inscriber.getProgress() == 0, Component.literal("progress without energy"));
            helper.succeed();
        });
    }

    private static void batteryChargesMachine(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.INSCRIBER.get());
        InscriberBlockEntity inscriber = helper.getBlockEntity(A, InscriberBlockEntity.class);
        ItemStack battery = MOItems.BATTERY.get().charged();
        inscriber.getInventory().setStack(inscriber.getBatterySlot(), battery);
        helper.runAfterDelay(10, () -> {
            int energy = inscriber.getEnergy().getEnergy();
            // inscriber accepts 256 FE/t; the battery gives up to 800 FE/t
            helper.assertTrue(energy >= 256 * 9 && energy <= 256 * 11, Component.literal("energy after 10 ticks: " + energy));
            int left = inscriber.getInventory().getStack(inscriber.getBatterySlot()).getOrDefault(MODataComponents.ENERGY.get(), 0);
            helper.assertTrue(left == (1 << 19) - energy, Component.literal("battery holds " + left + ", machine " + energy));
            helper.succeed();
        });
    }

    private static void upgradeMultipliers(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.INSCRIBER.get());
        InscriberBlockEntity inscriber = helper.getBlockEntity(A, InscriberBlockEntity.class);
        List<Integer> upgradeSlots = new java.util.ArrayList<>();
        for (int i = 0; i < inscriber.getInventory().size(); i++) {
            if (inscriber.getInventory().spec(i).role() == matteroverdrive.machine.MachineInventory.Role.UPGRADE) upgradeSlots.add(i);
        }
        helper.assertTrue(upgradeSlots.size() == 4, Component.literal("inscriber has " + upgradeSlots.size() + " upgrade slots"));
        inscriber.getInventory().setStack(upgradeSlots.get(0), new ItemStack(MOItems.UPGRADE_SPEED.get()));
        assertNear(helper, inscriber.getUpgradeMultiplier(UpgradeType.SPEED), 0.75, "speed with one speed upgrade");
        assertNear(helper, inscriber.getUpgradeMultiplier(UpgradeType.POWER_USAGE), 1.25, "power with one speed upgrade");
        assertNear(helper, inscriber.getUpgradeMultiplier(UpgradeType.RANGE), 1, "inscriber ignores range");
        inscriber.getInventory().setStack(upgradeSlots.get(1), new ItemStack(MOItems.UPGRADE_HYPER_SPEED.get()));
        // 0.75 * 0.15 = 0.1125, above the 0.1 speed floor
        assertNear(helper, inscriber.getUpgradeMultiplier(UpgradeType.SPEED), 0.1125, "speed + hyper speed");
        inscriber.getInventory().setStack(upgradeSlots.get(2), new ItemStack(MOItems.UPGRADE_HYPER_SPEED.get()));
        assertNear(helper, inscriber.getUpgradeMultiplier(UpgradeType.SPEED), 0.1, "speed floor");
        inscriber.getInventory().setStack(upgradeSlots.get(3), new ItemStack(MOItems.UPGRADE_POWER_STORAGE.get()));
        helper.assertTrue(inscriber.getEnergy().getCapacity() == 1024000,
                Component.literal("capacity with power storage: " + inscriber.getEnergy().getCapacity()));
        helper.succeed();
    }

    private static void dropKeepsEnergy(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.INSCRIBER.get());
        InscriberBlockEntity inscriber = helper.getBlockEntity(A, InscriberBlockEntity.class);
        inscriber.getEnergy().set(12345);
        BlockPos abs = helper.absolutePos(A);
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(abs), helper.getLevel(), abs, inscriber, null,
                new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(MOItems.INSCRIBER.get()), Component.literal("drops: " + drops));
        Integer energy = drops.get(0).get(MODataComponents.ENERGY.get());
        helper.assertTrue(energy != null && energy == 12345, Component.literal("dropped energy " + energy));
        helper.succeed();
    }

    /** 1.7.10 writeToDropItem / readFromPlaceItem: energy (with its capacity), matter and the owner go with the item. */
    private static void itemKeepsMatterAndOwner(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.REPLICATOR.get());
        var replicator = helper.getBlockEntity(A, matteroverdrive.block.entity.ReplicatorBlockEntity.class);
        replicator.getEnergy().set(5000);
        replicator.getMatterTank().setMatter(321);
        java.util.UUID owner = java.util.UUID.randomUUID();
        ItemStack protocol = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
        protocol.set(MODataComponents.SECURITY_OWNER.get(), owner);
        helper.assertTrue(replicator.claim(protocol), Component.literal("claim"));
        BlockPos abs = helper.absolutePos(A);
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(abs), helper.getLevel(), abs, replicator, null,
                new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1, Component.literal("drops: " + drops));
        ItemStack item = drops.get(0);
        var storage = item.get(MODataComponents.MACHINE_STORAGE.get());
        helper.assertTrue(item.getItem() instanceof matteroverdrive.machine.MachineBlockItem && storage != null
                && storage.maxEnergy() == replicator.getEnergy().getCapacity() && storage.matter() == 321
                && storage.maxMatter() == replicator.getMatterTank().getCapacity() && owner.equals(item.get(MODataComponents.SECURITY_OWNER.get()))
                && matteroverdrive.machine.MachineBlockItem.isConfigured(item) && item.isBarVisible(),
                Component.literal("item " + item.getComponentsPatch()));
        helper.setBlock(B, MOBlocks.REPLICATOR.get());
        var placed = helper.getBlockEntity(B, matteroverdrive.block.entity.ReplicatorBlockEntity.class);
        placed.applyComponentsFromItemStack(item);
        helper.assertTrue(placed.getEnergy().getEnergy() == 5000 && placed.getMatterTank().getMatter() == 321 && owner.equals(placed.getOwner()),
                Component.literal("placed " + placed.getEnergy().getEnergy() + " FE, " + placed.getMatterTank().getMatter() + " kM, " + placed.getOwner()));
        helper.assertFalse(matteroverdrive.machine.MachineBlockItem.isConfigured(new ItemStack(MOItems.REPLICATOR.get())), Component.literal("fresh item configured"));
        helper.succeed();
    }

    private static void assertNear(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-6, Component.literal(what + ": " + actual + " != " + expected));
    }

    private MachineGameTests() {}
}
