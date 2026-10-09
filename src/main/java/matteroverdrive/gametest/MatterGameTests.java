package matteroverdrive.gametest;

import matteroverdrive.block.entity.AnalyzerBlockEntity;
import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.block.entity.MatterPipeBlockEntity;
import matteroverdrive.block.entity.RecyclerBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.MatterDustItem;
import matteroverdrive.item.PatternDriveItem;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Phase 3 checks: matter values, decomposer, recycler, pipes. */
final class MatterGameTests {
    static void addAll() {
        MOGameTests.add("matter_values", 20, false, MatterGameTests::matterValues);
        MOGameTests.add("matter_values_modern", 20, false, MatterGameTests::matterValuesModern);
        MOGameTests.add("decomposer_makes_matter", 400, false, MatterGameTests::decomposerMakesMatter);
        MOGameTests.add("decomposer_rejects_no_matter", 20, false, MatterGameTests::decomposerRejects);
        MOGameTests.add("recycler_refines_dust", 200, false, MatterGameTests::recyclerRefines);
        MOGameTests.add("pipes_carry_matter", 200, false, MatterGameTests::pipesCarryMatter);
        MOGameTests.add("analyzer_builds_pattern", 300, false, MatterGameTests::analyzerBuildsPattern);
        MOGameTests.add("pattern_drive_limits", 20, false, MatterGameTests::patternDriveLimits);
    }

    private static void matterValues(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        // base values (data map, 1.7.10 registerBasic*)
        expect(helper, server, Items.IRON_INGOT, 32);
        expect(helper, server, Items.DIAMOND, 256);
        expect(helper, server, Items.OAK_LOG, 16);
        expect(helper, server, Items.STICK, 1);
        expect(helper, server, Items.IRON_ORE, 64);
        expect(helper, server, Items.DEEPSLATE_IRON_ORE, 64);
        // crafting: sum of ingredients / output count
        expect(helper, server, Items.IRON_BLOCK, 9 * 32);
        expect(helper, server, Items.BUCKET, 3 * 32);
        expect(helper, server, Items.CRAFTING_TABLE, 4 * 4);
        expect(helper, server, Items.IRON_NUGGET, Math.round(32 / 9f));
        // returned containers are subtracted: the cake's 3 milk buckets give their buckets back
        int cake = MatterRegistry.get(server, Items.CAKE);
        helper.assertTrue(cake == 3 * 12 + 2 * 1 + 3 * 1 + 1, "cake = " + cake);
        // smelting: the result takes its input's matter
        expect(helper, server, Items.COOKED_BEEF, 2);
        // no base value and no recipe
        expect(helper, server, Items.DRAGON_EGG, 0);
        helper.succeed();
    }

    /** Beyond 1.7.10: other recipe types, block conversions, modern base values, estimates and the blacklist. */
    private static void matterValuesModern(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var estimated = MatterRegistry.estimated(server);
        // weathering + waxing (block conversions, then the waxing recipe)
        int copper = MatterRegistry.get(server, Items.CHISELED_COPPER);
        helper.assertTrue(copper > 0 && MatterRegistry.get(server, Items.WAXED_OXIDIZED_CHISELED_COPPER) > 0
                && !estimated.contains(Items.WAXED_OXIDIZED_CHISELED_COPPER), "waxed oxidized chiseled copper");
        // (the stonecutter makes cut copper 4 per block, cheaper than the crafting table: either derivation may win)
        helper.assertTrue(MatterRegistry.get(server, Items.OXIDIZED_CHISELED_COPPER) > 0 && !estimated.contains(Items.OXIDIZED_CHISELED_COPPER),
                "oxidized chiseled copper");
        // smithing: netherite from ancient debris; stonecutting: polished tuff; concrete from its powder; infested stone
        helper.assertTrue(MatterRegistry.get(server, Items.NETHERITE_INGOT) > 4 * 512 && !estimated.contains(Items.NETHERITE_SWORD)
                && MatterRegistry.get(server, Items.NETHERITE_SWORD) > MatterRegistry.get(server, Items.DIAMOND_SWORD),
                "netherite " + MatterRegistry.get(server, Items.NETHERITE_INGOT));
        helper.assertTrue(MatterRegistry.get(server, Items.POLISHED_TUFF) > 0 && !estimated.contains(Items.POLISHED_TUFF), "polished tuff");
        expect(helper, server, Items.WHITE_CONCRETE, MatterRegistry.get(server, Items.WHITE_CONCRETE_POWDER));
        expect(helper, server, Items.INFESTED_STONE, MatterRegistry.get(server, Items.STONE));
        expect(helper, server, Items.STRIPPED_OAK_LOG, 16);
        // never estimated: the blacklist tag, unbreakable blocks, spawn eggs (1.7.10 itself gave bedrock 1024)
        for (var item : new net.minecraft.world.item.Item[] {Items.BARRIER, Items.COMMAND_BLOCK, Items.SPAWNER, Items.DRAGON_EGG, Items.PIG_SPAWN_EGG}) {
            expect(helper, server, item, 0);
        }
        // an item no recipe or base value covers gets a "~" estimate
        helper.assertTrue(estimated.contains(Items.SKULL_POTTERY_SHERD) && MatterRegistry.get(server, Items.SKULL_POTTERY_SHERD) > 0,
                "pottery sherd estimate");
        helper.succeed();
    }

