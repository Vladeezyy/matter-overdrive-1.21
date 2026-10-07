package matteroverdrive.gametest;

import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.block.entity.MatterPipeBlockEntity;
import matteroverdrive.block.entity.RecyclerBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.MatterDustItem;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.matter.MatterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Phase 3 checks: matter values, decomposer, recycler, pipes. */
final class MatterGameTests {
    static void addAll() {
        MOGameTests.add("matter_values", 20, false, MatterGameTests::matterValues);
        MOGameTests.add("decomposer_makes_matter", 400, false, MatterGameTests::decomposerMakesMatter);
        MOGameTests.add("decomposer_rejects_no_matter", 20, false, MatterGameTests::decomposerRejects);
        MOGameTests.add("recycler_refines_dust", 200, false, MatterGameTests::recyclerRefines);
        MOGameTests.add("pipes_carry_matter", 200, false, MatterGameTests::pipesCarryMatter);
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
        helper.assertTrue(cake == 3 * 12 + 2 * 1 + 3 * 1 + 1, Component.literal("cake = " + cake));
        // smelting: the result takes its input's matter
        expect(helper, server, Items.COOKED_BEEF, 2);
        // no base value and no recipe
        expect(helper, server, Items.DRAGON_EGG, 0);
        helper.succeed();
    }

    private static final BlockPos A = new BlockPos(1, 1, 1);

    /** Two fail-safe upgrades push the 0.5% failure chance down to ~0.03% so the test is deterministic in practice. */
    private static DecomposerBlockEntity decomposer(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, MOBlocks.DECOMPOSER.get());
        DecomposerBlockEntity d = helper.getBlockEntity(pos, DecomposerBlockEntity.class);
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
            helper.assertTrue(d.getInventory().getStack(DecomposerBlockEntity.INPUT).isEmpty(), Component.literal("input not consumed"));
            int matter = d.getMatterTank().getMatter();
            int dust = d.getInventory().getStack(DecomposerBlockEntity.OUTPUT).getCount();
            helper.assertTrue(matter + dust == 3, Component.literal("matter " + matter + " + failed " + dust + " != 3"));
            helper.assertTrue(d.getEnergy().getEnergy() < d.getEnergy().getCapacity(), Component.literal("no energy used"));
            // refined dust decomposes into exactly its own matter (1 here: 60 ticks)
            d.getInventory().setStack(DecomposerBlockEntity.INPUT, MatterDustItem.withMatter(MOItems.MATTER_DUST_REFINED.get(), 1));
            helper.runAfterDelay(90, () -> {
                int gained = d.getMatterTank().getMatter() - matter;
                helper.assertTrue(gained == 1, Component.literal("refined dust gave " + gained));
                helper.succeed();
            });
        });
    }

    private static void decomposerRejects(GameTestHelper helper) {
        DecomposerBlockEntity d = decomposer(helper, A);
        var inv = d.getInventory();
        helper.assertFalse(inv.isValid(DecomposerBlockEntity.INPUT, ItemResource.of(Items.DRAGON_EGG)), Component.literal("accepts dragon egg"));
        helper.assertFalse(inv.isValid(DecomposerBlockEntity.INPUT, ItemResource.of(MOItems.MATTER_DUST.get())), Component.literal("accepts unrefined dust"));
        helper.assertTrue(inv.isValid(DecomposerBlockEntity.INPUT, ItemResource.of(Items.DIAMOND)), Component.literal("rejects diamond"));
        helper.succeed();
    }

    private static void recyclerRefines(GameTestHelper helper) {
        helper.setBlock(A, MOBlocks.RECYCLER.get());
        RecyclerBlockEntity r = helper.getBlockEntity(A, RecyclerBlockEntity.class);
        r.getEnergy().set(r.getEnergy().getCapacity());
        // dust with 1 matter: 80 * ln(2)^2 = 38 ticks each
        r.getInventory().setStack(RecyclerBlockEntity.INPUT, MatterDustItem.withMatter(MOItems.MATTER_DUST.get(), 1).copyWithCount(2));
        helper.runAfterDelay(100, () -> {
            ItemStack out = r.getInventory().getStack(RecyclerBlockEntity.OUTPUT);
            helper.assertTrue(out.is(MOItems.MATTER_DUST_REFINED.get()) && out.getCount() == 2 && MatterDustItem.getMatter(out) == 1,
                    Component.literal("recycler output " + out + " matter " + MatterDustItem.getMatter(out)));
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
                inPipes += helper.getBlockEntity(new BlockPos(x, 1, 1), MatterPipeBlockEntity.class).getTank().getMatter();
            }
            int last = helper.getBlockEntity(new BlockPos(5, 1, 1), MatterPipeBlockEntity.class).getTank().getMatter();
            helper.assertTrue(last > 0, Component.literal("matter never reached the last pipe"));
            helper.assertTrue(inPipes + d.getMatterTank().getMatter() == 100,
                    Component.literal("matter not conserved: pipes " + inPipes + " decomposer " + d.getMatterTank().getMatter()));
            helper.succeed();
        });
    }

    private static void expect(GameTestHelper helper, net.minecraft.server.MinecraftServer server, Item item, int value) {
        int actual = MatterRegistry.get(server, item);
        helper.assertTrue(actual == value, Component.literal(item + " has " + actual + " matter, expected " + value));
    }

    private MatterGameTests() {}
}
