package matteroverdrive.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.AnalyzerBlockEntity;
import matteroverdrive.block.entity.DecomposerBlockEntity;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.block.entity.PatternStorageBlockEntity;
import matteroverdrive.block.entity.ReplicatorBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.machine.MachineBlock;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Performance and look checks for development, inert unless {@code matteroverdrive.perf} is set
 * ({@code ./gradlew runPerf -Pperf=stress|hands}, world {@code run/saves/mo_scene}):
 * <ul>
 * <li>{@code stress}: the same field measured empty, with 576 burning vanilla furnaces and with 48 working Matter Overdrive
 * modules (576 machines and pipes: decomposer, matter pipes, replicator, solar panel, inscriber, analyzer, pattern storage,
 * network pipes, pattern monitor). Logs {@code [perf] PERF} lines with the frame rate and the server tick time, then quits.</li>
 * <li>{@code hands}: holds the big items (energy weapons, omni tool, machines with OBJ models) and saves first and third
 * person screenshots {@code run/screenshots/hand_*.png}, then quits.</li>
 * </ul>
 */
public final class PerfScene {
    private static final String MODE = System.getProperty("matteroverdrive.perf", "");
    private static final int MODULES_X = 6, MODULES_Z = 8;
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;
    private static BlockPos field;
    private static volatile boolean measuring;
    private static long frames, measureStart, tickNanos, tickMax;
    private static int tickSamples;

    private record Step(int at, Consumer<Minecraft> action) {}

    /** Builds the steps on the first tick (static fields below are not set yet while this class initialises). */
    private static void initSteps() {
        if (MODE.equals("stress")) {
            stressSteps();
        } else if (MODE.equals("types")) {
            typeSteps();
        } else if (MODE.equals("hands")) {
            handSteps();
        } else if (MODE.equals("look")) {
            lookSteps();
        }
    }

    private static void at(int t, Consumer<Minecraft> action) {
        STEPS.add(new Step(t, action));
    }

    // --- stress ----------------------------------------------------------------------------------

    private static void stressSteps() {
        int t = 40;
        at(t, mc -> {
            // the frame rate must not hit a limit, or both sides of a comparison read the same number
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            // 1.21.2+ caps an idle game ("AFK" after a minute without input) at 30 FPS: nobody touches it during the run
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            mc.options.hideGui = true;
            server(mc, PerfScene::prepareField);
        });
        // the world settles first (chunks, lighting), so the empty baseline is not inflated by loading
        at(t += 400, mc -> startMeasure());
        at(t += 300, mc -> {
            stopMeasure("empty field");
            server(mc, p -> fill(p.serverLevel(), false));
        });
        at(t += 200, mc -> startMeasure());
        at(t += 300, mc -> {
            stopMeasure("576 burning vanilla furnaces");
            server(mc, p -> fill(p.serverLevel(), true));
        });
        at(t += 200, mc -> startMeasure());
        at(t += 300, mc -> {
            stopMeasure("48 Matter Overdrive modules (576 machines and pipes), working");
            server(mc, p -> {
                long matter = 0;
                int working = 0;
                for (BlockPos pos : modules()) {
                    if (p.serverLevel().getBlockEntity(pos) instanceof DecomposerBlockEntity d) {
                        matter += d.getMatterTank().getMatter();
                    }
                    if (p.serverLevel().getBlockEntity(pos.offset(4, 0, 0)) instanceof ReplicatorBlockEntity r && r.getMatterTank().getMatter() > 0) {
                        working++;
                    }
                }
                MatterOverdrive.LOGGER.info("[perf] CHECK decomposers hold {} matter, {} of {} replicators got matter through the pipes", matter, working, MODULES_X * MODULES_Z);
            });
        });
        at(t += 20, mc -> {
            MatterOverdrive.LOGGER.info("[perf] DONE");
            mc.options.hideGui = false;
            mc.stop();
        });
    }

    private static void startMeasure() {
        frames = 0;
        tickNanos = 0;
        tickMax = 0;
        tickSamples = 0;
        measureStart = System.nanoTime();
        measuring = true;
    }

