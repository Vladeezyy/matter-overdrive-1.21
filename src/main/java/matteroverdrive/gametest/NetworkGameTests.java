package matteroverdrive.gametest;

import java.util.List;

import matteroverdrive.block.entity.AnalyzerBlockEntity;
import matteroverdrive.block.entity.PatternMonitorBlockEntity;
import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity.Task;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.matter.ItemPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Phase 3 step 4 checks: the matter network from analysis to replication. */
final class NetworkGameTests {
    static void addAll() {
        MOGameTests.add("analyzer_stores_on_network", 200, false, NetworkGameTests::analyzerStoresOnNetwork);
        MOGameTests.add("monitor_request_replicates", 400, false, NetworkGameTests::monitorRequestReplicates);
        MOGameTests.add("unconnected_replicator_idle", 60, false, NetworkGameTests::unconnectedReplicatorIdle);
        MOGameTests.add("replicator_fail_chance", 20, false, NetworkGameTests::replicatorFailChance);
        MOGameTests.add("replicator_irradiates", 200, false, NetworkGameTests::replicatorIrradiates);
    }

    private static void upgrades(MachineBlockEntity m, Item upgrade, int count) {
        for (int i = 0, placed = 0; i < m.getInventory().size() && placed < count; i++) {
            if (m.getInventory().spec(i).role() == MachineInventory.Role.UPGRADE) {
                m.getInventory().setStack(i, new ItemStack(upgrade));
                placed++;
            }
        }
    }

    private static PatternStorageBlockEntity storage(GameTestHelper helper, BlockPos pos, Item patternItem, int progress) {
        helper.setBlock(pos, MOBlocks.PATTERN_STORAGE.get());
        PatternStorageBlockEntity s = helper.getBlockEntity(pos, PatternStorageBlockEntity.class);
        s.getEnergy().set(1000);
        ItemStack drive = new ItemStack(MOItems.PATTERN_DRIVE.get());
        if (patternItem != null) MOItems.PATTERN_DRIVE.get().addProgress(drive, patternItem, progress);
        s.getInventory().setStack(0, drive);
        return s;
    }

    private static void pipes(GameTestHelper helper, int y, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) helper.setBlock(new BlockPos(x, y, 1), MOBlocks.NETWORK_PIPE.get());
    }

    private static void analyzerStoresOnNetwork(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), MOBlocks.ANALYZER.get());
        pipes(helper, 1, 2, 3);
        PatternStorageBlockEntity s = storage(helper, new BlockPos(4, 1, 1), null, 0);
        AnalyzerBlockEntity a = helper.getBlockEntity(new BlockPos(1, 1, 1), AnalyzerBlockEntity.class);
        upgrades(a, MOItems.UPGRADE_HYPER_SPEED.get(), 2);   // 80 ticks per item
        a.getEnergy().set(a.getEnergy().getCapacity());
        a.getInventory().setStack(AnalyzerBlockEntity.INPUT, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(140, () -> {
            List<ItemPattern> patterns = s.getPatterns();
            helper.assertTrue(patterns.size() == 1 && patterns.get(0).is(Items.IRON_INGOT) && patterns.get(0).progress() == 20,
                    Component.literal("storage patterns: " + patterns));
            helper.succeed();
        });
    }

    /** storage - pipe - monitor - pipe - replicator on one line. */
    private static void monitorRequestReplicates(GameTestHelper helper) {
        storage(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 100);
        pipes(helper, 1, 2, 2);
        helper.setBlock(new BlockPos(3, 1, 1), MOBlocks.PATTERN_MONITOR.get());
        pipes(helper, 1, 4, 4);
        helper.setBlock(new BlockPos(5, 1, 1), MOBlocks.REPLICATOR.get());
        PatternMonitorBlockEntity monitor = helper.getBlockEntity(new BlockPos(3, 1, 1), PatternMonitorBlockEntity.class);
        ReplicatorBlockEntity r = helper.getBlockEntity(new BlockPos(5, 1, 1), ReplicatorBlockEntity.class);
        r.getEnergy().set(r.getEnergy().getCapacity());
        r.getMatterTank().setMatter(10);
        upgrades(r, MOItems.UPGRADE_FAILSAFE.get(), 2);   // 0.5% -> ~0.13% failure per item
        List<ItemPattern> seen = monitor.networkPatterns();
        helper.assertTrue(seen.size() == 1 && seen.get(0).is(Items.COBBLESTONE), Component.literal("monitor sees " + seen));
        monitor.request(List.of(new Task(seen.get(0), 3)));
        // cobblestone (1 matter): ~60 ticks per item, x1.56 for the fail-safes; dispatch within 20 ticks
        helper.runAfterDelay(350, () -> {
            ItemStack out = r.getInventory().getStack(ReplicatorBlockEntity.OUTPUT);
            helper.assertTrue(out.is(Items.COBBLESTONE) && out.getCount() == 3, Component.literal("replicated " + out));
            helper.assertTrue(r.getMatterTank().getMatter() == 7, Component.literal("matter left " + r.getMatterTank().getMatter()));
            helper.assertTrue(r.isIdle() && monitor.getQueue().isEmpty(), Component.literal("task not finished"));
            helper.succeed();
        });
    }

    private static void unconnectedReplicatorIdle(GameTestHelper helper) {
        storage(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 100);
        pipes(helper, 1, 2, 2);
        helper.setBlock(new BlockPos(3, 1, 1), MOBlocks.PATTERN_MONITOR.get());
        helper.setBlock(new BlockPos(5, 1, 1), MOBlocks.REPLICATOR.get());     // gap at x=4
        PatternMonitorBlockEntity monitor = helper.getBlockEntity(new BlockPos(3, 1, 1), PatternMonitorBlockEntity.class);
        monitor.request(List.of(new Task(monitor.networkPatterns().get(0), 1)));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(helper.getBlockEntity(new BlockPos(5, 1, 1), ReplicatorBlockEntity.class).isIdle(),
                    Component.literal("task reached an unconnected replicator"));
            helper.assertTrue(monitor.getQueue().size() == 1, Component.literal("request lost"));
            helper.succeed();
        });
    }

    private static void replicatorFailChance(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), MOBlocks.REPLICATOR.get());
        ReplicatorBlockEntity r = helper.getBlockEntity(new BlockPos(1, 1, 1), ReplicatorBlockEntity.class);
        var item = Items.DIAMOND.builtInRegistryHolder();
        // 1.7.10: 0.005 * FAIL + (1 - progress) * 0.5 * (1 + FAIL)
        near(helper, r.getFailChance(new ItemPattern(item, 100)), 0.005, "complete pattern");
        near(helper, r.getFailChance(new ItemPattern(item, 20)), 0.005 + 0.8, "20% pattern");
        helper.succeed();
    }

    private static void replicatorIrradiates(GameTestHelper helper) {
        storage(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 100);
        helper.setBlock(new BlockPos(2, 1, 1), MOBlocks.REPLICATOR.get());
        ReplicatorBlockEntity r = helper.getBlockEntity(new BlockPos(2, 1, 1), ReplicatorBlockEntity.class);
        r.getEnergy().set(r.getEnergy().getCapacity());
        r.getMatterTank().setMatter(50);
        r.setTask(new Task(new ItemPattern(Items.COBBLESTONE.builtInRegistryHolder(), 100), 10));
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 1));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.POISON), Component.literal("unshielded replicator didn't irradiate"));
            helper.succeed();
        });
    }

    private static void near(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-9, Component.literal(what + ": " + actual + " != " + expected));
    }

    private NetworkGameTests() {}
}
