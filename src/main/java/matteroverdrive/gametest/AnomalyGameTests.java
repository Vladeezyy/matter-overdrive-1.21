package matteroverdrive.gametest;

import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.world.GravitationalAnomalyFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Phase 4 checks: the gravitational anomaly, the stabilizer and the space-time equalizer. */
final class AnomalyGameTests {
    private static final BlockPos CORE = new BlockPos(6, 4, 6);

    static void addAll() {
        MOGameTests.add("anomaly_formulas", 20, false, AnomalyGameTests::formulas);
        MOGameTests.add("anomaly_swallows_items", 80, false, AnomalyGameTests::swallowsItems);
        MOGameTests.add("anomaly_pulls_mobs", 60, false, AnomalyGameTests::pullsMobs);
        MOGameTests.add("equalizer_stops_pull", 60, false, AnomalyGameTests::equalizerStopsPull);
        MOGameTests.add("nether_star_collapses_anomaly", 80, false, AnomalyGameTests::netherStarCollapse);
        MOGameTests.add("stabilizer_suppresses_anomaly", 40, false, AnomalyGameTests::stabilizerSuppresses);
        MOGameTests.add("anomaly_feature_places", 20, false, AnomalyGameTests::featurePlaces);
    }

    private static GravitationalAnomalyBlockEntity anomaly(GameTestHelper helper, long mass) {
        helper.setBlock(CORE, MOBlocks.GRAVITATIONAL_ANOMALY.get());
        GravitationalAnomalyBlockEntity a = helper.<GravitationalAnomalyBlockEntity>getBlockEntity(CORE);
        a.setMass(mass);
        return a;
    }

    private static void formulas(GameTestHelper helper) {
        GravitationalAnomalyBlockEntity a = anomaly(helper, 6000);
        double m = Math.log1p(6000 * 1e-5);
        near(helper, a.getRealMass(), m, "real mass");
        near(helper, a.getMaxRange(), Math.sqrt(m * 6.67384 / 0.01), "range");
        near(helper, a.getBlockBreakRange(), Math.sqrt(m * 6.67384 / 0.01) / 2, "break range");
        near(helper, a.getEventHorizon(), 0.5, "event horizon floor");
        helper.succeed();
    }

    private static void swallowsItems(GameTestHelper helper) {
        GravitationalAnomalyBlockEntity a = anomaly(helper, 6000);
        ItemEntity item = helper.spawnItem(Items.IRON_INGOT, new BlockPos(6, 4, 8).getCenter());
        item.setDeltaMovement(0, 0, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(item.isRemoved(), "ingot still there, " + item.position());
            helper.assertTrue(a.getMass() == 6000 + 32, "mass " + a.getMass() + ", expected 6032");
            helper.succeed();
        });
    }

    private static void pullsMobs(GameTestHelper helper) {
        anomaly(helper, 100000);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 3, 10));
        double start = pig.distanceToSqr(helper.absolutePos(CORE).getCenter());
        // mass 100000: ~0.26 blocks/tick^2 at this distance, so check before the pig flies through the core
        helper.runAfterDelay(5, () -> {
            double now = pig.isRemoved() ? 0 : pig.distanceToSqr(helper.absolutePos(CORE).getCenter());
            helper.assertTrue(now < start, "pig not pulled: " + start + " -> " + now);
            helper.succeed();
        });
    }

    private static void equalizerStopsPull(GameTestHelper helper) {
        anomaly(helper, 100000);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 3, 10));
        pig.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MOItems.SPACETIME_EQUALIZER.get()));
        var start = pig.position();
        helper.runAfterDelay(20, () -> {
            double moved = Math.abs(pig.position().z - start.z) + Math.abs(pig.position().x - start.x);
            helper.assertTrue(moved < 0.05, "pig with equalizer moved " + moved);
            helper.succeed();
        });
    }

    private static void netherStarCollapse(GameTestHelper helper) {
        anomaly(helper, 6000);
        helper.spawnItem(Items.NETHER_STAR, new BlockPos(6, 4, 7).getCenter());
        helper.runAfterDelay(60, () -> {
            helper.assertBlockNotPresent(MOBlocks.GRAVITATIONAL_ANOMALY.get(), CORE);
            helper.succeed();
        });
    }

    private static void stabilizerSuppresses(GameTestHelper helper) {
        GravitationalAnomalyBlockEntity a = anomaly(helper, 6000);
        helper.setBlock(new BlockPos(6, 4, 2), MOBlocks.GRAVITATIONAL_STABILIZER.get().defaultBlockState()
                .setValue(MachineBlock.FACING, helper.getTestRotation().rotate(Direction.SOUTH)));
        helper.runAfterDelay(10, () -> {
            near(helper, a.getSuppression(), 0.7, "suppression");
            helper.succeed();
        });
    }

    private static void featurePlaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(ResourceKey.create(
                Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("matteroverdrive", "gravitational_anomaly"))).value();
        BlockPos pos = helper.absolutePos(CORE);
        helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos),
                "feature refused to place");
        helper.assertTrue(level.getBlockEntity(pos) instanceof GravitationalAnomalyBlockEntity a
                        && a.getMass() >= GravitationalAnomalyFeature.MIN_MASS && a.getMass() < GravitationalAnomalyFeature.MAX_MASS,
                "no anomaly with a valid mass at " + pos);
        helper.succeed();
    }

    private static void near(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1e-6, what + ": " + actual + " != " + expected);
    }

    private AnomalyGameTests() {}
}
