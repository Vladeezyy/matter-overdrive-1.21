package matteroverdrive.gametest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * In-game checks for the port, run headless with {@code ./gradlew runGameTestServer}.
 * They only register when game tests are enabled, so they never appear in a normal game.
 */
public final class MOGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, MatterOverdrive.MODID);

    /**
     * A registered test: body, tick budget, whether the area needs open sky, and a fixed time of day (-1: any).
     * Tests in one environment share a world and run together, so each time of day gets its own environment.
     */
    public record Spec(Consumer<GameTestHelper> body, int maxTicks, boolean skyAccess, int timeOfDay) {}

    private static final Map<String, Spec> TESTS = new LinkedHashMap<>();

    public static void add(String name, int maxTicks, boolean skyAccess, Consumer<GameTestHelper> body) {
        add(name, maxTicks, skyAccess, -1, body);
    }

    public static void add(String name, int maxTicks, boolean skyAccess, int timeOfDay, Consumer<GameTestHelper> body) {
        TESTS.put(name, new Spec(body, maxTicks, skyAccess, timeOfDay));
    }

    static {
        add("ores_in_overworld", 100, false, MOGameTests::oresInOverworld);
        add("ores_place_in_stone", 100, false, MOGameTests::oresPlaceInStone);
        add("dilithium_drops_crystal", 100, false, MOGameTests::dilithiumDropsCrystal);
        add("tritanium_smelts", 100, false, MOGameTests::tritaniumSmelts);
        add("tool_tiers", 100, false, MOGameTests::toolTiers);
        MachineGameTests.addAll();
        MatterGameTests.addAll();
        NetworkGameTests.addAll();
        AnomalyGameTests.addAll();
        FusionGameTests.addAll();
        WeaponGameTests.addAll();
        ModuleGameTests.addAll();
        AndroidGameTests.addAll();
        WorldGameTests.addAll();
        QuestGameTests.addAll();
        ContractGameTests.addAll();
        SecurityGameTests.addAll();
        TESTS.forEach((name, spec) -> FUNCTIONS.register(name, () -> spec.body()));
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(MOGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var anyTime = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        Map<Integer, Holder<TestEnvironmentDefinition>> timed = new HashMap<>();
        TESTS.forEach((name, spec) -> {
            var environment = spec.timeOfDay() < 0 ? anyTime : timed.computeIfAbsent(spec.timeOfDay(),
                    t -> event.registerEnvironment(id("time_" + t), new TestEnvironmentDefinition.TimeOfDay(t)));
            event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(environment, id("gametest_area"), spec.maxTicks(), 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, spec.skyAccess())));
        });
    }

    /** The biome modifiers attach both placed features to an overworld biome's ore step. */
    private static void oresInOverworld(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var plains = registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value();
        List<HolderSet<PlacedFeature>> steps = plains.getGenerationSettings().features();
        HolderSet<PlacedFeature> ores = steps.get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
        for (String ore : new String[] {"ore_tritanium", "ore_dilithium"}) {
            boolean found = ores.stream().anyMatch(h -> h.is(id(ore)));
            helper.assertTrue(found, Component.literal(ore + " missing from plains UNDERGROUND_ORES"));
        }
        helper.succeed();
    }

    /** Each configured ore feature actually turns stone into ore. */
    private static void oresPlaceInStone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var features = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var entry : new Object[][] {{"ore_tritanium", MOBlocks.TRITANIUM_ORE.get()}, {"ore_dilithium", MOBlocks.DILITHIUM_ORE.get()}}) {
            fill(level, origin, 9, Blocks.STONE.defaultBlockState());
            ConfiguredFeature<?, ?> feature = features.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, id((String) entry[0]))).value();
            RandomSource random = RandomSource.create(42);
            for (int i = 0; i < 4; i++) {
                feature.place(level, level.getChunkSource().getGenerator(), random, origin.offset(4, 4, 4));
            }
            int count = count(level, origin, 9, (Block) entry[1]);
            helper.assertTrue(count > 0, Component.literal(entry[0] + " placed no ore"));
        }
        fill(level, origin, 9, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    private static void dilithiumDropsCrystal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        List<ItemStack> drops = Block.getDrops(MOBlocks.DILITHIUM_ORE.get().defaultBlockState(), level, pos, null, null,
                new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(MOItems.DILITHIUM_CRYSTAL.get()),
                Component.literal("dilithium ore dropped " + drops));
        helper.succeed();
    }

    private static void tritaniumSmelts(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        var key = ResourceKey.<Recipe<?>>create(Registries.RECIPE, id("tritanium_ingot_from_ore_from_smelting"));
        helper.assertTrue(recipes.byKey(key).isPresent(), Component.literal("smelting recipe missing"));
        helper.succeed();
    }

    /** 1.7.10: tritanium ore/block need harvest level 2 (iron), dilithium ore level 3 (diamond); tritanium tools are level 2. */
    private static void toolTiers(GameTestHelper helper) {
        ItemStack pick = new ItemStack(MOItems.TRITANIUM_PICKAXE.get());
        BlockState tritaniumOre = MOBlocks.TRITANIUM_ORE.get().defaultBlockState();
        BlockState dilithiumOre = MOBlocks.DILITHIUM_ORE.get().defaultBlockState();
        helper.assertTrue(pick.isCorrectToolForDrops(tritaniumOre), Component.literal("tritanium pickaxe can't mine tritanium ore"));
        helper.assertFalse(pick.isCorrectToolForDrops(dilithiumOre), Component.literal("tritanium pickaxe shouldn't mine dilithium ore"));
        helper.assertFalse(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(tritaniumOre), Component.literal("stone pickaxe mines tritanium ore"));
        helper.assertTrue(new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(dilithiumOre), Component.literal("diamond pickaxe can't mine dilithium ore"));
        helper.succeed();
    }

    private static void fill(ServerLevel level, BlockPos origin, int size, BlockState state) {
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    private static int count(ServerLevel level, BlockPos origin, int size, Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            if (level.getBlockState(p).is(block)) n++;
        }
        return n;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    private MOGameTests() {}
}