    private static void stopMeasure(String name) {
        measuring = false;
        double seconds = (System.nanoTime() - measureStart) / 1e9;
        double mspt = tickSamples == 0 ? 0 : tickNanos / (double) tickSamples / 1e6;
        MatterOverdrive.LOGGER.info(String.format("[perf] PERF %s: %.1f FPS, %.2f ms per server tick (worst 100-tick window %.2f ms)",
                name, frames / seconds, mspt, tickMax / 1e6));
    }

    /** After each rendered frame (both loaders call this) */
    public static void onFrame() {
        if (measuring) frames++;
    }

    private static List<BlockPos> modules() {
        List<BlockPos> list = new ArrayList<>();
        for (int x = 0; x < MODULES_X; x++) {
            for (int z = 0; z < MODULES_Z; z++) {
                list.add(field.offset(x * 8 - MODULES_X * 4, 0, z * 4 - MODULES_Z * 2));
            }
        }
        return list;
    }

    /** A flat stone field 100 blocks east of the spawn, the player above its south edge looking over it. */
    private static void prepareField(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        BlockPos spawn = level.getSharedSpawnPos();
        // high in the air: the same open sky and light for every run, no caves or terrain around
        field = new BlockPos(spawn.getX() + 100, 150, spawn.getZ());
        clear(level);
        player.teleportTo(level, field.getX() + 0.5, field.getY() + 18, field.getZ() + 26.5, Set.of(), 180f, 35f);
    }

