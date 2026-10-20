package matteroverdrive.gametest;

import matteroverdrive.block.entity.FusionReactorControllerBlockEntity;
import matteroverdrive.block.entity.GravitationalAnomalyBlockEntity;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** Phase 4 checks: the fusion reactor multiblock. */
final class FusionGameTests {
    /** Controller facing north, so the ring (x -5..5, z 0..10 behind it) fits the 12x12 test area. */
    private static final BlockPos CONTROLLER = new BlockPos(6, 1, 0);

    static void addAll() {
        MOGameTests.add("fusion_reactor_generates", 120, false, h -> build(h, true, false, FusionGameTests::generates));
        MOGameTests.add("fusion_reactor_needs_coils", 100, false, h -> build(h, true, true, FusionGameTests::needsCoils));
        MOGameTests.add("fusion_reactor_needs_anomaly", 100, false, h -> build(h, false, false, FusionGameTests::needsAnomaly));
        MOGameTests.add("fusion_reactor_io_outputs", 160, false, h -> build(h, true, false, FusionGameTests::ioOutputs));
    }

    private interface Check {
        void run(GameTestHelper helper, FusionReactorControllerBlockEntity reactor);
    }

    /** Builds the ring from the controller's own position list (hull / coil / IO port), the anomaly in the centre. */
    private static void build(GameTestHelper helper, boolean anomaly, boolean hullForOneCoil, Check check) {
        helper.setBlock(CONTROLLER, MOBlocks.FUSION_REACTOR_CONTROLLER.get().defaultBlockState()
                .setValue(MachineBlock.FACING, helper.getTestRotation().rotate(Direction.NORTH)));
        FusionReactorControllerBlockEntity reactor = helper.getBlockEntity(CONTROLLER, FusionReactorControllerBlockEntity.class);
        boolean replaced = false;
        for (int i = 0; i < FusionReactorControllerBlockEntity.POSITION_COUNT; i++) {
            BlockPos abs = reactor.getPosition(i);
            int want = FusionReactorControllerBlockEntity.BLOCKS[i];
            Block block;
            if (want == 255) {
                if (!anomaly) continue;
                block = MOBlocks.GRAVITATIONAL_ANOMALY.get();
            } else if (want == 0) {
                block = MOBlocks.MACHINE_HULL.get();
            } else if (want == 1) {
                block = hullForOneCoil && !replaced ? MOBlocks.MACHINE_HULL.get() : MOBlocks.FUSION_REACTOR_COIL.get();
                replaced |= hullForOneCoil;
            } else {
                block = MOBlocks.FUSION_REACTOR_IO.get();
            }
            helper.getLevel().setBlockAndUpdate(abs, block.defaultBlockState());
            if (want == 255 && helper.getLevel().getBlockEntity(abs) instanceof GravitationalAnomalyBlockEntity a) {
                a.setMass(100000);   // real mass ln(2): 1420 FE/t at full efficiency
            }
        }
        reactor.getMatterTank().setMatter(100);
        check.run(helper, reactor);
    }

    private static void generates(GameTestHelper helper, FusionReactorControllerBlockEntity reactor) {
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(reactor.isValidStructure(), Component.literal("structure invalid: " + reactor.getStatus()));
            helper.assertTrue(reactor.getEnergyPerTick() == Math.round(2048 * Math.log(2)),
                    Component.literal("energy per tick " + reactor.getEnergyPerTick()));
            helper.assertTrue(reactor.getEnergy().getEnergy() > 0, Component.literal("no energy generated"));
            helper.succeed();
        });
    }

    private static void needsCoils(GameTestHelper helper, FusionReactorControllerBlockEntity reactor) {
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(reactor.isValidStructure(), Component.literal("valid without all coils"));
            helper.assertTrue(reactor.getStatus().contains("COILS"), Component.literal("status " + reactor.getStatus()));
            helper.assertTrue(reactor.getEnergy().getEnergy() == 0, Component.literal("generated anyway"));
            helper.succeed();
        });
    }

    private static void needsAnomaly(GameTestHelper helper, FusionReactorControllerBlockEntity reactor) {
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(reactor.isValidStructure(), Component.literal("valid without an anomaly"));
            helper.assertTrue(reactor.getStatus().contains("ANOMALY"), Component.literal("status " + reactor.getStatus()));
            helper.succeed();
        });
    }

    /** An inscriber next to the IO port (ring position 1) gets the reactor's energy. */
    private static void ioOutputs(GameTestHelper helper, FusionReactorControllerBlockEntity reactor) {
        BlockPos io = reactor.getPosition(1);
        BlockPos outside = io.relative(helper.getTestRotation().rotate(Direction.NORTH));
        helper.getLevel().setBlockAndUpdate(outside, MOBlocks.INSCRIBER.get().defaultBlockState());
        helper.runAfterDelay(120, () -> {
            var inscriber = (InscriberBlockEntity) helper.getLevel().getBlockEntity(outside);
            helper.assertTrue(inscriber.getEnergy().getEnergy() > 0, Component.literal("nothing came out of the IO port"));
            helper.succeed();
        });
    }

    private FusionGameTests() {}
}
