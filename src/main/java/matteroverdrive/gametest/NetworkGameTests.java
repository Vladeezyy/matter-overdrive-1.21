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
        MOGameTests.add("matter_scanner", 40, false, NetworkGameTests::matterScanner);
        MOGameTests.add("network_destination_filter", 20, false, NetworkGameTests::destinationFilter);
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
        PatternStorageBlockEntity s = helper.<PatternStorageBlockEntity>getBlockEntity(pos);
        s.getEnergy().set(1000);
        ItemStack drive = new ItemStack(MOItems.PATTERN_DRIVE.get());
        if (patternItem != null) MOItems.PATTERN_DRIVE.get().addProgress(drive, patternItem, progress);
        s.getInventory().setStack(0, drive);
        return s;
    }

    private static void pipes(GameTestHelper helper, int y, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) helper.setBlock(new BlockPos(x, y, 1), MOBlocks.NETWORK_PIPE.get());
    }

    /** A network flash drive marks blocks; in the monitor's filter slot it only sees the marked storage. */
    private static void destinationFilter(GameTestHelper helper) {
        storage(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 100);
        pipes(helper, 1, 2, 2);
        helper.setBlock(new BlockPos(3, 1, 1), MOBlocks.PATTERN_MONITOR.get());
        pipes(helper, 1, 4, 4);
        storage(helper, new BlockPos(5, 1, 1), Items.DIRT, 100);
        PatternMonitorBlockEntity monitor = helper.<PatternMonitorBlockEntity>getBlockEntity(new BlockPos(3, 1, 1));
        helper.assertTrue(monitor.networkPatterns().size() == 2, "unfiltered " + monitor.networkPatterns());
        // use the drive on the dirt storage (marks it), on a pipe twice (marks and unmarks it)
        ItemStack drive = new ItemStack(MOItems.NETWORK_FLASH_DRIVE.get());
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (BlockPos rel : List.of(new BlockPos(5, 1, 1), new BlockPos(4, 1, 1), new BlockPos(4, 1, 1))) {
            BlockPos abs = helper.absolutePos(rel);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, drive);
            drive.getItem().onItemUseFirst(drive, new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false)));
        }
        helper.assertTrue(matteroverdrive.item.NetworkFlashDriveItem.getConnections(drive).equals(List.of(helper.absolutePos(new BlockPos(5, 1, 1)))),
                "drive " + matteroverdrive.item.NetworkFlashDriveItem.getConnections(drive));
        monitor.getInventory().setStack(0, drive);
        List<ItemPattern> seen = monitor.networkPatterns();
        helper.assertTrue(seen.size() == 1 && seen.get(0).is(Items.DIRT), "filtered " + seen);
        helper.succeed();
    }

    private static void analyzerStoresOnNetwork(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), MOBlocks.ANALYZER.get());
        pipes(helper, 1, 2, 3);
        PatternStorageBlockEntity s = storage(helper, new BlockPos(4, 1, 1), null, 0);
        AnalyzerBlockEntity a = helper.<AnalyzerBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        upgrades(a, MOItems.UPGRADE_HYPER_SPEED.get(), 2);   // 80 ticks per item
        a.getEnergy().set(a.getEnergy().getCapacity());
        a.getInventory().setStack(AnalyzerBlockEntity.INPUT, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(140, () -> {
            List<ItemPattern> patterns = s.getPatterns();
            helper.assertTrue(patterns.size() == 1 && patterns.get(0).is(Items.IRON_INGOT) && patterns.get(0).progress() == 20,
                    "storage patterns: " + patterns);
            helper.succeed();
        });
    }

    /** Matter scanner: linked in the storage's scanner slot; scanning a block adds 10% to its pattern and removes it. */
    private static void matterScanner(GameTestHelper helper) {
        PatternStorageBlockEntity s = storage(helper, new BlockPos(1, 1, 1), null, 0);
        s.getInventory().setStack(PatternStorageBlockEntity.SCANNER, new ItemStack(MOItems.MATTER_SCANNER.get()));
        BlockPos target = new BlockPos(4, 1, 4);
        helper.setBlock(target, net.minecraft.world.level.block.Blocks.IRON_BLOCK);
        helper.runAfterDelay(5, () -> {
            ItemStack scanner = s.getInventory().getStack(PatternStorageBlockEntity.SCANNER);
            helper.assertTrue(matteroverdrive.item.MatterScannerItem.getDatabase(helper.getLevel(), scanner) == s,
                    "scanner not linked: " + matteroverdrive.item.MatterScannerItem.getLink(scanner));
            var player = helper.makeMockServerPlayerInLevel();
            boolean ok = matteroverdrive.item.MatterScannerItem.scan(helper.getLevel(), scanner, player, helper.absolutePos(target));
            List<ItemPattern> patterns = s.getPatterns();
            helper.assertTrue(ok && helper.getLevel().getBlockState(helper.absolutePos(target)).isAir()
                    && patterns.size() == 1 && patterns.get(0).is(Items.IRON_BLOCK) && patterns.get(0).progress() == 10,
                    "scan " + ok + ", patterns " + patterns);
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
        PatternMonitorBlockEntity monitor = helper.<PatternMonitorBlockEntity>getBlockEntity(new BlockPos(3, 1, 1));
        ReplicatorBlockEntity r = helper.<ReplicatorBlockEntity>getBlockEntity(new BlockPos(5, 1, 1));
        r.getEnergy().set(r.getEnergy().getCapacity());
        r.getMatterTank().setMatter(10);
        upgrades(r, MOItems.UPGRADE_FAILSAFE.get(), 2);   // 0.5% -> ~0.13% failure per item
        List<ItemPattern> seen = monitor.networkPatterns();
        helper.assertTrue(seen.size() == 1 && seen.get(0).is(Items.COBBLESTONE), "monitor sees " + seen);
        monitor.request(List.of(new Task(seen.get(0), 3)));
        // cobblestone (1 matter): ~60 ticks per item, x1.56 for the fail-safes; dispatch within 20 ticks
        helper.runAfterDelay(350, () -> {
            ItemStack out = r.getInventory().getStack(ReplicatorBlockEntity.OUTPUT);
            helper.assertTrue(out.is(Items.COBBLESTONE) && out.getCount() == 3, "replicated " + out);
            int failed = r.getInventory().getStack(ReplicatorBlockEntity.SECOND_OUTPUT).getCount();   // a rare failure costs matter too
            helper.assertTrue(r.getMatterTank().getMatter() == 7 - failed, "matter left " + r.getMatterTank().getMatter() + ", failed " + failed);
            helper.assertTrue(r.isIdle() && monitor.getQueue().isEmpty(), "task not finished");
            helper.succeed();
        });
    }

    private static void unconnectedReplicatorIdle(GameTestHelper helper) {
        storage(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 100);
        pipes(helper, 1, 2, 2);
        helper.setBlock(new BlockPos(3, 1, 1), MOBlocks.PATTERN_MONITOR.get());
        helper.setBlock(new BlockPos(5, 1, 1), MOBlocks.REPLICATOR.get());     // gap at x=4
        PatternMonitorBlockEntity monitor = helper.<PatternMonitorBlockEntity>getBlockEntity(new BlockPos(3, 1, 1));
        monitor.request(List.of(new Task(monitor.networkPatterns().get(0), 1)));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(helper.<ReplicatorBlockEntity>getBlockEntity(new BlockPos(5, 1, 1)).isIdle(),
                    "task reached an unconnected replicator");
            helper.assertTrue(monitor.getQueue().size() == 1, "request lost");
            helper.succeed();
        });
    }

    private static void replicatorFailChance(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), MOBlocks.REPLICATOR.get());
        ReplicatorBlockEntity r = helper.<ReplicatorBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        var item = Items.DIAMOND.builtInRegistryHolder();
        // 1.7.10: 0.005 * FAIL + (1 - progress) * 0.5 * (1 + FAIL)
        near(helper, r.getFailChance(new ItemPattern(item, 100)), 0.005, "complete pattern");
        near(helper, r.getFailChance(new ItemPattern(item, 20)), 0.005 + 0.8, "20% pattern");
        helper.succeed();
    }

    private static void replicatorIrradiates(GameTestHelper helper) {
        storage(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 100);
        helper.setBlock(new BlockPos(2, 1, 1), MOBlocks.REPLICATOR.get());
        ReplicatorBlockEntity r = helper.<ReplicatorBlockEntity>getBlockEntity(new BlockPos(2, 1, 1));
        r.getEnergy().set(r.getEnergy().getCapacity());
        r.getMatterTank().setMatter(50);
        r.setTask(new Task(new ItemPattern(Items.COBBLESTONE.builtInRegistryHolder(), 100), 10));
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 1));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.POISON), "unshielded replicator didn't irradiate");
            helper.succeed();
        });
    }

    private static void near(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-9, what + ": " + actual + " != " + expected);
    }

    private NetworkGameTests() {}
}
