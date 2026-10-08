package matteroverdrive.gametest;

import matteroverdrive.entity.animal.FailedPig;
import matteroverdrive.entity.animal.FailedSheep;
import matteroverdrive.init.MOEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;

/** Phase 7 checks: world mobs. */
final class WorldGameTests {
    static void addAll() {
        MOGameTests.add("failed_animals", 20, false, WorldGameTests::failedAnimals);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
    }

    /** Failed animals breed failed young, and only with their own kind. */
    private static void failedAnimals(GameTestHelper helper) {
        var level = helper.getLevel();
        FailedPig pig = helper.spawnWithNoFreeWill(MOEntities.FAILED_PIG.get(), new BlockPos(2, 1, 2));
        FailedPig pig2 = helper.spawnWithNoFreeWill(MOEntities.FAILED_PIG.get(), new BlockPos(3, 1, 2));
        var vanilla = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        check(helper, pig.getBreedOffspring(level, pig2) instanceof FailedPig, "piglet isn't a failed pig");
        pig.setInLove(null);
        pig2.setInLove(null);
        vanilla.setInLove(null);
        check(helper, pig.canMate(pig2) && !pig.canMate(vanilla), "failed pigs mate with normal pigs");
        check(helper, MOEntities.FAILED_COW.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                .getBreedOffspring(level, null).getType() == MOEntities.FAILED_COW.get(), "calf isn't a failed cow");
        var chicken = helper.spawnWithNoFreeWill(MOEntities.FAILED_CHICKEN.get(), new BlockPos(6, 1, 6));
        check(helper, chicken.getBreedOffspring(level, chicken).getType() == MOEntities.FAILED_CHICKEN.get(), "chick isn't a failed chicken");
        FailedSheep sheep = helper.spawnWithNoFreeWill(MOEntities.FAILED_SHEEP.get(), new BlockPos(8, 1, 8));
        FailedSheep sheep2 = helper.spawnWithNoFreeWill(MOEntities.FAILED_SHEEP.get(), new BlockPos(9, 1, 8));
        sheep.setColor(DyeColor.RED);
        sheep2.setColor(DyeColor.YELLOW);
        var lamb = sheep.getBreedOffspring(level, sheep2);
        check(helper, lamb instanceof FailedSheep && lamb.getColor() == DyeColor.ORANGE, "lamb " + lamb);
        helper.succeed();
    }
}