    private static final BlockPos A = new BlockPos(1, 1, 1);

    /** Two fail-safe upgrades push the 0.5% failure chance down to ~0.03% so the test is deterministic in practice. */
    private static DecomposerBlockEntity decomposer(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, MOBlocks.DECOMPOSER.get());
        DecomposerBlockEntity d = helper.<DecomposerBlockEntity>getBlockEntity(pos);
        d.getEnergy().set(d.getEnergy().getCapacity());
        int placed = 0;
        for (int i = 0; i < d.getInventory().size() && placed < 2; i++) {
            if (d.getInventory().spec(i).role() == MachineInventory.Role.UPGRADE) {
                d.getInventory().setStack(i, new ItemStack(MOItems.UPGRADE_FAILSAFE.get()));
                placed++;
            }
        }
        return d;
    }

    private static void decomposerMakesMatter(GameTestHelper helper) {
        DecomposerBlockEntity d = decomposer(helper, A);
        // cobblestone holds 1 matter: 80 * ln(2)^2 = 38 ticks, x1.5625 for two fail-safes
        d.getInventory().setStack(DecomposerBlockEntity.INPUT, new ItemStack(Items.COBBLESTONE, 3));
        d.getInventory().setStack(DecomposerBlockEntity.OUTPUT, ItemStack.EMPTY);
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(d.getInventory().getStack(DecomposerBlockEntity.INPUT).isEmpty(), "input not consumed");
            int matter = d.getMatterTank().getMatter();
            int dust = d.getInventory().getStack(DecomposerBlockEntity.OUTPUT).getCount();
            helper.assertTrue(matter + dust == 3, "matter " + matter + " + failed " + dust + " != 3");
            helper.assertTrue(d.getEnergy().getEnergy() < d.getEnergy().getCapacity(), "no energy used");
            // refined dust decomposes into exactly its own matter (1 here: 60 ticks)
            d.getInventory().setStack(DecomposerBlockEntity.INPUT, MatterDustItem.withMatter(MOItems.MATTER_DUST_REFINED.get(), 1));
            helper.runAfterDelay(90, () -> {
                int gained = d.getMatterTank().getMatter() - matter;
                helper.assertTrue(gained == 1, "refined dust gave " + gained);
                helper.succeed();
            });
        });
    }

    private static void decomposerRejects(GameTestHelper helper) {
        DecomposerBlockEntity d = decomposer(helper, A);
        var inv = d.getInventory();
        helper.assertFalse(inv.isValid(DecomposerBlockEntity.INPUT, new ItemStack(Items.DRAGON_EGG)), "accepts dragon egg");
        helper.assertFalse(inv.isValid(DecomposerBlockEntity.INPUT, new ItemStack(MOItems.MATTER_DUST.get())), "accepts unrefined dust");
        helper.assertTrue(inv.isValid(DecomposerBlockEntity.INPUT, new ItemStack(Items.DIAMOND)), "rejects diamond");
        helper.succeed();
    }

    private static void recyclerRefines(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.RECYCLER.get());
        RecyclerBlockEntity r = helper.<RecyclerBlockEntity>getBlockEntity(A);
        r.getEnergy().set(r.getEnergy().getCapacity());
        // dust with 1 matter: 80 * ln(2)^2 = 38 ticks each
        r.getInventory().setStack(RecyclerBlockEntity.INPUT, MatterDustItem.withMatter(MOItems.MATTER_DUST.get(), 1).copyWithCount(2));
        helper.runAfterDelay(100, () -> {
            ItemStack out = r.getInventory().getStack(RecyclerBlockEntity.OUTPUT);
            helper.assertTrue(out.is(MOItems.MATTER_DUST_REFINED.get()) && out.getCount() == 2 && MatterDustItem.getMatter(out) == 1,
                    "recycler output " + out + " matter " + MatterDustItem.getMatter(out));
            helper.succeed();
        });
    }

    private static void pipesCarryMatter(GameTestHelper helper) {
        DecomposerBlockEntity d = decomposer(helper, A);
        d.getMatterTank().setMatter(100);
        for (int x = 2; x <= 5; x++) helper.setBlock(new BlockPos(x, 1, 1), MOBlocks.MATTER_PIPE.get());
        helper.runAfterDelay(150, () -> {
            int inPipes = 0;
            for (int x = 2; x <= 5; x++) {
                inPipes += helper.<MatterPipeBlockEntity>getBlockEntity(new BlockPos(x, 1, 1)).getTank().getMatter();
            }
            int last = helper.<MatterPipeBlockEntity>getBlockEntity(new BlockPos(5, 1, 1)).getTank().getMatter();
            helper.assertTrue(last > 0, "matter never reached the last pipe");
            helper.assertTrue(inPipes + d.getMatterTank().getMatter() == 100,
                    "matter not conserved: pipes " + inPipes + " decomposer " + d.getMatterTank().getMatter());
            helper.succeed();
        });
    }

    private static void analyzerBuildsPattern(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.ANALYZER.get());
        AnalyzerBlockEntity a = helper.<AnalyzerBlockEntity>getBlockEntity(A);
        int placed = 0;
        for (int i = 0; i < a.getInventory().size() && placed < 2; i++) {
            if (a.getInventory().spec(i).role() == MachineInventory.Role.UPGRADE) {
                a.getInventory().setStack(i, new ItemStack(MOItems.UPGRADE_HYPER_SPEED.get()));
                placed++;
            }
        }
        // two hyper speed upgrades: speed hits the 0.1 floor (80 ticks), power x4 (256000 FE per item)
        a.getEnergy().set(a.getEnergy().getCapacity());
        a.getInventory().setStack(AnalyzerBlockEntity.INPUT, new ItemStack(Items.IRON_INGOT, 2));
        a.getInventory().setStack(AnalyzerBlockEntity.DATABASE, new ItemStack(MOItems.PATTERN_DRIVE.get()));
        helper.runAfterDelay(200, () -> {
            var patterns = PatternDriveItem.getPatterns(a.getInventory().getStack(AnalyzerBlockEntity.DATABASE));
            helper.assertTrue(patterns.size() == 1 && patterns.get(0).is(Items.IRON_INGOT) && patterns.get(0).progress() == 40,
                    "patterns after two ingots: " + patterns);
            helper.assertTrue(a.getInventory().getStack(AnalyzerBlockEntity.INPUT).isEmpty(), "ingots not consumed");
            helper.succeed();
        });
    }

    private static void patternDriveLimits(GameTestHelper helper) {
        PatternDriveItem item = MOItems.PATTERN_DRIVE.get();
        ItemStack drive = new ItemStack(item);
        for (int i = 0; i < 5; i++) item.addProgress(drive, Items.DIAMOND, AnalyzerBlockEntity.PROGRESS_PER_ITEM);
        helper.assertTrue(PatternDriveItem.getPatterns(drive).get(0).isComplete(), "5 analyses don't complete a pattern");
        helper.assertFalse(item.canAccept(drive, Items.DIAMOND), "complete pattern still accepts");
        helper.assertTrue(item.addProgress(drive, Items.GOLD_INGOT, 20), "second pattern rejected");
        helper.assertFalse(item.canAccept(drive, Items.EMERALD), "drive holds more than 2 patterns");
        helper.assertTrue(item.canAccept(drive, Items.GOLD_INGOT), "incomplete pattern refuses progress");
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, net.minecraft.server.MinecraftServer server, Item item, int value) {
        int actual = MatterRegistry.get(server, item);
        helper.assertTrue(actual == value, item + " has " + actual + " matter, expected " + value);
    }

    private MatterGameTests() {}
}
