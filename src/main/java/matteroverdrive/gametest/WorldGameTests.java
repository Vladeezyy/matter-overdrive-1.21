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
        MOGameTests.add("mutant_scientist", 20, false, WorldGameTests::mutantScientist);
        MOGameTests.add("tritanium_crate", 20, false, WorldGameTests::tritaniumCrate);
        MOGameTests.add("buildings", 20, false, WorldGameTests::buildings);
        MOGameTests.add("matter_container", 20, false, WorldGameTests::matterContainer);
        MOGameTests.add("portable_decomposer", 20, false, WorldGameTests::portableDecomposer);
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

    /** Mutant scientist: 256 health, rogue androids hunt it. */
    private static void mutantScientist(GameTestHelper helper) {
        var mutant = helper.spawnWithNoFreeWill(MOEntities.MUTANT_SCIENTIST.get(), new BlockPos(3, 1, 3));
        check(helper, mutant.getMaxHealth() == 256 && mutant.getHealth() == 256, "health " + mutant.getMaxHealth());
        check(helper, matteroverdrive.entity.monster.RogueAndroid.isEnemy(mutant), "rogue androids ignore mutants");
        helper.succeed();
    }

    /** Tritanium crate: 54 slots, keeps its contents in the dropped item. */
    private static void tritaniumCrate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, matteroverdrive.init.MOBlocks.crate(DyeColor.ORANGE).get());
        var crate = (matteroverdrive.block.entity.TritaniumCrateBlockEntity) helper.getBlockEntity(pos, matteroverdrive.block.entity.TritaniumCrateBlockEntity.class);
        check(helper, crate.getContainerSize() == 54, "size " + crate.getContainerSize());
        crate.setItem(53, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
        BlockPos abs = helper.absolutePos(pos);
        var drops = net.minecraft.world.level.block.Block.getDrops(helper.getLevel().getBlockState(abs), helper.getLevel(), abs, crate,
                null, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        check(helper, drops.size() == 1, "drops " + drops);
        var contents = drops.get(0).get(net.minecraft.core.component.DataComponents.CONTAINER);
        check(helper, contents != null && contents.nonEmptyStream().anyMatch(s -> s.getCount() == 7), "contents not kept: " + contents);
        helper.succeed();
    }

    /** Image buildings: every template loads with its 1.7.10 size and every structure is registered from its JSON. */
    private static void buildings(GameTestHelper helper) {
        var structures = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        int[][] sizes = {{21, 9, 21}, {24, 16, 24}, {11, 6, 35}, {43, 25, 43}, {58, 16, 23}};
        for (var building : matteroverdrive.world.Building.values()) {
            var t = building.template();
            int[] size = sizes[building.ordinal()];
            check(helper, t.width() == size[0] && t.height() == size[1] && t.depth() == size[2],
                    building + " size " + t.width() + "x" + t.height() + "x" + t.depth());
            var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("matteroverdrive", building.getSerializedName());
            check(helper, structures.get(key).isPresent(), "structure " + key + " missing");
        }
        helper.succeed();
    }

    /** Matter container: holds exactly 32 mB of Matter Plasma through the fluid item capability; the plasma block is a source. */
    private static void matterContainer(GameTestHelper helper) {
        var plasma = net.neoforged.neoforge.transfer.fluid.FluidResource.of(matteroverdrive.init.MOFluids.MATTER_PLASMA.get());
        var slots = new net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler(2);
        slots.set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(matteroverdrive.init.MOItems.MATTER_CONTAINER.get()), 1);
        var access = net.neoforged.neoforge.transfer.access.ItemAccess.forHandlerIndex(slots, 0);
        var tank = access.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM);
        check(helper, tank != null, "no fluid capability");
        int partial, full;
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            partial = tank.insert(0, plasma, 10, tx);
        }
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            full = tank.insert(0, plasma, 100, tx);
            tx.commit();
        }
        check(helper, partial == 0 && full == 32, "inserted " + partial + " / " + full);
        check(helper, slots.getResource(0).is(matteroverdrive.init.MOItems.MATTER_CONTAINER_FULL.get()), "container not full: " + slots.getResource(0));
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, matteroverdrive.init.MOBlocks.MATTER_PLASMA.get());
        var fluid = helper.getLevel().getFluidState(helper.absolutePos(pos));
        check(helper, fluid.isSource() && fluid.getType() == matteroverdrive.init.MOFluids.MATTER_PLASMA.get(), "plasma block " + fluid);
        helper.succeed();
    }

    /** Portable decomposer: listed items become 10% of their matter for 1 FE per point; others are left alone. */
    private static void portableDecomposer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var item = net.minecraft.world.item.Items.IRON_INGOT;
        int value = matteroverdrive.matter.MatterHelper.getMatter(server, new net.minecraft.world.item.ItemStack(item));
        check(helper, value > 0, "iron has no matter");
        var decomposer = new net.minecraft.world.item.ItemStack(matteroverdrive.init.MOItems.PORTABLE_DECOMPOSER.get());
        decomposer.set(matteroverdrive.init.MODataComponents.ENERGY.get(), 100000);
        var dirt = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 10);
        matteroverdrive.item.PortableDecomposerItem.decompose(server, decomposer, dirt);
        check(helper, dirt.getCount() == 10, "unlisted item decomposed");
        check(helper, matteroverdrive.item.PortableDecomposerItem.addToList(decomposer, new net.minecraft.world.item.ItemStack(item)), "not listed");
        var ingots = new net.minecraft.world.item.ItemStack(item, 3);
        matteroverdrive.item.PortableDecomposerItem.decompose(server, decomposer, ingots);
        float matter = matteroverdrive.item.PortableDecomposerItem.getMatter(decomposer);
        check(helper, ingots.isEmpty() && Math.abs(matter - 3 * value * 0.1f) < 0.01f
                && matteroverdrive.item.PortableDecomposerItem.getEnergy(decomposer) == 100000 - 3 * value,
                "matter " + matter + ", energy " + matteroverdrive.item.PortableDecomposerItem.getEnergy(decomposer) + ", left " + ingots.getCount());
        helper.succeed();
    }
}
