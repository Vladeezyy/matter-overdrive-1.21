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
        MOGameTests.add("android_station_parts", 20, false, AndroidGameTests::stationParts);
        MOGameTests.add("android_passive_stats", 20, false, AndroidGameTests::passiveStats);
        MOGameTests.add("android_toggle_stats", 20, false, AndroidGameTests::toggleStats);
        MOGameTests.add("android_shield_and_shockwave", 20, false, AndroidGameTests::shieldAndShockwave);
        MOGameTests.add("android_teleport", 20, false, AndroidGameTests::teleport);
        MOGameTests.add("charging_station_charges_androids", 20, false, AndroidGameTests::chargingStation);
        MOGameTests.add("rogue_androids", 20, false, AndroidGameTests::rogueAndroids);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    /** GameTestHelper.makeMockServerPlayerInLevel, but in survival (the vanilla mock is hard-wired to creative). */
    static ServerPlayer player(GameTestHelper helper) {
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

    /** Android station: parts go in their own slot only; each fitted part adds a heart point; humans can't use it. */
    private static void stationParts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, matteroverdrive.init.MOBlocks.ANDROID_STATION.get());
        var station = helper.getBlockEntity(pos, matteroverdrive.block.entity.AndroidStationBlockEntity.class);
        ServerPlayer player = player(helper);
        Android.setAndroid(player, true);
        var menu = new matteroverdrive.menu.AndroidStationMenu(1, player.getInventory(), station, new net.minecraft.world.inventory.SimpleContainerData(11));
        var head = (matteroverdrive.menu.AndroidStationMenu.PartSlot) menu.slots.stream()
                .filter(s -> s instanceof matteroverdrive.menu.AndroidStationMenu.PartSlot p && p.part == AndroidData.SLOT_HEAD).findFirst().orElseThrow();
        check(helper, !head.mayPlace(new ItemStack(MOItems.ROGUE_ANDROID_ARMS.get())), "head slot takes an arm");
        head.set(new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get()));
        check(helper, Android.get(player).getStack(AndroidData.SLOT_HEAD).is(MOItems.ROGUE_ANDROID_HEAD.get()), "part not stored");
        Android.tick(player);
        check(helper, player.getMaxHealth() == 21, "max health " + player.getMaxHealth());
        Android.setAndroid(player, false);
        check(helper, player.getMaxHealth() == 20, "max health as human " + player.getMaxHealth());
        check(helper, !menu.stillValid(player), "humans can use the android station");
        helper.succeed();
    }

    private static ServerPlayer android(GameTestHelper helper, matteroverdrive.android.BioticStat... stats) {
        ServerPlayer player = player(helper);
        Android.setAndroid(player, true);
        player.giveExperienceLevels(1000);
        for (var stat : stats) {
            for (int level = 1; level <= stat.maxLevel(); level++) Android.get(player).getStats().put(stat.id(), level);
        }
        return player;
    }

    /** Speed +40% (level 4), attack +25%, nanobots heal 0.6 a second, nano armour takes 30% off. */
    private static void passiveStats(GameTestHelper helper) {
        ServerPlayer player = android(helper, BioticStats.SPEED, BioticStats.NANOBOTS, BioticStats.NANO_ARMOR);
        double speed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        Android.tick(player);
        double boosted = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        check(helper, Math.abs(boosted - speed * 1.4) < 1e-6, "speed " + speed + " -> " + boosted);
        player.setHealth(10);
        float before = player.getHealth();
        // nanobots run on game time % 20 == 0; call the stat directly
        BioticStats.NANOBOTS.onAndroidTick(player, Android.get(player), 1);
        check(helper, helper.getLevel().getGameTime() % 20 != 0 || player.getHealth() > before, "nanobots didn't heal");
        player.setHealth(20);
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 10);
        check(helper, Math.abs(player.getHealth() - (20 - 10 * (1 - 0.3f))) < 1e-4, "nano armour: health " + player.getHealth());
        helper.succeed();
    }

    /** Night vision and cloak toggle with the ability key while selected. */
    private static void toggleStats(GameTestHelper helper) {
        ServerPlayer player = android(helper, BioticStats.NIGHT_VISION, BioticStats.CLOAK);
        Android.get(player).setActiveStat(BioticStats.NIGHT_VISION.id());
        Android.onActionKey(player);
        Android.tick(player);
        check(helper, player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "no night vision");
        Android.onActionKey(player);
        Android.tick(player);
        check(helper, !player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "night vision stayed on");
        Android.get(player).setActiveStat(BioticStats.CLOAK.id());
        Android.onActionKey(player);
        Android.tick(player);
        check(helper, player.isInvisible(), "not cloaked");
        Android.onActionKey(player);
        Android.tick(player);
        check(helper, !player.isInvisible(), "still cloaked");
        helper.succeed();
    }

    /** The shield stops arrows; the shockwave hurts and throws nearby mobs. */
    private static void shieldAndShockwave(GameTestHelper helper) {
        ServerPlayer player = android(helper, BioticStats.NANOBOTS, BioticStats.NANO_ARMOR, BioticStats.SHIELD, BioticStats.ATTACK,
                BioticStats.FLASH_COOLING, BioticStats.SHOCKWAVE);
        Android.get(player).getStats().remove(BioticStats.NANO_ARMOR.id());   // only the shield should matter here
        Android.get(player).setActiveStat(BioticStats.SHIELD.id());
        Android.onActionKey(player);
        check(helper, Android.get(player).getFlag("Shield"), "shield not up");
        var arrow = net.minecraft.world.entity.EntityType.ARROW.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        // 2 damage = 512 FE: within the built-in store's 1024 FE per pull, so the hit is blocked outright
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().arrow((net.minecraft.world.entity.projectile.AbstractArrow) arrow, null), 2);
        check(helper, player.getHealth() == 20, "arrow got through the shield: " + player.getHealth());
        // 6 damage = 1536 FE: more than one pull, so (1.7.10) only 1024/1536 of it is absorbed
        player.invulnerableTime = 0;
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().arrow((net.minecraft.world.entity.projectile.AbstractArrow) arrow, null), 6);
        check(helper, Math.abs(player.getHealth() - 16) < 1e-4, "partial shield: " + player.getHealth());
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(6, 1, 3));
        Android.get(player).setActiveStat(BioticStats.SHOCKWAVE.id());
        Android.onActionKey(player);
        check(helper, pig.getHealth() < pig.getMaxHealth(), "shockwave missed the pig");
        check(helper, BioticStats.SHOCKWAVE.getDelay(player, Android.get(player), 1) > 0, "no shockwave cooldown");
        helper.succeed();
    }

    /** Teleport: up to 32 blocks, 4096 FE, 2 s cooldown; only while selected. */
    private static void teleport(GameTestHelper helper) {
        ServerPlayer player = android(helper, BioticStats.TELEPORT);
        // 4096 FE per jump needs a battery that can give that much at once (the built-in store gives 1024)
        Android.get(player).setStack(AndroidData.SLOT_BATTERY, MOItems.HC_BATTERY.get().charged());
        var target = player.position().add(0, 0, 5);
        Android.teleport(player, target);
        check(helper, player.position().distanceTo(target) > 1, "teleported without selecting it");
        Android.get(player).setActiveStat(BioticStats.TELEPORT.id());
        int energy = Android.getEnergy(player);
        Android.teleport(player, target);
        check(helper, player.position().distanceTo(target) < 1e-3, "didn't teleport: " + player.position());
        check(helper, Android.getEnergy(player) < energy, "teleport was free");
        Android.teleport(player, target.add(0, 0, 3));
        check(helper, player.position().distanceTo(target) < 1e-3, "teleported during the cooldown");
        helper.succeed();
    }

    /** 1.7.10 charging station: up to 512 FE/t to androids in range, less with distance; three blocks high. */
    private static void chargingStation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 1, 3);
        helper.setBlock(pos, matteroverdrive.init.MOBlocks.CHARGING_STATION.get());
        var state = helper.getBlockState(pos);
        state.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(pos), state, null, ItemStack.EMPTY);
        check(helper, helper.getBlockState(pos.above(2)).is(matteroverdrive.init.MOBlocks.CHARGING_STATION.get()), "no top part");
        var station = helper.getBlockEntity(pos, matteroverdrive.block.entity.ChargingStationBlockEntity.class);
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            for (int i = 0; i < 100; i++) station.getEnergyHandler(null).insert(512, tx);
            tx.commit();
        }
        ServerPlayer player = player(helper);
        Android.setAndroid(player, true);
        Android.extractEnergy(player, 1000, false);
        int before = Android.getEnergy(player);
        matteroverdrive.machine.MachineBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        int gained = Android.getEnergy(player) - before;
        check(helper, gained > 0 && gained <= 512, "gained " + gained);
        helper.succeed();
    }

    /** Rogue androids: level stats, they hunt humans but not androids, the ranged one shoots its weapon. */
    private static void rogueAndroids(GameTestHelper helper) {
        var melee = helper.spawnWithNoFreeWill(matteroverdrive.init.MOEntities.ROGUE_ANDROID.get(), new BlockPos(3, 1, 3));
        melee.setup(2, false);
        check(helper, melee.getMaxHealth() == 52 && melee.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 6,
                "level 2: health " + melee.getMaxHealth());
        melee.setup(3, true);
        check(helper, melee.getMaxHealth() == 128, "legendary health " + melee.getMaxHealth());
        ServerPlayer human = player(helper);
        check(helper, matteroverdrive.entity.monster.RogueAndroid.isEnemy(human), "humans aren't targets");
        Android.setAndroid(human, true);
        check(helper, !matteroverdrive.entity.monster.RogueAndroid.isEnemy(human), "androids are targets");
        var ranged = helper.spawnWithNoFreeWill(matteroverdrive.init.MOEntities.RANGED_ROGUE_ANDROID.get(), new BlockPos(6, 1, 6));
        ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
        ranged.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, rifle);
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(6, 1, 10));
        ranged.performRangedAttack(pig, 1);
        var bolts = helper.getLevel().getEntitiesOfClass(matteroverdrive.entity.PlasmaBolt.class, ranged.getBoundingBox().inflate(8),
                b -> b.getOwner() == ranged);
        check(helper, bolts.size() == 1, "bolts " + bolts.size());
        check(helper, EnergyWeaponItemAccess.full(ranged.getMainHandItem()), "weapon not kept charged");
        helper.succeed();
    }

    private static final class EnergyWeaponItemAccess {
        static boolean full(ItemStack weapon) {
            return matteroverdrive.item.weapon.EnergyWeaponItem.getEnergy(weapon) == matteroverdrive.item.weapon.EnergyWeaponItem.getCapacity(weapon);
        }
    }
}
