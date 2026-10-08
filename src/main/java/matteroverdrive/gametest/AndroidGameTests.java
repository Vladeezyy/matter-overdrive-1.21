package matteroverdrive.gametest;

import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidData;
import matteroverdrive.android.BioticStats;
import matteroverdrive.init.MOItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Phase 6 checks: becoming an android, android energy, stats. */
final class AndroidGameTests {
    static void addAll() {
        MOGameTests.add("android_transformation", 20, false, AndroidGameTests::transformation);
        MOGameTests.add("android_energy", 20, false, AndroidGameTests::energy);
        MOGameTests.add("android_battery_slot", 20, false, AndroidGameTests::batterySlot);
        MOGameTests.add("android_pills", 20, false, AndroidGameTests::pills);
        MOGameTests.add("android_stat_rules", 20, false, AndroidGameTests::statRules);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    /** GameTestHelper.makeMockServerPlayerInLevel, but in survival (the vanilla mock is hard-wired to creative). */
    private static ServerPlayer player(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-android"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public net.minecraft.world.level.GameType gameMode() {
                return net.minecraft.world.level.GameType.SURVIVAL;
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setClientLoaded(true);      // players are invulnerable until their client reports it has loaded
        net.minecraft.world.level.GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());   // abilities came from the creative default
        var pos = helper.absolutePos(new BlockPos(6, 1, 1)).getBottomCenter();
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    /** Red pill: 34 s of turning, then android - and dead (1.7.10 TRANSFORMATION_DEATH). */
    private static void transformation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Android.startTransformation(player);
        check(helper, Android.get(player).getTurning() == Android.TRANSFORM_TIME, "turning " + Android.get(player).getTurning());
        Android.tick(player);
        check(helper, player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA), "no sickness while turning");
        Android.setTurning(player, 1);
        Android.tick(player);
        check(helper, Android.isAndroid(player), "not an android after turning");
        check(helper, player.isDeadOrDying(), "survived the transformation");
        helper.succeed();
    }

    /** Built-in store: at most 1024 FE per pull; hunger is filled from energy at 256 FE per point. */
    private static void energy(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Android.setAndroid(player, true);
        check(helper, Android.getEnergy(player) == AndroidData.MAX_ENERGY, "energy " + Android.getEnergy(player));
        check(helper, Android.extractEnergy(player, 5000, false) == Android.BUILTIN_ENERGY_TRANSFER, "builtin transfer cap");
        int before = Android.getEnergy(player);
        player.getFoodData().setFoodLevel(18);
        Android.tick(player);
        check(helper, player.getFoodData().getFoodLevel() == 20, "food " + player.getFoodData().getFoodLevel());
        check(helper, Android.getEnergy(player) == before - 2 * Android.ENERGY_FOOD_MULTIPLY, "energy after food " + Android.getEnergy(player));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 200));
        Android.tick(player);
        check(helper, player.getActiveEffects().isEmpty(), "androids keep potion effects");
        helper.succeed();
    }

    /** A battery in the android's battery slot replaces the built-in store. */
    private static void batterySlot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Android.setAndroid(player, true);
        Android.get(player).setStack(AndroidData.SLOT_BATTERY, MOItems.BATTERY.get().charged());
        check(helper, Android.getMaxEnergy(player) == 1 << 19, "max " + Android.getMaxEnergy(player));
        int extracted = Android.extractEnergy(player, 5000, false);
        check(helper, extracted == 800, "extracted " + extracted);     // battery max extract
        check(helper, Android.getEnergy(player) == (1 << 19) - 800, "battery energy " + Android.getEnergy(player));
        helper.succeed();
    }

    /** Yellow pill: forget all stats for half their XP; blue pill: human again. */
    private static void pills(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Android.setAndroid(player, true);
        player.giveExperienceLevels(100);
        check(helper, Android.tryUnlock(player, BioticStats.NANOBOTS, 1), "couldn't unlock nanobots");
        check(helper, Android.tryUnlock(player, BioticStats.SPEED, 1), "couldn't unlock speed");
        check(helper, player.experienceLevel == 100 - 26 - 18, "xp " + player.experienceLevel);
        ItemStack yellow = new ItemStack(MOItems.ANDROID_PILL_YELLOW.get());
        yellow.finishUsingItem(helper.getLevel(), player);
        check(helper, Android.get(player).getStats().isEmpty(), "stats kept");
        check(helper, player.experienceLevel == 100 - 26 - 18 + (26 + 18) / 2, "xp after reset " + player.experienceLevel);
        new ItemStack(MOItems.ANDROID_PILL_BLUE.get()).finishUsingItem(helper.getLevel(), player);
        check(helper, !Android.isAndroid(player), "still an android");
        helper.succeed();
    }

    /** 1.7.10 stat tree: roots must be maxed, competitors lock each other out, items and XP are needed. */
    private static void statRules(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.giveExperienceLevels(200);
        check(helper, !Android.tryUnlock(player, BioticStats.NANOBOTS, 1), "humans can unlock stats");
        Android.setAndroid(player, true);
        check(helper, !Android.tryUnlock(player, BioticStats.NANO_ARMOR, 1), "nano armor without nanobots");
        check(helper, Android.tryUnlock(player, BioticStats.NANOBOTS, 1), "nanobots");
        check(helper, Android.tryUnlock(player, BioticStats.ATTACK, 1), "attack");
        check(helper, !Android.tryUnlock(player, BioticStats.NANO_ARMOR, 1), "nano armor next to its competitor attack");
        check(helper, !Android.tryUnlock(player, BioticStats.MINIMAP, 1), "minimap without a compass");
        player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.COMPASS));
        check(helper, Android.tryUnlock(player, BioticStats.MINIMAP, 1), "minimap with a compass");
        check(helper, player.getInventory().countItem(net.minecraft.world.item.Items.COMPASS) == 0, "compass not consumed");
        helper.succeed();
    }
}