    private static void clear(ServerLevel level) {
        for (BlockPos p : BlockPos.betweenClosed(field.offset(-28, -1, -20), field.offset(28, 3, 20))) {
            BlockState state = p.getY() == field.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            setQuietly(level, p, state);
        }
        level.getEntitiesOfClass(ItemEntity.class, new AABB(field).inflate(40)).forEach(e -> e.discard());
        level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, new AABB(field).inflate(40)).forEach(e -> e.discard());
    }

    /** 48 modules of 12 blocks each: vanilla furnaces or working Matter Overdrive machines in the same places. */
    private static void fill(ServerLevel level, boolean matterOverdrive) {
        clear(level);
        for (BlockPos m : modules()) {
            if (!matterOverdrive) {
                for (int x = 0; x < 6; x++) {
                    for (int z = 0; z < 2; z++) {
                        BlockPos p = m.offset(x, 0, z);
                        level.setBlockAndUpdate(p, Blocks.FURNACE.defaultBlockState());
                        if (level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity furnace) {
                            furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
                            furnace.setItem(1, new ItemStack(Items.COAL, 64));
                        }
                    }
                }
                continue;
            }
            // row 1: decomposer -> matter pipes -> replicator, solar panel
            machine(level, m, MOBlocks.DECOMPOSER.get());
            level.setBlockAndUpdate(m.offset(1, 0, 0), MOBlocks.MATTER_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(m.offset(2, 0, 0), MOBlocks.MATTER_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(m.offset(3, 0, 0), MOBlocks.HEAVY_MATTER_PIPE.get().defaultBlockState());
            machine(level, m.offset(4, 0, 0), MOBlocks.REPLICATOR.get());
            machine(level, m.offset(5, 0, 0), MOBlocks.SOLAR_PANEL.get());
            // row 2: inscriber, analyzer, pattern storage - network pipe - pattern monitor - network pipe
            machine(level, m.offset(0, 0, 1), MOBlocks.INSCRIBER.get());
            machine(level, m.offset(1, 0, 1), MOBlocks.ANALYZER.get());
            machine(level, m.offset(2, 0, 1), MOBlocks.PATTERN_STORAGE.get());
            level.setBlockAndUpdate(m.offset(3, 0, 1), MOBlocks.NETWORK_PIPE.get().defaultBlockState());
            machine(level, m.offset(4, 0, 1), MOBlocks.PATTERN_MONITOR.get());
            level.setBlockAndUpdate(m.offset(5, 0, 1), MOBlocks.NETWORK_PIPE.get().defaultBlockState());

            var decomposer = (DecomposerBlockEntity) level.getBlockEntity(m);
            decomposer.getEnergy().set(400000);
            decomposer.getInventory().setStack(DecomposerBlockEntity.INPUT, new ItemStack(Items.COBBLESTONE, 64));
            var replicator = (ReplicatorBlockEntity) level.getBlockEntity(m.offset(4, 0, 0));
            replicator.getEnergy().set(500000);
            replicator.getInventory().setStack(ReplicatorBlockEntity.SHIELDING, new ItemStack(MOItems.TRITANIUM_PLATE.get(), 5));
            replicator.setTask(new ReplicatorBlockEntity.Task(new matteroverdrive.matter.ItemPattern(Items.COBBLESTONE.builtInRegistryHolder(), 100), 64));
            var inscriber = (InscriberBlockEntity) level.getBlockEntity(m.offset(0, 0, 1));
            inscriber.getEnergy().set(300000);
            inscriber.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 64));
            inscriber.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 64));
            var analyzer = (AnalyzerBlockEntity) level.getBlockEntity(m.offset(1, 0, 1));
            analyzer.getEnergy().set(500000);
            analyzer.getInventory().setStack(AnalyzerBlockEntity.INPUT, new ItemStack(Items.DIAMOND, 64));
            analyzer.getInventory().setStack(AnalyzerBlockEntity.DATABASE, new ItemStack(MOItems.PATTERN_DRIVE.get()));
            var storage = (PatternStorageBlockEntity) level.getBlockEntity(m.offset(2, 0, 1));
            storage.getEnergy().set(60000);
            Item[] patterns = {Items.IRON_INGOT, Items.COBBLESTONE, Items.GOLD_INGOT};
            for (int i = 0; i < patterns.length; i++) {
                ItemStack drive = new ItemStack(MOItems.PATTERN_DRIVE.get());
                MOItems.PATTERN_DRIVE.get().addProgress(drive, patterns[i], 100);
                storage.getInventory().setStack(i, drive);
            }
        }
    }

    private static void machine(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
    }

    // --- one machine type at a time --------------------------------------------------------------

    private static final String[] TYPES = {"minecraft:furnace", "decomposer", "replicator", "inscriber", "inscriber/idle", "pattern_storage", "pattern_monitor",
            "matter_analyzer", "solar_panel", "matter_pipe", "network_pipe", "tritanium_crate_white", "star_map", "weapon_station",
            "android_station", "transporter", "gravitational_stabilizer"};

    /** {@code types}: the field filled with 576 blocks of one kind at a time, set up to work where they can. */
    private static void typeSteps() {
        int t = 40;
        at(t, mc -> {
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            // 1.21.2+ caps an idle game ("AFK" after a minute without input) at 30 FPS: nobody touches it during the run
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            mc.options.hideGui = true;
            server(mc, PerfScene::prepareField);
        });
        t += 400;
        // -Dmatteroverdrive.perf.types=a,b limits the run to those kinds (to profile one renderer)
        String only = System.getProperty("matteroverdrive.perf.types", "");
        for (String id : TYPES) {
            if (!only.isEmpty() && !List.of(only.split(",")).contains(id)) continue;
            // "<block>/idle": the machines get no work (an idle inscriber shows its rail and head at rest)
            boolean idle = id.endsWith("/idle");
            String block = idle ? id.substring(0, id.length() - 5) : id;
            var key = new net.minecraft.resources.ResourceLocation(block.contains(":") ? block : MatterOverdrive.MODID + ":" + block);
            at(t, mc -> server(mc, p -> fillWith(p.serverLevel(), net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key), idle)));
            String shotName = "types_" + id.replace("minecraft:", "vanilla_").replace("/", "_");
            // a close look at a few of them (the same spot every run) before measuring
            at(t + 60, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), field.getX() - 20.5, field.getY() + 2.5, field.getZ() + 21.5, Set.of(), 160f, 25f)));
            at(t + 80, mc -> shot(mc, shotName));
            at(t + 82, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), field.getX() + 0.5, field.getY() + 18, field.getZ() + 26.5, Set.of(), 180f, 35f)));
            at(t += 140, mc -> startMeasure());
            at(t += 200, mc -> stopMeasure("576 x " + id));
            t += 1;
        }
        at(t + 5, mc -> {
            MatterOverdrive.LOGGER.info("[perf] DONE");
            mc.options.hideGui = false;
            mc.stop();
        });
    }

    private static void fillWith(ServerLevel level, Block block, boolean idle) {
        clear(level);
        for (BlockPos m : modules()) {
            for (int x = 0; x < 6; x++) {
                for (int z = 0; z < 2; z++) {
                    BlockPos p = m.offset(x, 0, z);
                    BlockState state = block.defaultBlockState();
                    if (state.hasProperty(MachineBlock.FACING)) state = state.setValue(MachineBlock.FACING, Direction.SOUTH);
                    level.setBlockAndUpdate(p, state);
                    var be = level.getBlockEntity(p);
                    if (idle) continue;
                    if (be instanceof AbstractFurnaceBlockEntity furnace) {
                        furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
                        furnace.setItem(1, new ItemStack(Items.COAL, 64));
                    } else if (be instanceof DecomposerBlockEntity d) {
                        d.getEnergy().set(400000);
                        d.getInventory().setStack(DecomposerBlockEntity.INPUT, new ItemStack(Items.COBBLESTONE, 64));
                    } else if (be instanceof ReplicatorBlockEntity r) {
                        r.getEnergy().set(500000);
                        r.getMatterTank().setMatter(1000);
                        r.getInventory().setStack(ReplicatorBlockEntity.SHIELDING, new ItemStack(MOItems.TRITANIUM_PLATE.get(), 5));
                        r.setTask(new ReplicatorBlockEntity.Task(new matteroverdrive.matter.ItemPattern(Items.COBBLESTONE.builtInRegistryHolder(), 100), 64));
                    } else if (be instanceof InscriberBlockEntity i) {
                        i.getEnergy().set(300000);
                        i.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 64));
                        i.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 64));
                    } else if (be instanceof PatternStorageBlockEntity storage) {
                        storage.getEnergy().set(60000);
                        for (int d = 0; d < 3; d++) {
                            ItemStack drive = new ItemStack(MOItems.PATTERN_DRIVE.get());
                            MOItems.PATTERN_DRIVE.get().addProgress(drive, Items.COBBLESTONE, 100);
                            storage.getInventory().setStack(d, drive);
                        }
                    } else if (be instanceof AnalyzerBlockEntity a) {
                        a.getEnergy().set(500000);
                        a.getInventory().setStack(AnalyzerBlockEntity.INPUT, new ItemStack(Items.DIAMOND, 64));
                        a.getInventory().setStack(AnalyzerBlockEntity.DATABASE, new ItemStack(MOItems.PATTERN_DRIVE.get()));
                    } else if (be instanceof matteroverdrive.block.entity.MatterPipeBlockEntity pipe) {
                        pipe.getTank().setMatter(16);
                    }
                }
            }
        }
    }

    // --- an old world as it was saved -------------------------------------------------------------

    /**
     * {@code look} ({@code -Pworld=<save>}): builds nothing, stands in front of DevScene's pattern storage (spawn base
     * -1, 0, -7) and the inscriber (-1, 0, -3) and takes screenshots: a world saved by an older version must show the
     * same machines (the storage's drives come from block state properties it did not have then).
     */
    private static void lookSteps() {
        at(40, mc -> server(mc, p -> {
            ServerLevel level = p.serverLevel();
            BlockPos spawn = level.getSharedSpawnPos();
            BlockPos base = new BlockPos(spawn.getX(), level.getMinBuildHeight() + 4, spawn.getZ());
            p.setGameMode(GameType.SPECTATOR);
            p.teleportTo(level, base.getX() - 0.5, base.getY() + 0.6, base.getZ() - 4.6, Set.of(), 180f, 10f);
        }));
        at(45, mc -> mc.options.hideGui = true);
        // the first machine tick sets the drive properties; the client gets the block update
        at(80, mc -> {
            shot(mc, "look_storage");
            mc.player.connection.sendCommand("data get block ~-1 ~-0.6 ~-2.4");
        });
        at(85, mc -> server(mc, p -> {
            BlockPos spawn = p.serverLevel().getSharedSpawnPos();
            BlockPos storage = new BlockPos(spawn.getX() - 1, p.serverLevel().getMinBuildHeight() + 4, spawn.getZ() - 7);
            MatterOverdrive.LOGGER.info("[perf] CHECK storage state {}", p.serverLevel().getBlockState(storage));
        }));
        at(90, mc -> {
            mc.options.hideGui = false;
            MatterOverdrive.LOGGER.info("[perf] DONE");
            mc.stop();
        });
    }

    // --- held items ------------------------------------------------------------------------------

    private static final String[] HAND_ITEMS = {"phaser", "phaser_rifle", "plasma_shotgun", "ion_sniper", "omni_tool", "charging_station",
            "inscriber", "replicator", "pattern_storage", "tritanium_crate_white", "star_map", "weapon_station", "android_station", "transporter",
            "minecraft:iron_sword", "minecraft:furnace"};

    private static void handSteps() {
        at(40, mc -> server(mc, p -> {
            ServerLevel level = p.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            p.setGameMode(GameType.CREATIVE);
            BlockPos spawn = level.getSharedSpawnPos();
            BlockPos base = new BlockPos(spawn.getX() - 100, 150, spawn.getZ());
            for (BlockPos q : BlockPos.betweenClosed(base.offset(-6, -1, -6), base.offset(6, 4, 6))) {
                BlockState state = q.getY() == base.getY() - 1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                setQuietly(level, q, state);
            }
            p.getInventory().clearContent();
            p.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 0f);
        }));
        at(45, mc -> mc.player.getInventory().selected = 0);
        int t = 60;
        for (String id : HAND_ITEMS) {
            String name = id.replace("minecraft:", "vanilla_");
            var key = new net.minecraft.resources.ResourceLocation(id.contains(":") ? id : MatterOverdrive.MODID + ":" + id);
            at(t, mc -> server(mc, p -> {
                ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key));
                p.getInventory().setItem(0, stack);
                p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), Set.of(), 180f, 10f);
            }));
            at(t + 1, mc -> {
                mc.player.getInventory().selected = 0;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
            });
            at(t + 10, mc -> shot(mc, "hand_" + name + "_first"));
            at(t + 11, mc -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.hideGui = true;
            });
            at(t + 16, mc -> shot(mc, "hand_" + name + "_third"));
            t += 18;
        }
        // the inventory icons of the same items (the OBJ machines' gui transforms)
        at(t, mc -> server(mc, p -> {
            for (int i = 0; i < HAND_ITEMS.length && i < 36; i++) {
                var key = new net.minecraft.resources.ResourceLocation(HAND_ITEMS[i].contains(":") ? HAND_ITEMS[i] : MatterOverdrive.MODID + ":" + HAND_ITEMS[i]);
                p.getInventory().setItem(i, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key)));
            }
        }));
        at(t + 4, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        });
        at(t + 14, mc -> shot(mc, "hand_inventory"));
        t += 16;
        at(t, mc -> {
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            MatterOverdrive.LOGGER.info("[perf] DONE");
            mc.stop();
        });
    }

    // --- plumbing --------------------------------------------------------------------------------

    /** After each client tick (both loaders call this) */
    public static void onTick() {
        if (MODE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        if (tick == 0 && STEPS.isEmpty()) initSteps();
        // the window is usually unfocused while this runs: never pause, or the integrated server stops
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
        }
        if (measuring) {
            long avg = (long) (mc.getSingleplayerServer().getAverageTickTime() * 1_000_000);
            tickNanos += avg;
            tickMax = Math.max(tickMax, avg);
            tickSamples++;
        }
        tick++;
        for (Step step : STEPS) {
            if (step.at() == tick) {
                try {
                    step.action().accept(mc);
                } catch (RuntimeException e) {
                    MatterOverdrive.LOGGER.error("[perf] step at tick {} failed", tick, e);
                }
            }
        }
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            try {
                action.accept(server.getPlayerList().getPlayer(mc.player.getUUID()));
            } catch (RuntimeException e) {
                MatterOverdrive.LOGGER.error("[perf] server step failed", e);
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                msg -> MatterOverdrive.LOGGER.info("[perf] {}", msg.getString()));
    }

    /** 1.21.10 placed these with UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS: replaced machines don't drop their contents. */
    private static void setQuietly(ServerLevel level, BlockPos pos, BlockState state) {
        matteroverdrive.compat.CompatBlockEntity.skipRemoveSideEffects = true;
        try {
            // vanilla furnaces would drop their items and the experience of their recipes (orbs that eat frames later)
            if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity) level.removeBlockEntity(pos);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        } finally {
            matteroverdrive.compat.CompatBlockEntity.skipRemoveSideEffects = false;
        }
    }

    private PerfScene() {}
}
