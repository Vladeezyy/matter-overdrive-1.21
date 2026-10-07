package matteroverdrive.gametest;

import matteroverdrive.matter.MatterRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Phase 3 checks: matter values. */
final class MatterGameTests {
    static void addAll() {
        MOGameTests.add("matter_values", 20, false, MatterGameTests::matterValues);
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

    private static void expect(GameTestHelper helper, net.minecraft.server.MinecraftServer server, Item item, int value) {
        int actual = MatterRegistry.get(server, item);
        helper.assertTrue(actual == value, Component.literal(item + " has " + actual + " matter, expected " + value));
    }

    private MatterGameTests() {}
}
