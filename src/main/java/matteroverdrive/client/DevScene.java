package matteroverdrive.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.init.MOItems;
import matteroverdrive.machine.MachineBlock;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.menu.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A scripted test scene for development: {@code ./gradlew runScene} joins the {@code mo_scene} world, builds the
 * machines in front of the player, opens every GUI page and saves screenshots to {@code run/screenshots/scene_*.png},
 * then quits. Inert unless the {@code matteroverdrive.scene} system property is set.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID, value = Dist.CLIENT)
public final class DevScene {
    private static final boolean ENABLED = System.getProperty("matteroverdrive.scene") != null;
    /** Debug: jump from the base build straight to this tick ({@code ./gradlew runScene -PsceneFrom=N}). */
    private static final int FROM = Integer.getInteger("matteroverdrive.scene.from", 0);
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;
    /** Where the player stood when the scene started; restored before quitting so every run builds in one place. */
    private static BlockPos origin;
    private static final java.util.UUID STRANGER = java.util.UUID.fromString("00000000-0000-4000-8000-00000000beef");
    private static BlockPos crashCrate;
    private static matteroverdrive.starmap.GalacticPosition scoutTarget;
    private static BlockPos galleryA, galleryMap, galleryAnomaly;
    private static BlockPos inscriberPos, solarPos, decomposerPos, recyclerPos, analyzerPos, storagePos, monitorPos, replicatorPos, reactorPos;

    private record Step(int at, Consumer<Minecraft> action) {}

    static {
        at(40, mc -> server(mc, DevScene::build));
        at(60, mc -> MatterOverdrive.LOGGER.info("[scene] client matter: iron_block={} cake={}",
                matteroverdrive.matter.MatterRegistry.getClient(Items.IRON_BLOCK), matteroverdrive.matter.MatterRegistry.getClient(Items.CAKE)));
        at(100, mc -> shot(mc, "world"));
        at(105, mc -> openMachine(mc, inscriberPos));
        at(125, mc -> shot(mc, "inscriber_home"));
        at(130, mc -> page(mc, MachineMenu.Page.UPGRADES));
        at(140, mc -> shot(mc, "inscriber_upgrades"));
        at(145, mc -> page(mc, MachineMenu.Page.CONFIG));
        at(155, mc -> shot(mc, "inscriber_config"));
        at(160, mc -> mc.player.closeContainer());
        at(165, mc -> openMachine(mc, solarPos));
        at(185, mc -> shot(mc, "solar_home"));
        at(190, mc -> mc.player.closeContainer());
        at(195, mc -> openMachine(mc, decomposerPos));
        at(215, mc -> shot(mc, "decomposer_home"));
        at(220, mc -> mc.player.closeContainer());
        at(225, mc -> openMachine(mc, recyclerPos));
        at(245, mc -> shot(mc, "recycler_home"));
        at(250, mc -> mc.player.closeContainer());
        at(253, mc -> openMachine(mc, analyzerPos));
        at(275, mc -> shot(mc, "analyzer_home"));
        at(280, mc -> mc.player.closeContainer());
        at(282, mc -> openMachine(mc, storagePos));
        at(300, mc -> shot(mc, "storage_home"));
        at(303, mc -> mc.player.closeContainer());
        at(306, mc -> openMachine(mc, monitorPos));
        at(326, mc -> shot(mc, "monitor_home"));
        at(329, mc -> mc.player.closeContainer());
        at(332, mc -> openMachine(mc, replicatorPos));
        at(352, mc -> shot(mc, "replicator_home"));
        at(355, mc -> mc.player.closeContainer());
        at(358, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 1.5, Set.of(), 140f, 30f, false)));
        at(366, mc -> shot(mc, "pipes"));
        at(368, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 3.5, Set.of(), 180f, 15f, false)));
        at(378, mc -> shot(mc, "network"));
        at(379, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), origin.getX() + 3.5, origin.getY() + 3, origin.getZ() - 7.5, Set.of(), 120f, 5f, false)));
        at(386, mc -> shot(mc, "anomaly"));
        at(387, mc -> openMachine(mc, reactorPos));
        at(400, mc -> shot(mc, "reactor_gui"));
        at(402, mc -> mc.player.closeContainer());
        at(404, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY() + 14, origin.getZ() - 6.5, Set.of(), 180f, 60f, false)));
        at(414, mc -> shot(mc, "reactor"));
        // phase 5: weapons in hand, a bolt in flight, inventory icons
        at(416, mc -> server(mc, p -> {
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
            for (var w : new matteroverdrive.item.weapon.EnergyWeaponItem[] {MOItems.PHASER_RIFLE.get(), MOItems.PLASMA_SHOTGUN.get(), MOItems.ION_SNIPER.get()}) {
                ItemStack s = new ItemStack(w);
                matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(s, 32000);
                p.getInventory().add(s);
            }
            p.getInventory().setItem(0, findWeapon(p, MOItems.PHASER_RIFLE.get()));
            p.getInventory().setSelectedSlot(0);
        }));
        at(418, mc -> mc.player.getInventory().setSelectedSlot(0));   // the selected slot is client-authoritative
        at(424, mc -> shot(mc, "rifle_hand"));
        // a slow bolt so the renderer can be seen (real ones cross the view in a tick or two)
        at(426, mc -> server(mc, p -> {
            for (int i = 0; i < 3; i++) {
                var bolt = new matteroverdrive.entity.PlasmaBolt(p.serverLevel(), p, 0, 64, matteroverdrive.item.weapon.WeaponColorModuleItem.COLORS[i]);
                var look = p.getLookAngle();
                bolt.setPos(p.getX() + (i - 1) * 0.8, p.getEyeY() - 0.2, p.getZ() - 2.5);
                bolt.shoot(look.x, look.y, look.z, 0.005f, 0);
                p.serverLevel().addFreshEntity(bolt);
            }
        }));
        at(428, mc -> shot(mc, "bolt"));
        at(430, mc -> server(mc, p -> p.getInventory().setItem(0, findWeapon(p, MOItems.PLASMA_SHOTGUN.get()))));
        at(436, mc -> shot(mc, "shotgun_hand"));
        at(437, mc -> server(mc, p -> MOItems.PLASMA_SHOTGUN.get().tryFire(p, p.getMainHandItem(), false)));
        at(439, mc -> shot(mc, "shotgun_fire"));
        at(441, mc -> server(mc, p -> p.getInventory().setItem(0, findWeapon(p, MOItems.ION_SNIPER.get()))));
        at(447, mc -> shot(mc, "sniper_hand"));
        at(448, mc -> server(mc, p -> {
            ItemStack phaser = new ItemStack(MOItems.PHASER.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(phaser, 32000);
            phaser.set(matteroverdrive.init.MODataComponents.PHASER_LEVEL.get(), 4);
            p.getInventory().setItem(0, phaser);
            p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), Set.of(), 180f, 25f, false);
        }));
        at(455, mc -> shot(mc, "phaser_hand"));
        // hold the use key: the client releases item use every tick the key is up
        at(456, mc -> mc.options.keyUse.setDown(true));
        at(459, mc -> shot(mc, "phaser_beam"));
        at(460, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        at(462, mc -> shot(mc, "phaser_beam_third"));
        at(463, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.keyUse.setDown(false);
        });
        at(464, mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at(469, mc -> shot(mc, "inventory"));
        at(471, mc -> mc.setScreen(null));
        at(473, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 35f, false)));
        // phase 5c: weapon station with a fully fitted rifle, its GUI, then a phaser (no sights slot) with a barrel
        at(475, mc -> server(mc, p -> {
            BlockPos station = origin.offset(0, 0, -2);
            p.serverLevel().setBlockAndUpdate(station, MOBlocks.WEAPON_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 0, MOItems.HC_BATTERY.get().charged());
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 1, new ItemStack(MOItems.COLOR_MODULES.get(0).get()));
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 2, new ItemStack(MOItems.BARREL_FIRE.get()));
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 3, new ItemStack(MOItems.SNIPER_SCOPE.get()));
            ((matteroverdrive.block.entity.WeaponStationBlockEntity) p.serverLevel().getBlockEntity(station)).getInventory()
                    .setStack(matteroverdrive.block.entity.WeaponStationBlockEntity.WEAPON, rifle);
            p.getInventory().add(new ItemStack(MOItems.BARREL_DAMAGE.get()));
            p.getInventory().add(new ItemStack(MOItems.SNIPER_SCOPE.get()));
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 30f, false);
        }));
        at(484, mc -> shot(mc, "weapon_station"));
        at(485, mc -> openMachine(mc, origin.offset(0, 0, -2)));
        at(492, mc -> shot(mc, "weapon_station_gui"));
        at(493, mc -> server(mc, p -> {
            ItemStack phaser = new ItemStack(MOItems.PHASER.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(phaser, 2, new ItemStack(MOItems.BARREL_EXPLOSION.get()));
            ((matteroverdrive.block.entity.WeaponStationBlockEntity) p.serverLevel().getBlockEntity(origin.offset(0, 0, -2))).getInventory()
                    .setStack(matteroverdrive.block.entity.WeaponStationBlockEntity.WEAPON, phaser);
        }));
        at(498, mc -> shot(mc, "weapon_station_phaser"));
        at(499, mc -> mc.setScreen(null));
        at(500, mc -> server(mc, p -> {
            ItemStack phaser = new ItemStack(MOItems.PHASER.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(phaser, 32000);
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(phaser, 2, new ItemStack(MOItems.BARREL_HEAL.get()));
            p.getInventory().setItem(0, phaser);
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
        }));
        at(508, mc -> shot(mc, "phaser_barrel"));
        // phase 6: become an android, buy a few stats, fit parts, android station GUI
        at(510, mc -> server(mc, p -> {
            BlockPos station = origin.offset(0, 0, -2);
            p.serverLevel().setBlock(station, MOBlocks.ANDROID_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH),
                    Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            matteroverdrive.android.Android.setAndroid(p, true);
            p.setExperienceLevels(200);
            matteroverdrive.android.Android.tryUnlock(p, matteroverdrive.android.BioticStats.NANOBOTS, 1);
            matteroverdrive.android.Android.tryUnlock(p, matteroverdrive.android.BioticStats.ATTACK, 1);
            matteroverdrive.android.Android.tryUnlock(p, matteroverdrive.android.BioticStats.ATTACK, 2);
            matteroverdrive.android.Android.tryUnlock(p, matteroverdrive.android.BioticStats.SPEED, 1);
            matteroverdrive.android.Android.tryUnlock(p, matteroverdrive.android.BioticStats.NIGHT_VISION, 1);
            matteroverdrive.android.Android.get(p).setStack(matteroverdrive.android.AndroidData.SLOT_HEAD, new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get()));
            matteroverdrive.android.Android.get(p).setStack(matteroverdrive.android.AndroidData.SLOT_BATTERY, MOItems.BATTERY.get().charged());
            matteroverdrive.android.Android.sync(p);
            p.getInventory().add(new ItemStack(MOItems.ROGUE_ANDROID_ARMS.get()));
            p.getInventory().add(new ItemStack(MOItems.ANDROID_PILL_RED.get()));
            p.getInventory().add(new ItemStack(MOItems.ANDROID_PILL_BLUE.get()));
            p.getInventory().add(new ItemStack(MOItems.ANDROID_PILL_YELLOW.get()));
            p.setExperienceLevels(40);
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 30f, false);
        }));
        at(516, mc -> shot(mc, "android_station"));
        at(517, mc -> openMachine(mc, origin.offset(0, 0, -2)));
        at(524, mc -> shot(mc, "android_station_gui"));
        at(526, mc -> mc.setScreen(null));
        // 6c/6d: abilities and the android HUD, the ability wheel, the transformation screen
        at(527, mc -> server(mc, p -> {
            for (var stat : new matteroverdrive.android.BioticStat[] {matteroverdrive.android.BioticStats.NANO_ARMOR,
                    matteroverdrive.android.BioticStats.SHIELD, matteroverdrive.android.BioticStats.CLOAK,
                    matteroverdrive.android.BioticStats.MINIMAP, matteroverdrive.android.BioticStats.TELEPORT}) {
                matteroverdrive.android.Android.get(p).getStats().put(stat.id(), stat.maxLevel());
            }
            matteroverdrive.android.Android.get(p).getStats().remove(matteroverdrive.android.BioticStats.ATTACK.id());
            matteroverdrive.android.Android.get(p).setActiveStat(matteroverdrive.android.BioticStats.SHIELD.id());
            matteroverdrive.android.Android.onActionKey(p);
            p.getInventory().setItem(0, findWeapon(p, MOItems.PHASER_RIFLE.get()));
            var pig = net.minecraft.world.entity.EntityType.PIG.create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            pig.setPos(origin.getX() + 3.5, origin.getY(), origin.getZ() - 4.5);
            p.serverLevel().addFreshEntity(pig);
            matteroverdrive.android.Android.sync(p);
        }));
        at(534, mc -> shot(mc, "android_hud"));
        at(535, mc -> mc.setScreen(new matteroverdrive.client.android.AbilityWheelScreen()));
        at(538, mc -> shot(mc, "android_wheel"));
        at(539, mc -> mc.setScreen(null));
        at(540, mc -> server(mc, p -> {
            matteroverdrive.android.Android.setAndroid(p, false);
            matteroverdrive.android.Android.startTransformation(p);
            matteroverdrive.android.Android.setTurning(p, 300);
        }));
        at(550, mc -> shot(mc, "android_transforming"));
        at(551, mc -> server(mc, p -> matteroverdrive.android.Android.setTurning(p, 0)));
        at(552, mc -> server(mc, p -> {
            BlockPos charger = origin.offset(2, 0, -2);
            var state = MOBlocks.CHARGING_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
            p.serverLevel().setBlockAndUpdate(charger, state);
            state.getBlock().setPlacedBy(p.serverLevel(), charger, state, p, ItemStack.EMPTY);
        }));
        at(558, mc -> shot(mc, "charging_station"));
        // 7a: every decorative block in a wall, tritanium glass, a holo sign with text
        at(560, mc -> server(mc, p -> {
            var blocks = matteroverdrive.init.MODecorative.ALL;
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos at = origin.offset(-9 + i % 19, i / 19, 3);
                p.serverLevel().setBlockAndUpdate(at, blocks.get(i).get().defaultBlockState());
            }
            BlockPos wall = origin.offset(0, 2, 3);
            p.serverLevel().setBlockAndUpdate(wall, matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
            BlockPos sign = wall.north();
            p.serverLevel().setBlockAndUpdate(sign, MOBlocks.HOLO_SIGN.get().defaultBlockState()
                    .setValue(matteroverdrive.block.HoloSignBlock.FACING, Direction.NORTH));
            if (p.serverLevel().getBlockEntity(sign) instanceof matteroverdrive.block.entity.HoloSignBlockEntity holo) {
                holo.setText("Matter\nOverdrive\n1.21.10");
            }
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 4.5, Set.of(), 0f, 10f, false);
        }));
        at(568, mc -> shot(mc, "decorative"));
        // 7b: rogue androids of each level (one legendary), melee and ranged
        at(570, mc -> server(mc, p -> {
            int[][] spec = {{0, 0}, {1, 0}, {2, 0}, {3, 1}};
            for (int i = 0; i < 4; i++) {
                for (boolean ranged : new boolean[] {false, true}) {
                    var type = ranged ? matteroverdrive.init.MOEntities.RANGED_ROGUE_ANDROID.get() : matteroverdrive.init.MOEntities.ROGUE_ANDROID.get();
                    var android = type.create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    android.snapTo(origin.getX() - 4.5 + i * 3 + (ranged ? 1.2 : 0), origin.getY(), origin.getZ() - 8.5, 0, 0);
                    android.finalizeSpawn(p.serverLevel(), p.serverLevel().getCurrentDifficultyAt(android.blockPosition()),
                            net.minecraft.world.entity.EntitySpawnReason.COMMAND, null);
                    android.setup(spec[i][0], spec[i][1] == 1);
                    android.setNoAi(true);
                    android.setYRot(0);
                    android.setYHeadRot(0);
                    android.yBodyRot = 0;
                    p.serverLevel().addFreshEntity(android);
                }
            }
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 1.5, Set.of(), 180f, 10f, false);
        }));
        at(578, mc -> shot(mc, "rogue_androids"));
        // 7c: failed animals (adults and a piglet) on a platform above the scene
        at(580, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            for (int x = -7; x <= 7; x++) {
                for (int z = -10; z <= 1; z++) {
                    p.serverLevel().setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 0; y < 4; y++) p.serverLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
            var types = java.util.List.of(matteroverdrive.init.MOEntities.FAILED_PIG.get(), matteroverdrive.init.MOEntities.FAILED_COW.get(),
                    matteroverdrive.init.MOEntities.FAILED_CHICKEN.get(), matteroverdrive.init.MOEntities.FAILED_SHEEP.get(),
                    matteroverdrive.init.MOEntities.FAILED_PIG.get());
            for (int i = 0; i < types.size(); i++) {
                var animal = types.get(i).create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                animal.snapTo(base.getX() - 4.5 + i * 2.4, base.getY(), base.getZ() - 6.5, 0, 0);
                if (i == 4) animal.setAge(-24000);
                animal.setNoAi(true);
                animal.setYRot(-40);
                animal.setYHeadRot(-40);
                animal.yBodyRot = -40;
                p.serverLevel().addFreshEntity(animal);
            }
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 20f, false);
        }));
        at(588, mc -> shot(mc, "failed_animals"));
        // 7d: the mutant scientist on the same platform
        at(590, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, p.getBoundingBox().inflate(16)).forEach(e -> e.discard());
            var mutant = matteroverdrive.init.MOEntities.MUTANT_SCIENTIST.get().create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            mutant.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 4.5, 0, 0);
            mutant.setNoAi(true);
            mutant.setYRot(25);
            mutant.setYHeadRot(25);
            mutant.yBodyRot = 25;
            p.serverLevel().addFreshEntity(mutant);
        }));
        at(598, mc -> shot(mc, "mutant_scientist"));
        // 7e: the 16 tritanium crates in two rows, facing the camera
        at(600, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            p.serverLevel().getEntitiesOfClass(matteroverdrive.entity.monster.MutantScientist.class, p.getBoundingBox().inflate(16)).forEach(e -> e.discard());
            for (int i = 0; i < 16; i++) {
                BlockPos pos = base.offset(-4 + i % 8, i < 8 ? 0 : 1, -5 - (i < 8 ? 0 : 1));
                if (i >= 8) p.serverLevel().setBlockAndUpdate(pos.below(), matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
                p.serverLevel().setBlockAndUpdate(pos, matteroverdrive.init.MOBlocks.TRITANIUM_CRATES.get(i).get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 25f, false);
        }));
        at(608, mc -> shot(mc, "tritanium_crates"));
        // 7f: every image building placed on the terrain far east of the scene, seen from above a corner
        var buildings = matteroverdrive.world.Building.values();
        for (int i = 0; i < buildings.length; i++) {
            var building = buildings[i];
            int t0 = 610 + i * 80;
            at(t0, mc -> server(mc, p -> {
                BlockPos site = buildingSite(building);
                p.teleportTo(p.serverLevel(), site.getX(), 200, site.getZ(), Set.of(), 0f, 0f, false);
            }));
            at(t0 + 60, mc -> server(mc, p -> {
                var t = building.template();
                BlockPos site = buildingSite(building);
                int y = p.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.getX(), site.getZ()) - 3;
                BlockPos at = new BlockPos(site.getX(), y, site.getZ());
                var piece = new matteroverdrive.world.ImageStructurePiece(building, at, 42);
                piece.postProcess(p.serverLevel(), p.serverLevel().structureManager(), p.serverLevel().getChunkSource().getGenerator(), p.serverLevel().getRandom(),
                        piece.getBoundingBox(), new net.minecraft.world.level.ChunkPos(at), at);
                double cx = at.getX() + t.width() / 2.0, cy = at.getY() + t.height() / 2.0, cz = at.getZ() + t.depth() / 2.0;
                double size = Math.max(t.width(), t.depth());
                double ex = cx - size * 0.75, ey = cy + size * 0.55 + 4, ez = cz - size * 0.75;
                float yaw = (float) Math.toDegrees(Math.atan2(-(cx - ex), cz - ez));
                float pitch = (float) Math.toDegrees(Math.atan2(ey - cy, Math.hypot(cx - ex, cz - ez)));
                p.teleportTo(p.serverLevel(), ex, ey, ez, Set.of(), yaw, pitch, false);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            }));
            at(t0 + 61, mc -> mc.options.hideGui = true);
            at(t0 + 78, mc -> shot(mc, "building_" + building.getSerializedName()));
            at(t0 + 79, mc -> mc.options.hideGui = false);
        }
        // and where the world generator would put them
        at(1012, mc -> server(mc, p -> {
            for (var building : buildings) {
                p.serverLevel().getServer().getCommands().performPrefixedCommand(p.serverLevel().getServer().createCommandSourceStack(),
                        "locate structure matteroverdrive:" + building.getSerializedName());
            }
        }));
        // a naturally generated android house and crashed ship: fly to the nearest ones (fresh chunks), report what is there
        String[] natural = {"android_house", "crashed_ship"};
        // somewhere new every run, so the chunks there are generated now
        BlockPos[] found0 = new BlockPos[1];
        BlockPos searchFrom = new BlockPos(new java.util.Random().nextInt(40000) + 10000, 100, new java.util.Random().nextInt(40000) + 10000);
        for (int i = 0; i < natural.length; i++) {
            String id = natural[i];
            boolean first = i == 0;
            int t0 = 1020 + i * 160;
            BlockPos[] found = i == 0 ? found0 : new BlockPos[1];
            at(t0, mc -> server(mc, p -> {
                var registry = p.serverLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                var holder = registry.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("matteroverdrive", id)));
                var result = p.serverLevel().getChunkSource().getGenerator().findNearestMapStructure(p.serverLevel(),
                        net.minecraft.core.HolderSet.direct(holder), first ? searchFrom : found0[0], 100, false);
                if (result == null) return;
                found[0] = result.getFirst();
                p.teleportTo(p.serverLevel(), found[0].getX(), 160, found[0].getZ(), Set.of(), 0f, 0f, false);
            }));
            at(t0 + 120, mc -> server(mc, p -> {
                if (found[0] == null) return;
                var holder = p.serverLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                        .getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("matteroverdrive", id));
                var start = p.serverLevel().structureManager().getStructureAt(
                        new BlockPos(found[0].getX(), p.serverLevel().getMinY() + 1, found[0].getZ()), holder);
                BlockPos lookup = found[0];
                if (!start.isValid()) {
                    for (var s2 : p.serverLevel().structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(lookup), st -> st == holder)) {
                        start = s2;
                    }
                }
                if (!start.isValid()) {
                    MatterOverdrive.LOGGER.info("[scene] {} near {}: no start", id, lookup);
                    return;
                }
                var box = start.getBoundingBox();
                var aabb = AABB.of(box).inflate(4);
                int androids = p.serverLevel().getEntitiesOfClass(matteroverdrive.entity.monster.RogueAndroid.class, aabb).size();
                int mutants = p.serverLevel().getEntitiesOfClass(matteroverdrive.entity.monster.MutantScientist.class, aabb).size();
                int crates = 0, lootCrates = 0;
                for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                    if (p.serverLevel().getBlockEntity(pos) instanceof matteroverdrive.block.entity.TritaniumCrateBlockEntity crate) {
                        crates++;
                        if (crate.getLootTable() != null || !crate.isEmpty()) lootCrates++;
                    }
                }
                MatterOverdrive.LOGGER.info("[scene] {} at {}: androids {}, mutants {}, crates {} (with loot {})", id, box, androids, mutants, crates, lootCrates);
                double cx = box.getCenter().getX(), cy = box.minY() + 3, cz = box.getCenter().getZ();
                double size = Math.max(box.getXSpan(), box.getZSpan());
                double ex = cx - size * 0.8, ey = cy + size * 0.6 + 4, ez = cz - size * 0.8;
                float yaw = (float) Math.toDegrees(Math.atan2(-(cx - ex), cz - ez));
                float pitch = (float) Math.toDegrees(Math.atan2(ey - cy, Math.hypot(cx - ex, cz - ez)));
                p.teleportTo(p.serverLevel(), ex, ey, ez, Set.of(), yaw, pitch, false);
            }));
            at(t0 + 121, mc -> mc.options.hideGui = true);
            at(t0 + 150, mc -> shot(mc, "natural_" + id));
            at(t0 + 151, mc -> mc.options.hideGui = false);
        }
        // 7g: a Matter Plasma pool on the platform, the containers in the hotbar
        at(1340, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 35f, false);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-4, 0, -9), base.offset(4, 1, -4))) {
                p.serverLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, -1, -7), base.offset(2, -1, -5))) {
                p.serverLevel().setBlockAndUpdate(pos, matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
                p.serverLevel().setBlockAndUpdate(pos.below(), matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
                p.serverLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.serverLevel().setBlockAndUpdate(base.offset(0, -1, -6), matteroverdrive.init.MOBlocks.MATTER_PLASMA.get().defaultBlockState());
            p.serverLevel().setBlockAndUpdate(base.offset(-1, -1, -6), matteroverdrive.init.MOBlocks.MATTER_PLASMA.get().defaultBlockState());
            p.getInventory().setItem(0, new ItemStack(MOItems.MATTER_CONTAINER.get(), 8));
            p.getInventory().setItem(1, new ItemStack(MOItems.MATTER_CONTAINER_FULL.get(), 3));
        }));
        at(1400, mc -> shot(mc, "matter_plasma"));
        // 7h: the omni tool digging a wall 6 blocks away
        at(1402, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, 0, -7), base.offset(2, 2, -7))) {
                p.serverLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            ItemStack tool = new ItemStack(MOItems.OMNI_TOOL.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(tool, 32000);
            p.getInventory().setItem(0, tool);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 0f, false);
        }));
        at(1410, mc -> mc.options.keyUse.setDown(true));
        at(1428, mc -> shot(mc, "omni_tool"));
        at(1429, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        at(1436, mc -> shot(mc, "omni_tool_third_person"));
        at(1437, mc -> {
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        });
        // 7i: microwaves facing the camera and sideways, the portable decomposer in the hotbar
        at(1440, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, 0, -7), base.offset(2, 2, -7))) {
                p.serverLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.serverLevel().setBlockAndUpdate(base.offset(-1, 0, -4), MOBlocks.MICROWAVE.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            p.serverLevel().setBlockAndUpdate(base.offset(1, 0, -4), MOBlocks.MICROWAVE.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
            ItemStack decomposer = new ItemStack(MOItems.PORTABLE_DECOMPOSER.get());
            decomposer.set(matteroverdrive.init.MODataComponents.ENERGY.get(), 90000);
            p.getInventory().setItem(0, decomposer);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 40f, false);
        }));
        at(1450, mc -> shot(mc, "microwave"));
        // 7j: a matter scanner linked to a pattern storage with a few patterns, its screen open
        at(1452, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            BlockPos storagePos = base.offset(2, 0, -5);
            p.serverLevel().setBlockAndUpdate(storagePos, MOBlocks.PATTERN_STORAGE.get().defaultBlockState());
            if (p.serverLevel().getBlockEntity(storagePos) instanceof matteroverdrive.block.entity.PatternStorageBlockEntity storage) {
                storage.getEnergy().set(64000);
                ItemStack drive = new ItemStack(MOItems.PATTERN_DRIVE.get());
                MOItems.PATTERN_DRIVE.get().addProgress(drive, net.minecraft.world.item.Items.DIAMOND, 100);
                MOItems.PATTERN_DRIVE.get().addProgress(drive, net.minecraft.world.item.Items.IRON_BLOCK, 40);
                ItemStack drive2 = new ItemStack(MOItems.PATTERN_DRIVE.get());
                MOItems.PATTERN_DRIVE.get().addProgress(drive2, net.minecraft.world.item.Items.GOLD_INGOT, 70);
                MOItems.PATTERN_DRIVE.get().addProgress(drive2, net.minecraft.world.item.Items.COBBLESTONE, 10);
                storage.getInventory().setStack(0, drive);
                storage.getInventory().setStack(1, drive2);
            }
            ItemStack scanner = new ItemStack(MOItems.MATTER_SCANNER.get());
            matteroverdrive.item.MatterScannerItem.link(scanner, p.serverLevel(), storagePos);
            matteroverdrive.item.MatterScannerItem.select(p.serverLevel(), scanner, net.minecraft.world.item.Items.IRON_BLOCK);
            p.getInventory().setItem(0, scanner);
        }));
        at(1460, mc -> mc.setScreen(new matteroverdrive.client.screen.MatterScannerScreen(0)));
        at(1470, mc -> shot(mc, "matter_scanner"));
        at(1471, mc -> mc.setScreen(null));
        // 7k: a transporter with two destinations: its screen, then the player on the pad mid-transport
        at(1474, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            BlockPos pad = base.offset(0, -1, -4);
            // a clean floor (the plasma pool and the microwaves go)
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-7, -2, -10), base.offset(7, 3, 1))) {
                p.serverLevel().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.serverLevel().setBlockAndUpdate(pad, MOBlocks.TRANSPORTER.get().defaultBlockState());
            if (p.serverLevel().getBlockEntity(pad) instanceof matteroverdrive.block.entity.TransporterBlockEntity t) {
                t.getEnergy().set(1000000);
                t.addLocation("Platform edge");
                t.setSelected("Platform edge", base.offset(-5, 0, -8));
                t.addLocation("Roof");
                t.setSelected("Roof", base.offset(3, 6, -4));
                t.select(0);
                ItemStack drive = new ItemStack(MOItems.TRANSPORT_FLASH_DRIVE.get());
                drive.set(matteroverdrive.init.MODataComponents.TRANSPORT_TARGET.get(), base.offset(4, -1, -9));
                t.getInventory().setStack(matteroverdrive.block.entity.TransporterBlockEntity.FLASH_DRIVE, drive);
            }
        }));
        // open the screen once the client has the new block entity
        at(1478, mc -> server(mc, p -> {
            BlockPos pad = origin.above(14).offset(0, -1, -4);
            if (p.serverLevel().getBlockEntity(pad) instanceof matteroverdrive.block.entity.TransporterBlockEntity t) {
                p.openMenu(t, buf -> buf.writeBlockPos(pad));
            }
        }));
        at(1484, mc -> shot(mc, "transporter_gui"));   // before 1.21.9 the screen closes right after the 1485 step
        at(1485, mc -> server(mc, p -> {
            p.closeContainer();
            BlockPos base = origin.above(14);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 3.5, Set.of(), 180f, 30f, false);
        }));
        at(1488, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        at(1540, mc -> shot(mc, "transporter_transport"));
        at(1541, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        // 7l: an android spawner on a team with a red colour module and a path; its screen (home + config), then the androids
        at(1544, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -1, -14), base.offset(8, 5, 2))) {
                p.serverLevel().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
            var scoreboard = p.serverLevel().getScoreboard();
            if (scoreboard.getPlayerTeam("rogues") == null) scoreboard.addPlayerTeam("rogues").setColor(net.minecraft.ChatFormatting.RED);
            BlockPos spawnerPos = base.offset(-2, -1, -5);
            p.serverLevel().setBlockAndUpdate(spawnerPos, MOBlocks.ANDROID_SPAWNER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (p.serverLevel().getBlockEntity(spawnerPos) instanceof matteroverdrive.block.entity.AndroidSpawnerBlockEntity s) {
                s.getInventory().setStack(matteroverdrive.block.entity.AndroidSpawnerBlockEntity.COLOR_MODULE_SLOT,
                        new ItemStack(MOItems.COLOR_MODULES.get(0).get()));
                ItemStack drive = new ItemStack(MOItems.TRANSPORT_FLASH_DRIVE.get());
                drive.set(matteroverdrive.init.MODataComponents.TRANSPORT_TARGET.get(), base.offset(3, -1, -6));
                s.getInventory().setStack(matteroverdrive.block.entity.AndroidSpawnerBlockEntity.FLASH_DRIVE_SLOT_START, drive);
                s.setConfig(4, 4, 0, "rogues");
            }
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 25f, false);
        }));
        at(1560, mc -> server(mc, p -> {
            BlockPos spawnerPos = origin.above(30).offset(-2, -1, -5);
            if (p.serverLevel().getBlockEntity(spawnerPos) instanceof matteroverdrive.block.entity.AndroidSpawnerBlockEntity s) {
                s.setConfig(4, 4, 600, "rogues");
                p.openMenu(s, buf -> buf.writeBlockPos(spawnerPos));
            }
        }));
        at(1570, mc -> shot(mc, "android_spawner_home"));
        at(1571, mc -> page(mc, MachineMenu.Page.CONFIG));
        at(1576, mc -> shot(mc, "android_spawner_config"));
        at(1577, mc -> server(mc, ServerPlayer::closeContainer));
        at(1630, mc -> shot(mc, "android_spawner_world"));
        at(1631, mc -> server(mc, p -> {
            BlockPos spawnerPos = origin.above(30).offset(-2, -1, -5);
            if (p.serverLevel().getBlockEntity(spawnerPos) instanceof matteroverdrive.block.entity.AndroidSpawnerBlockEntity s) s.removeAllAndroids();
        }));
        // 7m: the mad scientist: a human's conversation, taking Puny Humans (HUD "Started"), the junkie's cocktail story, trades
        at(1636, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -1, -14), base.offset(8, 5, 2))) {
                p.serverLevel().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
            matteroverdrive.quest.PlayerQuests.get(p).getActiveQuests().clear();
            matteroverdrive.quest.PlayerQuests.get(p).getCompletedQuests().clear();
            matteroverdrive.quest.PlayerQuests.sync(p);
            matteroverdrive.android.Android.setAndroid(p, false);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 5f, false);
            var npc = matteroverdrive.init.MOEntities.MAD_SCIENTIST.get().create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            npc.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 2.5, 0, 0);
            npc.setJunkie(false);
            npc.setNoAi(true);
            p.serverLevel().addFreshEntity(npc);
        }));
        at(1644, mc -> server(mc, p -> p.serverLevel().getEntitiesOfClass(matteroverdrive.entity.MadScientist.class, p.getBoundingBox().inflate(6))
                .forEach(npc -> matteroverdrive.dialog.DialogPayloads.startConversation(p, npc))));
        at(1652, mc -> shot(mc, "dialog_human"));
        at(1653, mc -> {
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen d) d.choose(0);
        });
        at(1680, mc -> shot(mc, "dialog_puny_humans"));
        at(1681, mc -> mc.setScreen(null));
        at(1682, mc -> server(mc, p -> p.serverLevel().getEntitiesOfClass(matteroverdrive.entity.MadScientist.class, p.getBoundingBox().inflate(6))
                .forEach(npc -> npc.setJunkie(true))));
        // the junkie flag reaches the client first: both sides build the same start message
        at(1686, mc -> server(mc, p -> p.serverLevel().getEntitiesOfClass(matteroverdrive.entity.MadScientist.class, p.getBoundingBox().inflate(6))
                .forEach(npc -> matteroverdrive.dialog.DialogPayloads.startConversation(p, npc))));
        at(1692, mc -> shot(mc, "dialog_junkie"));
        // the cocktail story: its first line, then on to the last line with Accept / Decline
        at(1693, mc -> {
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen d) {
                d.choose(d.getCurrent().getOptions(d.getNpc(), mc.player).indexOf(matteroverdrive.entity.MadScientist.Dialogs.cocktailOfAscension));
            }
        });
        at(1720, mc -> shot(mc, "dialog_cocktail"));
        at(1721, mc -> {
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen d) {
                var lines = matteroverdrive.entity.MadScientist.Dialogs.cocktailQuest;
                d.setCurrent(lines[lines.length - 1]);
            }
        });
        at(1726, mc -> shot(mc, "dialog_cocktail_accept"));
        at(1727, mc -> {
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen d) {
                d.setCurrent(d.getNpc().getStartDialogMessage(mc.player));
            }
        });
        at(1730, mc -> {
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen d) {
                d.choose(d.getCurrent().getOptions(d.getNpc(), mc.player).indexOf(matteroverdrive.entity.MadScientist.Dialogs.trade));
            }
        });
        at(1745, mc -> shot(mc, "mad_scientist_trades"));
        at(1746, mc -> mc.setScreen(null));
        at(1750, mc -> mc.options.hideGui = true);
        at(1760, mc -> shot(mc, "mad_scientist"));
        at(1761, mc -> mc.options.hideGui = false);
        // 7n: plains villages placed far east until one has the mad scientist's house; a view from above and one inside
        for (int i = 0; i < 8; i++) {
            int n = i;
            // the player goes first so the village's chunks (and its entities) are loaded, then the village is placed
            at(1766 + i * 30, mc -> server(mc, p -> {
                if (!p.serverLevel().getEntities(matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), e -> e.getX() > origin.getX() + 1000).isEmpty()) return;
                BlockPos at = origin.offset(2000 + n * 300, 0, 0);
                p.teleportTo(p.serverLevel(), at.getX(), at.getY() + 40, at.getZ(), Set.of(), 0, 90f, false);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            }));
            // /place needs every chunk of the village loaded: force-load 15 x 15 chunks around it first
            at(1770 + i * 30, mc -> server(mc, p -> {
                if (!p.serverLevel().getEntities(matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), e -> e.getX() > origin.getX() + 1000).isEmpty()) return;
                BlockPos at = origin.offset(2000 + n * 300, 0, 0);
                p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                        "forceload add " + (at.getX() - 112) + " " + (at.getZ() - 112) + " " + (at.getX() + 112) + " " + (at.getZ() + 112));
            }));
            at(1786 + i * 30, mc -> server(mc, p -> {
                if (!p.serverLevel().getEntities(matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), e -> e.getX() > origin.getX() + 1000).isEmpty()) return;
                BlockPos at = origin.offset(2000 + n * 300, 0, 0);
                p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4)
                        .withCallback((ok, result) -> MatterOverdrive.LOGGER.info("[scene] village at {}: {}", at, ok)),
                        "place structure minecraft:village_plains " + at.getX() + " " + at.getY() + " " + at.getZ());
                MatterOverdrive.LOGGER.info("[scene] scientists loaded: {}", p.serverLevel().getEntities(
                        matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), e -> e.getX() > origin.getX() + 1000).size());
            }));
        }
        at(2010, mc -> server(mc, p -> {
            var found = p.serverLevel().getEntities(matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), e -> e.getX() > origin.getX() + 1000);
            MatterOverdrive.LOGGER.info("[scene] mad scientists in villages: {}", found.size());
            if (found.isEmpty()) return;
            var npc = found.get(0);
            npc.setNoAi(true);
            p.teleportTo(p.serverLevel(), npc.getX() + 6, npc.getY() + 14, npc.getZ() + 6, Set.of(), 135f, 55f, false);
        }));
        at(2011, mc -> mc.options.hideGui = true);
        at(2050, mc -> shot(mc, "mad_scientist_house"));
        at(2051, mc -> server(mc, p -> {
            var found = p.serverLevel().getEntities(matteroverdrive.init.MOEntities.MAD_SCIENTIST.get(), e -> e.getX() > origin.getX() + 1000);
            if (found.isEmpty()) return;
            var npc = found.get(0);
            // inside: from the scientist's spot, looking around the room towards the bookshelves
            BlockPos crate = BlockPos.betweenClosedStream(npc.blockPosition().offset(-4, -1, -4), npc.blockPosition().offset(4, 1, 4))
                    .filter(b -> p.serverLevel().getBlockState(b).getBlock() instanceof matteroverdrive.block.TritaniumCrateBlock)
                    .map(BlockPos::immutable).findFirst().orElse(npc.blockPosition());
            var eye = npc.position().add(npc.position().subtract(crate.getCenter()).normalize().scale(2.5));
            p.teleportTo(p.serverLevel(), eye.x, npc.getY(), eye.z, Set.of(), 0, 15f, false);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, crate.getCenter());
        }));
        at(2070, mc -> shot(mc, "mad_scientist_house_inside"));
        at(2071, mc -> mc.options.hideGui = false);
        at(2072, mc -> server(mc, p -> {
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "forceload remove all");
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
        }));
        // 7o: the Data Pad's quest log with two quests (the cocktail half done), then the other one selected
        at(2076, mc -> server(mc, p -> {
            var quests = matteroverdrive.quest.PlayerQuests.get(p);
            quests.getActiveQuests().clear();
            quests.getCompletedQuests().clear();
            var cocktail = new matteroverdrive.quest.QuestStack(matteroverdrive.quest.Quests.COCKTAIL_OF_ASCENSION);
            cocktail.getData().putByte("CreeperKills", (byte) 3);
            cocktail.getData().putByte("GunpowderCount", (byte) 5);
            quests.getActiveQuests().add(cocktail);
            quests.getActiveQuests().add(new matteroverdrive.quest.QuestStack(matteroverdrive.quest.Quests.PUNY_HUMANS));
            matteroverdrive.quest.PlayerQuests.sync(p);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(MOItems.DATA_PAD.get()));
        }));
        // close first: the closing screen sends its own state, which would overwrite the next one
        at(2082, mc -> mc.setScreen(null));
        at(2088, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT)));
        at(2094, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2102, mc -> shot(mc, "data_pad_guide"));
        at(2106, mc -> mc.setScreen(null));
        at(2112, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withOrdering(0))));
        at(2118, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2126, mc -> shot(mc, "data_pad_guide_list"));
        at(2130, mc -> mc.setScreen(null));
        at(2136, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(1).withGuide("tile.decomposer", 0))));
        at(2142, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2150, mc -> shot(mc, "data_pad_guide_decomposer"));
        at(2154, mc -> mc.setScreen(null));
        at(2160, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(1).withGuide("tile.decomposer", 1))));
        at(2166, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2174, mc -> shot(mc, "data_pad_guide_decomposer_2"));
        at(2178, mc -> mc.setScreen(null));
        at(2184, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(1).withGuide("tile.decomposer", 2))));
        at(2190, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2198, mc -> shot(mc, "data_pad_guide_recipe"));
        at(2202, mc -> mc.setScreen(null));
        at(2208, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(1).withGuide("tile.dilithium_ore", 0))));
        at(2214, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2222, mc -> shot(mc, "data_pad_guide_ore"));
        at(2226, mc -> mc.setScreen(null));
        at(2232, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withCategory("weapons"))));
        at(2238, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2246, mc -> shot(mc, "data_pad_guide_weapons"));
        at(2250, mc -> mc.setScreen(null));
        at(2256, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(2))));
        at(2262, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2270, mc -> shot(mc, "data_pad_quests"));
        at(2274, mc -> mc.setScreen(null));
        at(2280, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(2).withQuest(1, 0))));
        at(2286, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2294, mc -> shot(mc, "data_pad_quests_2"));
        at(2298, mc -> mc.setScreen(null));
        // 7q: a contract market with a few contracts, its screen, a contract's preview screen and the block in the world
        at(2302, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -1, -14), base.offset(8, 5, 2))) {
                p.serverLevel().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
            BlockPos market = base.offset(0, 0, -3);
            p.serverLevel().setBlockAndUpdate(market, matteroverdrive.init.MOBlocks.CONTRACT_MARKET.get().defaultBlockState()
                    .setValue(matteroverdrive.machine.MachineBlock.FACING, net.minecraft.core.Direction.SOUTH));
            if (p.serverLevel().getBlockEntity(market) instanceof matteroverdrive.block.entity.ContractMarketBlockEntity m) {
                var quests = List.of(matteroverdrive.quest.Quests.KILL_ANDROIDS, matteroverdrive.quest.Quests.SACRIFICE,
                        matteroverdrive.quest.Quests.DEPARTMENT_OF_AGRICULTURE, matteroverdrive.quest.Quests.WEAPONS_OF_WAR,
                        matteroverdrive.quest.Quests.ONE_TRUE_LOVE, matteroverdrive.quest.Quests.IS_IT_REALLY_ME,
                        matteroverdrive.quest.Quests.BEAST_BELLY);
                for (int i = 0; i < quests.size(); i++) {
                    m.getInventory().setStack(i, matteroverdrive.item.ContractItem.of(quests.get(i).generate(p.serverLevel().random)));
                }
                m.addGenerationDelay();
            }
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 20f, false);
        }));
        at(2310, mc -> server(mc, p -> {
            BlockPos market = origin.above(30).offset(0, 0, -3);
            if (p.serverLevel().getBlockEntity(market) instanceof matteroverdrive.block.entity.ContractMarketBlockEntity m) {
                p.openMenu(m, buf -> buf.writeBlockPos(market));
            }
        }));
        at(2320, mc -> shot(mc, "contract_market"));
        at(2321, mc -> server(mc, ServerPlayer::closeContainer));
        at(2324, mc -> server(mc, p -> p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                matteroverdrive.item.ContractItem.of(matteroverdrive.quest.Quests.KILL_ANDROIDS.generate(p.serverLevel().random)))));
        at(2330, mc -> mc.setScreen(new matteroverdrive.client.quest.ContractScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2338, mc -> shot(mc, "contract_screen"));
        at(2339, mc -> mc.setScreen(null));
        at(2342, mc -> server(mc, p -> p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                matteroverdrive.item.ContractItem.of(matteroverdrive.quest.Quests.GMO.generate(p.serverLevel().random)))));
        at(2348, mc -> mc.setScreen(new matteroverdrive.client.quest.ContractScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2356, mc -> shot(mc, "contract_screen_gmo"));
        at(2357, mc -> mc.setScreen(null));
        at(2362, mc -> shot(mc, "contract_market_world"));
        // 7r: the security protocol types, a machine claimed by someone else, crash landing -> we must know, a crashed ship's crate
        at(2370, mc -> server(mc, p -> {
            var quests = matteroverdrive.quest.PlayerQuests.get(p);
            quests.getActiveQuests().clear();
            quests.getCompletedQuests().clear();
            matteroverdrive.quest.PlayerQuests.sync(p);
            for (int i = 0; i < 4; i++) {
                ItemStack protocol = new ItemStack(MOItems.SECURITY_PROTOCOL.get(), 16);
                if (i > 0) {
                    protocol.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), p.getUUID());
                    protocol.set(matteroverdrive.init.MODataComponents.SECURITY_TYPE.get(), i);
                }
                p.getInventory().setItem(i, protocol);
            }
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, p.getInventory().getItem(1).copy());
        }));
        at(2378, mc -> shot(mc, "security_protocol_hand"));
        at(2380, mc -> server(mc, p -> {
            BlockPos market = origin.above(30).offset(0, 0, -3);
            if (p.serverLevel().getBlockEntity(market) instanceof matteroverdrive.machine.MachineBlockEntity m) {
                ItemStack foreign = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
                foreign.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), STRANGER);
                m.claim(foreign);
            }
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.serverLevel().getBlockState(market).useWithoutItem(p.serverLevel(), p,
                    new net.minecraft.world.phys.BlockHitResult(market.getCenter(), net.minecraft.core.Direction.SOUTH, market, false));
        }));
        at(2388, mc -> shot(mc, "security_no_rights"));
        at(2390, mc -> server(mc, p -> {
            BlockPos market = origin.above(30).offset(0, 0, -3);
            if (p.serverLevel().getBlockEntity(market) instanceof matteroverdrive.machine.MachineBlockEntity m) {
                ItemStack remove = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
                remove.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), STRANGER);
                m.unclaim(remove);
            }
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            BlockPos crate = origin.above(30).offset(6, 0, -6);
            var contract = matteroverdrive.quest.Quests.CRASH_LANDING.generate(p.serverLevel().random);
            contract.getData().putIntArray("Pos", new int[] {crate.getX(), crate.getY(), crate.getZ()});
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, matteroverdrive.item.ContractItem.of(contract));
        }));
        at(2396, mc -> mc.setScreen(new matteroverdrive.client.quest.ContractScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2404, mc -> shot(mc, "contract_crash_landing"));
        at(2405, mc -> mc.setScreen(null));
        at(2408, mc -> server(mc, p -> {
            var contract = matteroverdrive.item.ContractItem.getQuest(p.getMainHandItem());
            if (contract == null) return;
            matteroverdrive.quest.QuestEvents.addQuest(p, contract.copy());
            matteroverdrive.quest.QuestEvents.onEvent(p, new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p,
                    new ItemStack(MOItems.SECURITY_PROTOCOL.get()), new net.minecraft.world.SimpleContainer(1)));
            matteroverdrive.quest.QuestEvents.manageQuestCompletion(p);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }));
        at(2416, mc -> shot(mc, "crash_landing_completed"));
        at(2420, mc -> server(mc, p -> p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(MOItems.DATA_PAD.get()))));
        at(2426, mc -> server(mc, p -> p.getMainHandItem().set(matteroverdrive.init.MODataComponents.DATA_PAD.get(),
                matteroverdrive.item.DataPadItem.State.DEFAULT.withPage(2))));
        at(2432, mc -> mc.setScreen(new matteroverdrive.client.quest.DataPadScreen(net.minecraft.world.InteractionHand.MAIN_HAND)));
        at(2440, mc -> shot(mc, "we_must_know_pad"));
        at(2442, mc -> mc.setScreen(null));
        // a crashed ship far south: player first (loads the chunks), force-load, place, then look at it and open a crate
        at(2446, mc -> server(mc, p -> {
            BlockPos at = origin.offset(0, 0, 3000);
            p.teleportTo(p.serverLevel(), at.getX(), at.getY() + 40, at.getZ(), Set.of(), 0, 90f, false);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }));
        at(2450, mc -> server(mc, p -> {
            BlockPos at = origin.offset(0, 0, 3000);
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "forceload add " + (at.getX() - 64) + " " + (at.getZ() - 64) + " " + (at.getX() + 64) + " " + (at.getZ() + 64));
        }));
        at(2466, mc -> server(mc, p -> {
            BlockPos at = origin.offset(0, 0, 3000);
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4)
                    .withCallback((ok, result) -> MatterOverdrive.LOGGER.info("[scene] crashed ship at {}: {}", at, ok)),
                    "place structure matteroverdrive:crashed_ship " + at.getX() + " " + at.getY() + " " + at.getZ());
        }));
        at(2472, mc -> server(mc, p -> {
            BlockPos at = origin.offset(0, 0, 3000);
            BlockPos crate = BlockPos.betweenClosedStream(new BlockPos(at.getX() - 48, p.serverLevel().getMinY(), at.getZ() - 48), new BlockPos(at.getX() + 48, at.getY() + 30, at.getZ() + 48))
                    .filter(b -> p.serverLevel().getBlockState(b).getBlock() instanceof matteroverdrive.block.TritaniumCrateBlock)
                    .map(BlockPos::immutable).findFirst().orElse(null);
            MatterOverdrive.LOGGER.info("[scene] crashed ship crate: {}", crate);
            if (crate == null) return;
            crashCrate = crate;
            p.teleportTo(p.serverLevel(), crate.getX() + 9.5, crate.getY() + 9, crate.getZ() + 9.5, Set.of(), 135f, 35f, false);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, crate.getCenter());
        }));
        at(2473, mc -> mc.options.hideGui = true);
        at(2500, mc -> shot(mc, "crashed_ship"));
        at(2501, mc -> mc.options.hideGui = false);
        at(2502, mc -> server(mc, p -> {
            if (crashCrate == null) return;
            p.teleportTo(p.serverLevel(), crashCrate.getX() + 0.5, crashCrate.getY() + 1, crashCrate.getZ() + 0.5, Set.of(), 0, 60f, false);
            p.serverLevel().getBlockState(crashCrate).useWithoutItem(p.serverLevel(), p,
                    new net.minecraft.world.phys.BlockHitResult(crashCrate.getCenter(), net.minecraft.core.Direction.UP, crashCrate, false));
        }));
        at(2510, mc -> shot(mc, "crashed_ship_crate"));
        at(2511, mc -> server(mc, p -> {
            p.closeContainer();
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "forceload remove all");
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
        }));
        // 7s: a star map showing the galaxy, then zoomed to the quadrant, the star, the planet, planet stats; access denied
        at(2520, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -1, -14), base.offset(8, 8, 8))) {
                p.serverLevel().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            BlockPos map = base.offset(0, 0, -4);
            p.serverLevel().setBlockAndUpdate(map, matteroverdrive.init.MOBlocks.STAR_MAP.get().defaultBlockState());
            if (p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) m.onPlaced(p);
            MatterOverdrive.LOGGER.info("[scene] homeworld: {}", matteroverdrive.starmap.GalaxyServer.getHomeworld(p) == null ? null
                    : matteroverdrive.starmap.GalaxyServer.getHomeworld(p).getName());
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 1.5, Set.of(), 180f, -16f, false);
            // the hologram is additive: night shows it like a dark room did in 1.7.10
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "time set 18000");
        }));
        at(2522, mc -> mc.options.hideGui = true);
        String[] zooms = {"galaxy", "quadrant", "star", "planet", "planet_stats"};
        for (int i = 0; i < zooms.length; i++) {
            int t = 2540 + i * 30;
            String name = zooms[i];
            at(t, mc -> shot(mc, "star_map_" + name));
            at(t + 2, mc -> server(mc, p -> {
                if (p.serverLevel().getBlockEntity(origin.above(30).offset(0, 0, -4)) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) m.zoom();
            }));
        }
        // planet stats with another planet of the system selected: both planets side by side
        at(2694, mc -> server(mc, p -> {
            if (!(p.serverLevel().getBlockEntity(origin.above(30).offset(0, 0, -4)) instanceof matteroverdrive.block.entity.StarMapBlockEntity m)) return;
            var home = matteroverdrive.starmap.GalaxyServer.getHomeworld(p);
            if (home == null) return;
            // another planet: of the home system, else of the nearest star with planets
            var galaxy = matteroverdrive.starmap.GalaxyServer.getGalaxy();
            matteroverdrive.starmap.Planet other = null;
            double best = Double.MAX_VALUE;
            for (var star : home.getStar().getQuadrant().getStars()) {
                for (var planet : star.getPlanets()) {
                    double d = star.getPosition().distanceTo(home.getStar().getPosition());
                    if (planet != home && d < best) {
                        best = d;
                        other = planet;
                    }
                }
            }
            if (other != null) m.setDestination(matteroverdrive.starmap.GalacticPosition.of(other));
            m.setZoomLevel(4);
            m.sync();
        }));
        at(2725, mc -> shot(mc, "star_map_planet_pair"));
        at(2728, mc -> server(mc, p -> {
            if (!(p.serverLevel().getBlockEntity(origin.above(30).offset(0, 0, -4)) instanceof matteroverdrive.block.entity.StarMapBlockEntity m)) return;
            ItemStack remove = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
            remove.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), p.getUUID());
            m.unclaim(remove);
            ItemStack claim = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
            claim.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), STRANGER);
            m.claim(claim);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY() + 30, origin.getZ() - 1.5, Set.of(), 180f, 15f, false);
        }));
        at(2745, mc -> shot(mc, "star_map_access_denied"));
        at(2746, mc -> server(mc, p -> {
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "time set 6000");
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
        }));
        at(2747, mc -> mc.options.hideGui = false);
        // 7t: the star map screen, every page
        at(2752, mc -> server(mc, p -> {
            BlockPos map = origin.above(30).offset(0, 0, -4);
            if (!(p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m)) return;
            ItemStack remove = new ItemStack(MOItems.SECURITY_PROTOCOL.get());
            remove.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), STRANGER);
            m.unclaim(remove);
            m.onPlaced(p);
            p.teleportTo(p.serverLevel(), map.getX() + 0.5, map.getY(), map.getZ() + 2.5, Set.of(), 180f, 10f, false);
        }));
        at(2756, mc -> server(mc, p -> {
            BlockPos map = origin.above(30).offset(0, 0, -4);
            if (p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) p.openMenu(m, buf -> buf.writeBlockPos(map));
        }));
        for (int i = 0; i < 5; i++) {
            int n = i;
            at(2762 + i * 14, mc -> {
                if (mc.screen instanceof matteroverdrive.client.screen.StarMapScreen screen) screen.setPage(n);
            });
            at(2774 + i * 14, mc -> shot(mc, "star_map_screen_" + n));
        }
        at(2832, mc -> server(mc, ServerPlayer::closeContainer));
        // 7u: the homeworld with buildings, ships, two constructions under way and a scout on its way to another system
        at(2840, mc -> server(mc, p -> {
            var home = matteroverdrive.starmap.GalaxyServer.getHomeworld(p);
            if (home == null) return;
            long now = p.serverLevel().getGameTime();
            home.getBuildings().clear();
            home.getFleet().clear();
            for (var item : List.of(MOItems.BUILDING_BASE.get(), MOItems.SHIP_FACTORY.get(), MOItems.BUILDING_RESIDENTIAL.get(),
                    MOItems.BUILDING_POWER_GENERATOR.get())) {
                ItemStack building = new ItemStack(item);
                ((matteroverdrive.starmap.Buildable) item).setOwner(building, p.getUUID());
                home.addBuilding(building);
            }
            for (var item : List.of(MOItems.SCOUT_SHIP.get(), MOItems.COLONIZER_SHIP.get(), MOItems.SCOUT_SHIP.get())) {
                ItemStack ship = new ItemStack(item);
                ((matteroverdrive.starmap.Buildable) item).setOwner(ship, p.getUUID());
                home.addShip(ship);
            }
            ItemStack extractor = new ItemStack(MOItems.BUILDING_MATTER_EXTRACTOR.get());
            ((matteroverdrive.starmap.Buildable) extractor.getItem()).setBuildStart(extractor, now - 20 * 200);
            home.setStackInSlot(0, extractor);
            ItemStack scout = new ItemStack(MOItems.SCOUT_SHIP.get());
            ((matteroverdrive.starmap.Buildable) scout.getItem()).setBuildStart(scout, now - 20 * 60);
            home.setStackInSlot(2, scout);
            home.markDirty();
            // the nearest other system's planet: a scout goes there
            matteroverdrive.starmap.Planet target = null;
            double best = Double.MAX_VALUE;
            for (var star : home.getStar().getQuadrant().getStars()) {
                double d = star.getPosition().distanceTo(home.getStar().getPosition());
                if (star == home.getStar() || star.getPlanets().isEmpty() || d >= best) continue;
                best = d;
                target = star.getPlanets().iterator().next();
            }
            if (target != null) {
                matteroverdrive.starmap.GalaxyServer.createTravelEvent(p.serverLevel(), matteroverdrive.starmap.GalacticPosition.of(home),
                        matteroverdrive.starmap.GalacticPosition.of(target), 2);
                matteroverdrive.starmap.GalaxyServer.sendTravelEvents(p.serverLevel().getServer());
                scoutTarget = matteroverdrive.starmap.GalacticPosition.of(target);
            }
            BlockPos map = origin.above(30).offset(0, 0, -4);
            if (p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) {
                m.setDestination(matteroverdrive.starmap.GalacticPosition.of(home));
                m.setZoomLevel(3);
                m.sync();
            }
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "time set 18000");
            p.teleportTo(p.serverLevel(), map.getX() + 0.5, map.getY(), map.getZ() + 4.0, Set.of(), 180f, -14f, false);
        }));
        at(2842, mc -> mc.options.hideGui = true);
        at(2870, mc -> shot(mc, "starmap_planet_buildings"));
        at(2872, mc -> server(mc, p -> {
            BlockPos map = origin.above(30).offset(0, 0, -4);
            if (p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) {
                m.setZoomLevel(0);
                m.sync();
            }
        }));
        at(2900, mc -> shot(mc, "starmap_travel"));
        at(2901, mc -> mc.options.hideGui = false);
        at(2904, mc -> server(mc, p -> {
            BlockPos map = origin.above(30).offset(0, 0, -4);
            if (p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) {
                m.setZoomLevel(3);
                m.sync();
                p.teleportTo(p.serverLevel(), map.getX() + 0.5, map.getY(), map.getZ() + 2.5, Set.of(), 180f, 10f, false);
                p.openMenu(m, buf -> buf.writeBlockPos(map));
            }
        }));
        at(2924, mc -> shot(mc, "starmap_screen_construction"));
        at(2926, mc -> server(mc, p -> {
            BlockPos map = origin.above(30).offset(0, 0, -4);
            if (scoutTarget != null && p.serverLevel().getBlockEntity(map) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) {
                m.setDestination(scoutTarget);
                m.setZoomLevel(4);
                m.sync();
            }
        }));
        at(2950, mc -> shot(mc, "starmap_screen_fleet"));
        at(2952, mc -> server(mc, p -> {
            p.closeContainer();
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "time set 6000");
        }));
        // leftovers: legendary weapon and legendary part tooltips
        at(2958, mc -> {
            var rng = net.minecraft.util.RandomSource.create(7);
            ItemStack weapon = matteroverdrive.item.weapon.WeaponFactory.randomDecorated(rng, 3, true);
            weapon.set(matteroverdrive.init.MODataComponents.LEGENDARY_WEAPON.get(),
                    new matteroverdrive.item.weapon.WeaponFactory.Legendary(1.3f, 0.8f, 0.9f, 1.3f));
            mc.setScreen(new TooltipScreen(weapon));
        });
        at(2966, mc -> shot(mc, "legendary_weapon"));
        at(2968, mc -> {
            ItemStack part = new ItemStack(MOItems.ROGUE_ANDROID_ARMS.get());
            part.set(matteroverdrive.init.MODataComponents.LEGENDARY_PART.get(),
                    new matteroverdrive.item.android.BionicPartItem.Legendary(2, 3, 0.1, 0.2, -0.4, -0.06));
            part.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, matteroverdrive.item.weapon.WeaponFactory.legendaryName(part));
            mc.setScreen(new TooltipScreen(part));
        });
        at(2976, mc -> shot(mc, "legendary_part"));
        at(2978, mc -> mc.setScreen(null));
        // leftovers: the analyzer's Config page with a network flash drive (two marked machines) in its destination filter
        at(2984, mc -> server(mc, p -> {
            if (!(p.serverLevel().getBlockEntity(analyzerPos) instanceof matteroverdrive.machine.MachineBlockEntity m)) return;
            ItemStack drive = new ItemStack(MOItems.NETWORK_FLASH_DRIVE.get());
            drive.set(matteroverdrive.init.MODataComponents.NETWORK_FILTER.get(), List.of(storagePos, replicatorPos));
            var inv = m.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                if (inv.spec(i).role() == matteroverdrive.machine.MachineInventory.Role.FILTER) inv.setStack(i, drive);
            }
            p.getInventory().setItem(p.getInventory().getSelectedSlot(), drive.copy());
            p.teleportTo(p.serverLevel(), analyzerPos.getX() + 0.5, analyzerPos.getY(), analyzerPos.getZ() + 2.5, Set.of(), 180f, 20f, false);
        }));
        at(2988, mc -> openMachine(mc, analyzerPos));
        at(2994, mc -> page(mc, MachineMenu.Page.CONFIG));
        at(3002, mc -> shot(mc, "analyzer_destination_filter"));
        at(3003, mc -> server(mc, ServerPlayer::closeContainer));
        at(3005, mc -> mc.setScreen(new TooltipScreen(mc.player.getMainHandItem())));
        at(3013, mc -> shot(mc, "network_flash_drive"));
        at(3014, mc -> mc.setScreen(null));
        // leftovers: machine renderers - the inscriber at work, pattern storage drives, the monitor screen, the replicated item
        at(3020, mc -> server(mc, p -> {
            if (p.serverLevel().getBlockEntity(inscriberPos) instanceof matteroverdrive.block.entity.InscriberBlockEntity inscriber) {
                inscriber.getEnergy().set(inscriber.getEnergy().getCapacity());
                inscriber.getInventory().setStack(matteroverdrive.block.entity.InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 4));
                inscriber.getInventory().setStack(matteroverdrive.block.entity.InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 4));
            }
            if (p.serverLevel().getBlockEntity(replicatorPos) instanceof matteroverdrive.block.entity.ReplicatorBlockEntity replicator) {
                replicator.getInventory().setStack(matteroverdrive.block.entity.ReplicatorBlockEntity.OUTPUT, new ItemStack(Items.DIAMOND, 3));
            }
            p.teleportTo(p.serverLevel(), inscriberPos.getX() + 0.5, inscriberPos.getY() + 0.4, inscriberPos.getZ() + 2.0, Set.of(), 180f, 35f, false);
        }));
        at(3022, mc -> mc.options.hideGui = true);
        at(3040, mc -> shot(mc, "inscriber_working"));
        at(3050, mc -> shot(mc, "inscriber_working_2"));
        at(3052, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), monitorPos.getX() + 0.5, monitorPos.getY() + 0.2, monitorPos.getZ() + 3.0,
                Set.of(), 180f, 18f, false)));
        at(3080, mc -> shot(mc, "network_machines"));
        at(3082, mc -> server(mc, p -> p.teleportTo(p.serverLevel(), inscriberPos.getX() + 0.5, inscriberPos.getY() + 0.6, inscriberPos.getZ() + 1.1,
                Set.of(), 180f, 60f, false)));
        at(3100, mc -> shot(mc, "inscriber_close"));
        at(3102, mc -> server(mc, p -> {
            BlockPos pos = origin.above(30).offset(0, 0, -4);
            for (BlockPos b : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 3, 3))) {
                p.serverLevel().setBlock(b, b.getY() < pos.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.serverLevel().setBlockAndUpdate(pos, MOBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (p.serverLevel().getBlockEntity(pos) instanceof matteroverdrive.block.entity.PatternStorageBlockEntity storage) {
                for (int i = 0; i < 6; i++) storage.getInventory().setStack(i, new ItemStack(MOItems.PATTERN_DRIVE.get()));
            }
            p.teleportTo(p.serverLevel(), pos.getX() + 2.2, pos.getY(), pos.getZ() + 2.2, Set.of(), 135f, 30f, false);
        }));
        at(3120, mc -> shot(mc, "pattern_storage_drives"));
        at(3121, mc -> server(mc, p -> {
            BlockPos pos = origin.above(30).offset(0, 0, -4);
            p.teleportTo(p.serverLevel(), pos.getX() - 1.2, pos.getY(), pos.getZ() + 2.2, Set.of(), 225f, 30f, false);
        }));
        at(3130, mc -> shot(mc, "pattern_storage_side"));
        at(3132, mc -> server(mc, p -> {
            BlockPos pos = origin.above(30).offset(0, 0, -4);
            p.serverLevel().setBlockAndUpdate(pos, MOBlocks.REPLICATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (p.serverLevel().getBlockEntity(pos) instanceof matteroverdrive.block.entity.ReplicatorBlockEntity replicator) {
                replicator.getInventory().setStack(matteroverdrive.block.entity.ReplicatorBlockEntity.OUTPUT, new ItemStack(Items.DIAMOND, 3));
            }
            // the replaced pattern storage dropped its drives
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(8))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            p.teleportTo(p.serverLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.8, Set.of(), 180f, 30f, false);
        }));
        at(3160, mc -> shot(mc, "replicator_item"));
        at(3161, mc -> mc.options.hideGui = false);
        // leftovers: machine sounds - the inscriber at work and the stabilizer aiming at the anomaly loop their sounds
        at(3162, mc -> server(mc, p -> {
            if (p.serverLevel().getBlockEntity(inscriberPos) instanceof matteroverdrive.block.entity.InscriberBlockEntity inscriber) {
                inscriber.getEnergy().set(inscriber.getEnergy().getCapacity());
                inscriber.getInventory().setStack(matteroverdrive.block.entity.InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 4));
                inscriber.getInventory().setStack(matteroverdrive.block.entity.InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 4));
            }
            // the anomaly may have pulled a block into the beam
            for (int z = -9; z <= -7; z++) p.serverLevel().setBlockAndUpdate(origin.offset(0, 3, z), Blocks.AIR.defaultBlockState());
            p.teleportTo(p.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() - 4.5, Set.of(), 180f, 0f, false);
        }));
        at(3180, mc -> {
            var sounds = mc.getSoundManager();
            for (BlockPos pos : List.of(inscriberPos, origin.offset(0, 3, -6), origin.offset(0, 3, -10))) {
                Object sound = mc.level.getBlockEntity(pos) instanceof matteroverdrive.machine.MachineBlockEntity m ? m.clientSound
                        : mc.level.getBlockEntity(pos) instanceof matteroverdrive.block.entity.GravitationalAnomalyBlockEntity a ? a.clientSound : null;
                MatterOverdrive.LOGGER.info("[scene] sound at {}: {} active={} playing={} volume={}", pos, mc.level.getBlockState(pos).getBlock(),
                        mc.level.getBlockEntity(pos) instanceof matteroverdrive.machine.MachineBlockEntity m && m.isActive(),
                        sound instanceof net.minecraft.client.resources.sounds.SoundInstance s && sounds.isActive(s),
                        sound instanceof net.minecraft.client.resources.sounds.SoundInstance s ? s.getVolume() : 0);
            }
        });
        // leftovers: the machine item tooltip - a replicator carried with its energy, matter and owner
        at(3182, mc -> {
            ItemStack item = new ItemStack(MOItems.REPLICATOR.get());
            if (mc.level.getBlockEntity(replicatorPos) instanceof matteroverdrive.machine.MachineBlockEntity m) {
                m.getMatterTank().setMatter(600);
                item.applyComponents(m.collectComponents());
            }
            item.set(matteroverdrive.init.MODataComponents.SECURITY_OWNER.get(), mc.player.getUUID());
            mc.setScreen(new TooltipScreen(item));
        });
        at(3190, mc -> shot(mc, "machine_item_tooltip"));
        at(3191, mc -> MachineTooltip.sceneShift = true);
        at(3199, mc -> shot(mc, "machine_item_tooltip_shift"));
        at(3200, mc -> {
            MachineTooltip.sceneShift = false;
            mc.setScreen(null);
        });
        // leftovers: the android shield bubble with two hit flashes (third and first person), then the teleport marker
        at(3202, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos b : BlockPos.betweenClosed(base.offset(-6, -1, -12), base.offset(6, 4, 6))) {
                p.serverLevel().setBlock(b, b.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(base).inflate(10)).forEach(e -> e.discard());
            matteroverdrive.android.Android.setAndroid(p, true);
            var data = matteroverdrive.android.Android.get(p);
            data.setStack(matteroverdrive.android.AndroidData.SLOT_BATTERY, MOItems.BATTERY.get().charged());
            data.getStats().put(matteroverdrive.android.BioticStats.SHIELD.id(), 1);
            data.getStats().put(matteroverdrive.android.BioticStats.TELEPORT.id(), 1);
            data.setActiveStat(matteroverdrive.android.BioticStats.SHIELD.id());
            data.setEffect("ShieldLastUse", 0);
            matteroverdrive.android.Android.onActionKey(p);
            matteroverdrive.android.Android.sync(p);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 10f, false);
        }));
        at(3204, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            mc.options.hideGui = true;
        });
        at(3212, mc -> server(mc, p -> {
            matteroverdrive.network.AndroidPayloads.sendShieldHit(p, new net.minecraft.world.phys.Vec3(-3, 0.5, -3));
            matteroverdrive.network.AndroidPayloads.sendShieldHit(p, new net.minecraft.world.phys.Vec3(3, -0.5, -2));
        }));
        at(3213, mc -> shot(mc, "android_shield"));
        at(3218, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        at(3228, mc -> shot(mc, "android_shield_first_person"));
        at(3230, mc -> server(mc, p -> {
            var data = matteroverdrive.android.Android.get(p);
            data.setEffect("Shield", 0);
            data.setActiveStat(matteroverdrive.android.BioticStats.TELEPORT.id());
            matteroverdrive.android.Android.sync(p);
            BlockPos base = origin.above(30);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 4.5, Set.of(), 180f, 35f, false);
        }));
        at(3236, mc -> matteroverdrive.client.android.AndroidKeys.ABILITY_USE.setDown(true));
        at(3246, mc -> shot(mc, "android_teleport_marker"));
        at(3248, mc -> matteroverdrive.client.android.AndroidKeys.ABILITY_USE.setDown(false));
        at(3256, mc -> server(mc, p -> {
            MatterOverdrive.LOGGER.info("[scene] after teleport: {}", p.position());
            matteroverdrive.android.Android.setAndroid(p, false);
        }));
        // leftovers: the 1.7.10 tritanium armor model - the player in the full set, armor stands with helmet + boots
        // and chestplate + leggings
        at(3258, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 0f, false);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(MOItems.TRITANIUM_HELMET.get()));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get()));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(MOItems.TRITANIUM_LEGGINGS.get()));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(MOItems.TRITANIUM_BOOTS.get()));
            p.getInventory().setItem(p.getInventory().getSelectedSlot(), ItemStack.EMPTY);
            // the platform is underground: light it up and clear the mobs that wandered in
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600, 0, false, false));
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(base).inflate(24)).forEach(e -> e.discard());
            for (int side : new int[] {-1, 1}) {
                var stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                stand.snapTo(base.getX() + 0.5 + side * 1.6, base.getY(), base.getZ() - 0.5, 0, 0);
                if (side < 0) {
                    stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(MOItems.TRITANIUM_HELMET.get()));
                    stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(MOItems.TRITANIUM_BOOTS.get()));
                } else {
                    stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get()));
                    stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(MOItems.TRITANIUM_LEGGINGS.get()));
                }
                stand.addTag("mo_scene_armor");
                p.serverLevel().addFreshEntity(stand);
            }
        }));
        at(3260, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            mc.options.hideGui = true;
        });
        at(3272, mc -> shot(mc, "tritanium_armor_front"));
        at(3273, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        at(3282, mc -> shot(mc, "tritanium_armor_back"));
        at(3283, mc -> server(mc, p -> {
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                p.setItemSlot(slot, ItemStack.EMPTY);
            }
            p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, new AABB(origin.above(30)).inflate(8),
                    e -> e.getTags().contains("mo_scene_armor")).forEach(net.minecraft.world.entity.Entity::discard);
        }));
        // leftovers: the stabilizer's beam with its motes, and the anomaly screen on its back (in the clean room)
        at(3285, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
            server(mc, p -> {
                BlockPos base = origin.above(30);
                p.serverLevel().setBlockAndUpdate(base.offset(0, 1, -9), MOBlocks.GRAVITATIONAL_ANOMALY.get().defaultBlockState());
                if (p.serverLevel().getBlockEntity(base.offset(0, 1, -9)) instanceof matteroverdrive.block.entity.GravitationalAnomalyBlockEntity a) a.setMass(1500);
                p.serverLevel().setBlockAndUpdate(base.offset(0, 1, -3), MOBlocks.GRAVITATIONAL_STABILIZER.get().defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.NORTH));
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600, 0, false, false));
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.teleportTo(p.serverLevel(), base.getX() + 4.5, base.getY() + 1, base.getZ() - 5.5, Set.of(), 90f, 10f, false);
            });
        });
        at(3310, mc -> shot(mc, "stabilizer_beam"));
        // the anomaly close up, off the beam (1.7.10: black sphere, thin white ring, dark specks drawn in)
        at(3311, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            p.teleportTo(p.serverLevel(), base.getX() + 3.5, base.getY() + 1, base.getZ() - 9 + 0.5, Set.of(), 90f, 0f, false);
        }));
        at(3318, mc -> shot(mc, "anomaly_close"));
        at(3319, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY() + 0.4, base.getZ() - 0.6, Set.of(), 180f, 0f, false);
        }));
        at(3324, mc -> shot(mc, "stabilizer_screen"));
        at(3325, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            p.serverLevel().setBlock(base.offset(0, 1, -9), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            p.serverLevel().setBlock(base.offset(0, 1, -3), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }));
        // leftovers: plasma shotgun - the full spread (thin bolts) and a charged shot (one big bolt): in flight and their
        // hits on the wall (sparks, puff)
        at(3327, mc -> {
            mc.options.hideGui = true;
            server(mc, p -> {
                BlockPos base = origin.above(30);
                ItemStack shotgun = new ItemStack(MOItems.PLASMA_SHOTGUN.get());
                matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(shotgun, matteroverdrive.item.weapon.EnergyWeaponItem.CAPACITY);
                p.getInventory().setItem(p.getInventory().getSelectedSlot(), shotgun);
                for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                        net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                    p.setItemSlot(slot, ItemStack.EMPTY);
                }
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 900, 0, false, false));
                p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() + 5.5, Set.of(), 180f, 0f, false);
            });
        });
        at(3335, mc -> server(mc, p -> MOItems.PLASMA_SHOTGUN.get().tryFire(p, p.getMainHandItem(), false)));
        at(3336, mc -> shot(mc, "shotgun_spread"));
        at(3362, mc -> server(mc, p -> {
            var shotgun = MOItems.PLASMA_SHOTGUN.get();
            shotgun.releaseUsing(p.getMainHandItem(), p.serverLevel(), p, shotgun.getUseDuration(p.getMainHandItem(), p) - 25);
        }));
        at(3363, mc -> shot(mc, "shotgun_charged"));
        at(3366, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server(mc, p -> {
                BlockPos base = origin.above(30);
                p.teleportTo(p.serverLevel(), base.getX() + 0.5, base.getY(), base.getZ() - 8.5, Set.of(), 180f, 0f, false);
            });
        });
        at(3390, mc -> server(mc, p -> MOItems.PLASMA_SHOTGUN.get().tryFire(p, p.getMainHandItem(), false)));
        at(3392, mc -> shot(mc, "shotgun_spread_hit"));
        at(3418, mc -> server(mc, p -> {
            var shotgun = MOItems.PLASMA_SHOTGUN.get();
            shotgun.releaseUsing(p.getMainHandItem(), p.serverLevel(), p, shotgun.getUseDuration(p.getMainHandItem(), p) - 25);
        }));
        at(3420, mc -> shot(mc, "shotgun_charged_hit"));
        at(3421, mc -> server(mc, p -> {
            p.getInventory().setItem(p.getInventory().getSelectedSlot(), ItemStack.EMPTY);
            p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        }));
        at(3423, mc -> mc.options.hideGui = false);
        // ---- gallery (Modrinth): daylight scenes on the surface, no GUI. ./gradlew runScene -PsceneFrom=3440 ----
        at(3440, mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server(mc, p -> {
                galleryReset(p, 6000);
                BlockPos c = gallerySite(p, 40);
                galleryA = c;
                ServerLevel level = p.serverLevel();
                // the matter network row: analyzer - storage - monitor - replicator (network pipes), decomposer feeding it
                BlockPos row = c.offset(0, 0, -2);
                level.setBlockAndUpdate(row.offset(-4, 0, 0), MOBlocks.ANALYZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(row.offset(-2, 0, 0), MOBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(row, MOBlocks.PATTERN_MONITOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(row.offset(2, 0, 0), MOBlocks.REPLICATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                for (int x : new int[] {-3, -1, 1}) level.setBlockAndUpdate(row.offset(x, 0, 0), MOBlocks.NETWORK_PIPE.get().defaultBlockState());
                level.setBlockAndUpdate(row.offset(3, 0, 0), MOBlocks.MATTER_PIPE.get().defaultBlockState());
                level.setBlockAndUpdate(row.offset(4, 0, 0), MOBlocks.DECOMPOSER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(row.offset(-2, 0, -2), MOBlocks.INSCRIBER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(row.offset(2, 0, -2), MOBlocks.SOLAR_PANEL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
                for (int x : new int[] {-5, 5}) {
                    level.setBlockAndUpdate(row.offset(x, 0, 1), matteroverdrive.init.MODecorative.TRITANIUM_LAMP.get().defaultBlockState());
                }
                var analyzer = (matteroverdrive.block.entity.AnalyzerBlockEntity) level.getBlockEntity(row.offset(-4, 0, 0));
                analyzer.getEnergy().set(500000);
                analyzer.getInventory().setStack(matteroverdrive.block.entity.AnalyzerBlockEntity.INPUT, new ItemStack(Items.DIAMOND, 8));
                ItemStack aDrive = new ItemStack(MOItems.PATTERN_DRIVE.get());
                MOItems.PATTERN_DRIVE.get().addProgress(aDrive, Items.EMERALD, 40);
                analyzer.getInventory().setStack(matteroverdrive.block.entity.AnalyzerBlockEntity.DATABASE, aDrive);
                var storage = (matteroverdrive.block.entity.PatternStorageBlockEntity) level.getBlockEntity(row.offset(-2, 0, 0));
                storage.getEnergy().set(60000);
                Item[][] drives = {{Items.IRON_INGOT, Items.DIAMOND}, {Items.GOLD_INGOT, Items.REDSTONE}, {Items.OAK_LOG, Items.COBBLESTONE},
                        {Items.EMERALD, Items.LAPIS_LAZULI}};
                for (int i = 0; i < drives.length; i++) {
                    ItemStack d = new ItemStack(MOItems.PATTERN_DRIVE.get());
                    for (Item it : drives[i]) MOItems.PATTERN_DRIVE.get().addProgress(d, it, 100);
                    storage.getInventory().setStack(i, d);
                }
                var replicator = (matteroverdrive.block.entity.ReplicatorBlockEntity) level.getBlockEntity(row.offset(2, 0, 0));
                replicator.getEnergy().set(1000000);
                replicator.getMatterTank().setMatter(1000);
                replicator.getInventory().setStack(matteroverdrive.block.entity.ReplicatorBlockEntity.SHIELDING, new ItemStack(MOItems.TRITANIUM_PLATE.get(), 5));
                replicator.getInventory().setStack(matteroverdrive.block.entity.ReplicatorBlockEntity.OUTPUT, new ItemStack(Items.DIAMOND, 7));
                replicator.setTask(new matteroverdrive.block.entity.ReplicatorBlockEntity.Task(
                        new matteroverdrive.matter.ItemPattern(Items.DIAMOND.builtInRegistryHolder(), 100), 64));
                var decomposer = (matteroverdrive.block.entity.DecomposerBlockEntity) level.getBlockEntity(row.offset(4, 0, 0));
                decomposer.getEnergy().set(400000);
                decomposer.getInventory().setStack(matteroverdrive.block.entity.DecomposerBlockEntity.INPUT, new ItemStack(Items.COBBLESTONE, 64));
                if (level.getBlockEntity(row.offset(-2, 0, -2)) instanceof InscriberBlockEntity inscriber) {
                    inscriber.getEnergy().set(300000);
                    inscriber.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 16));
                    inscriber.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 16));
                }
                galleryCamera(p, c.offset(0, 0, 3), 0.5, 1.6, 0.5, 180f, 22f);
            });
        });
        // the first site is 1000 blocks away: give the client time to load and mesh the chunks
        at(3490, mc -> shot(mc, "gallery_matter_network"));
        at(3491, mc -> server(mc, p -> galleryCamera(p, galleryA.offset(4, 0, 2), 0.5, 1.6, 0.5, 150f, 22f)));
        at(3500, mc -> shot(mc, "gallery_matter_network_2"));
        // gravitational anomaly held by three stabilizers (1.7.10 guide picture)
        at(3502, mc -> server(mc, p -> {
            BlockPos c = gallerySite(p, 80);
            ServerLevel level = p.serverLevel();
            BlockPos a = c.offset(0, 2, -2);
            level.setBlockAndUpdate(a, MOBlocks.GRAVITATIONAL_ANOMALY.get().defaultBlockState());
            if (level.getBlockEntity(a) instanceof matteroverdrive.block.entity.GravitationalAnomalyBlockEntity anomaly) anomaly.setMass(4000);
            Object[][] stabilizers = {{a.offset(0, 0, -6), Direction.SOUTH}, {a.offset(-6, 0, 0), Direction.EAST}, {a.offset(6, 0, 0), Direction.WEST}};
            for (Object[] st : stabilizers) {
                BlockPos sp = (BlockPos) st[0];
                for (int y = 1; y <= 2; y++) level.setBlockAndUpdate(sp.below(y), matteroverdrive.init.MODecorative.CLEAN.get().defaultBlockState());
                level.setBlockAndUpdate(sp, MOBlocks.GRAVITATIONAL_STABILIZER.get().defaultBlockState().setValue(MachineBlock.FACING, (Direction) st[1]));
            }
            galleryAnomaly = a;
            // at sunset: unlit additive beams wash out on a sunlit floor (1.7.10 too); the time (synced to the client every
            // 20 ticks) is picked so the noise gives all three beams some colour
            level.setDayTime(12612);
            galleryCamera(p, c.offset(2, 0, 4), 0.5, 2.6, 0.5, 160f, 6f);
        }));
        at(3534, mc -> server(mc, p -> {
            for (BlockPos sp : new BlockPos[] {galleryAnomaly.offset(0, 0, -6), galleryAnomaly.offset(-6, 0, 0), galleryAnomaly.offset(6, 0, 0)}) {
                if (p.serverLevel().getBlockEntity(sp) instanceof matteroverdrive.block.entity.GravitationalStabilizerBlockEntity st) {
                    MatterOverdrive.LOGGER.info("[scene] stabilizer {} active={} rgb={},{},{}", sp, st.isActive(), st.getBeamColorR(), st.getBeamColorG(), st.getBeamColorB());
                } else {
                    MatterOverdrive.LOGGER.info("[scene] stabilizer {} missing: {}", sp, p.serverLevel().getBlockState(sp));
                }
            }
            MatterOverdrive.LOGGER.info("[scene] anomaly: {}", p.serverLevel().getBlockState(galleryAnomaly));
        }));
        at(3535, mc -> shot(mc, "gallery_anomaly"));
        // fusion reactor around an anomaly
        at(3537, mc -> server(mc, p -> {
            p.serverLevel().setDayTime(6000);
            BlockPos c = gallerySite(p, 120);
            BlockPos controller = c.offset(0, 0, 4);
            buildReactor(p.serverLevel(), controller);
            galleryCamera(p, controller.offset(6, 0, 4), 0.5, 4.5, 0.5, 145f, 28f);
        }));
        at(3566, mc -> server(mc, p -> p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(24)).forEach(e -> e.discard())));
        at(3570, mc -> shot(mc, "gallery_fusion_reactor"));
        // an android with its shield up in front of the machines
        at(3572, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            server(mc, p -> {
                matteroverdrive.android.Android.setAndroid(p, true);
                var data = matteroverdrive.android.Android.get(p);
                data.setStack(matteroverdrive.android.AndroidData.SLOT_BATTERY, MOItems.BATTERY.get().charged());
                data.getStats().put(matteroverdrive.android.BioticStats.SHIELD.id(), 1);
                data.setActiveStat(matteroverdrive.android.BioticStats.SHIELD.id());
                data.setEffect("ShieldLastUse", 0);
                matteroverdrive.android.Android.onActionKey(p);
                matteroverdrive.android.Android.sync(p);
                ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
                matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(rifle, matteroverdrive.item.weapon.EnergyWeaponItem.CAPACITY);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rifle);
                galleryCamera(p, galleryA.offset(0, 0, 1), 0.5, 0, 0.5, 0f, 8f);
            });
        });
        at(3590, mc -> shot(mc, "gallery_android_shield"));
        // the android HUD in first person
        at(3592, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            server(mc, p -> {
                var data = matteroverdrive.android.Android.get(p);
                data.setEffect("Shield", 0);
                for (var stat : new matteroverdrive.android.BioticStat[] {matteroverdrive.android.BioticStats.NANO_ARMOR,
                        matteroverdrive.android.BioticStats.CLOAK, matteroverdrive.android.BioticStats.MINIMAP,
                        matteroverdrive.android.BioticStats.TELEPORT, matteroverdrive.android.BioticStats.NIGHT_VISION}) {
                    data.getStats().put(stat.id(), stat.maxLevel());
                }
                data.setStack(matteroverdrive.android.AndroidData.SLOT_HEAD, new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get()));
                matteroverdrive.android.Android.sync(p);
                galleryCamera(p, galleryA.offset(0, 0, 4), 0.5, 0, 0.5, 180f, 12f);
            });
        });
        at(3610, mc -> shot(mc, "gallery_android_hud"));
        // a phaser beam on a rogue android
        at(3612, mc -> {
            // first person: the beam is a camera-facing ribbon, edge-on from behind; the hand needs the GUI shown
            mc.options.hideGui = false;
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server(mc, p -> {
                matteroverdrive.android.Android.setAndroid(p, false);
                ItemStack phaser = new ItemStack(MOItems.PHASER.get());
                matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(phaser, 32000);
                phaser.set(matteroverdrive.init.MODataComponents.PHASER_LEVEL.get(), 2);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, phaser);
                var rogue = matteroverdrive.init.MOEntities.ROGUE_ANDROID.get().create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                rogue.snapTo(galleryA.getX() + 1.5, galleryA.getY(), galleryA.getZ() + 8.5, 180, 0);
                rogue.setNoAi(true);
                rogue.addTag("mo_gallery");
                p.serverLevel().addFreshEntity(rogue);
                galleryCamera(p, galleryA.offset(0, 0, 2), 0.5, 0, 0.5, -9.5f, 6f);
            });
        });
        at(3622, mc -> mc.options.keyUse.setDown(true));
        at(3628, mc -> {
            shot(mc, "gallery_phaser");
            MatterOverdrive.LOGGER.info("[scene] phaser: using={} item={} rogues={}", mc.player.isUsingItem(), mc.player.getMainHandItem(),
                    mc.level.getEntitiesOfClass(matteroverdrive.entity.monster.RogueAndroid.class, new AABB(galleryA).inflate(20)).size());
        });
        at(3629, mc -> {
            mc.options.keyUse.setDown(false);
            mc.options.hideGui = true;
        });
        // the tritanium armor: the player in the full set between two armor stands
        at(3631, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            server(mc, p -> {
                p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(galleryA).inflate(20),
                        e -> e.getTags().contains("mo_gallery")).forEach(net.minecraft.world.entity.Entity::discard);
                p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MOItems.TRITANIUM_HELMET.get()));
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get()));
                p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(MOItems.TRITANIUM_LEGGINGS.get()));
                p.setItemSlot(EquipmentSlot.FEET, new ItemStack(MOItems.TRITANIUM_BOOTS.get()));
                ItemStack shotgun = new ItemStack(MOItems.PLASMA_SHOTGUN.get());
                matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(shotgun, matteroverdrive.item.weapon.EnergyWeaponItem.CAPACITY);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shotgun);
                for (int side : new int[] {-1, 1}) {
                    var stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(p.serverLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    stand.snapTo(galleryA.getX() + 0.5 + side * 1.6, galleryA.getY(), galleryA.getZ() + 1.0, 0, 0);
                    stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MOItems.TRITANIUM_HELMET.get()));
                    stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get()));
                    stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(MOItems.TRITANIUM_LEGGINGS.get()));
                    stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(MOItems.TRITANIUM_BOOTS.get()));
                    stand.addTag("mo_gallery");
                    p.serverLevel().addFreshEntity(stand);
                }
                galleryCamera(p, galleryA.offset(0, 0, 1), 0.5, 0, 0.5, 0f, 5f);
            });
        });
        at(3650, mc -> shot(mc, "gallery_tritanium_armor"));
        // the star map hologram at night
        at(3652, mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server(mc, p -> {
                p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(galleryA).inflate(20),
                        e -> e.getTags().contains("mo_gallery")).forEach(net.minecraft.world.entity.Entity::discard);
                for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    p.setItemSlot(slot, ItemStack.EMPTY);
                }
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                BlockPos c = gallerySite(p, 160);
                galleryMap = c.offset(0, 0, -2);
                p.serverLevel().setBlockAndUpdate(galleryMap, MOBlocks.STAR_MAP.get().defaultBlockState());
                if (p.serverLevel().getBlockEntity(galleryMap) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) m.onPlaced(p);
                p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                        "time set 18000");
                galleryCamera(p, c.offset(0, 0, 1), 0.5, 0.0, 0.3, 180f, -22f);
            });
        });
        at(3680, mc -> shot(mc, "gallery_star_map"));
        for (int i = 0; i < 3; i++) {
            int t = 3682 + i * 26;
            at(t, mc -> server(mc, p -> {
                if (p.serverLevel().getBlockEntity(galleryMap) instanceof matteroverdrive.block.entity.StarMapBlockEntity m) m.zoom();
            }));
            String name = "gallery_star_map_" + (i + 1);
            at(t + 24, mc -> shot(mc, name));
        }
        at(3762, mc -> server(mc, p -> {
            p.serverLevel().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                    "time set 6000");
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }));
        at(3764, mc -> mc.options.hideGui = false);
        // matter of modern items: derived (waxed oxidized copper from weathering + the waxing recipe) and estimated ("~")
        at(3770, mc -> mc.setScreen(new TooltipScreen(new ItemStack(Items.WAXED_OXIDIZED_CHISELED_COPPER))));
        at(3778, mc -> shot(mc, "matter_tooltip_copper"));
        at(3779, mc -> mc.setScreen(new TooltipScreen(new ItemStack(Items.NETHERITE_SWORD))));
        at(3787, mc -> shot(mc, "matter_tooltip_netherite"));
        at(3788, mc -> mc.setScreen(new TooltipScreen(new ItemStack(Items.SKULL_POTTERY_SHERD))));
        at(3796, mc -> shot(mc, "matter_tooltip_estimated"));
        at(3797, mc -> mc.setScreen(null));
        at(3800, mc -> mc.stop());
    }

    /** Shows one item's tooltip in the middle of the screen. */
    private static class TooltipScreen extends net.minecraft.client.gui.screens.Screen {
        private final ItemStack stack;

        TooltipScreen(ItemStack stack) {
            super(net.minecraft.network.chat.Component.empty());
            this.stack = stack;
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            g.renderItem(stack, width / 2 - 100, height / 2 - 40);
            g.renderItemDecorations(font, stack, width / 2 - 100, height / 2 - 40);
            matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, stack, width / 2 - 80, height / 2 - 40);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static void at(int t, Consumer<Minecraft> action) {
        STEPS.add(new Step(t, action));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        // The window is usually unfocused while an agent runs this: never pause, or the integrated server stops.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
        }
        tick++;
        if (tick == 41 && FROM > 41) tick = FROM;
        for (Step step : STEPS) {
            if (step.at() == tick) {
                MatterOverdrive.LOGGER.info("[scene] tick {}", tick);
                step.action().accept(mc);
            }
        }
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(mc.player.getUUID())));
    }

    /** Machines on a stone floor north of the player, the player facing them; noon, clear sky. */
    private static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.setGameMode(GameType.CREATIVE);
        // A fixed spot (the spawn, on the flat world's surface): the player's saved position drifts between runs.
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = new BlockPos(spawn.getX(), level.getMinY() + 4, spawn.getZ());
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 0f, false);
        origin = base;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-3, -1, -8), base.offset(3, -1, 0))) {
            level.setBlockAndUpdate(p, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        // Clear the previous run's scene without machine side effects (they would drop their contents),
        // then remove any items already lying around.
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-3, 0, -8), base.offset(3, 4, 0))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }
        level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8)).forEach(e -> e.discard());
        inscriberPos = base.offset(-1, 0, -3);
        solarPos = base.offset(1, 0, -3);
        level.setBlockAndUpdate(inscriberPos, MOBlocks.INSCRIBER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(solarPos, MOBlocks.SOLAR_PANEL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        decomposerPos = base.offset(-3, 0, -3);
        recyclerPos = base.offset(3, 0, -3);
        level.setBlockAndUpdate(decomposerPos, MOBlocks.DECOMPOSER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(recyclerPos, MOBlocks.RECYCLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(base.offset(-3, 0, -2), MOBlocks.MATTER_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(-3, 0, -1), MOBlocks.MATTER_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(-2, 0, -1), MOBlocks.HEAVY_MATTER_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(-3, 1, -1), MOBlocks.MATTER_PIPE.get().defaultBlockState());
        var decomposer = (matteroverdrive.block.entity.DecomposerBlockEntity) level.getBlockEntity(decomposerPos);
        decomposer.getEnergy().set(400000);
        decomposer.getMatterTank().setMatter(300);
        decomposer.getInventory().setStack(matteroverdrive.block.entity.DecomposerBlockEntity.INPUT, new ItemStack(Items.COBBLESTONE, 64));
        var recycler = (matteroverdrive.block.entity.RecyclerBlockEntity) level.getBlockEntity(recyclerPos);
        recycler.getEnergy().set(200000);
        recycler.getInventory().setStack(matteroverdrive.block.entity.RecyclerBlockEntity.INPUT,
                matteroverdrive.item.MatterDustItem.withMatter(MOItems.MATTER_DUST.get(), 1).copyWithCount(16));
        // matter network row: analyzer - pipe - storage - pipe - monitor - pipe - replicator
        analyzerPos = base.offset(-3, 0, -7);
        storagePos = base.offset(-1, 0, -7);
        monitorPos = base.offset(1, 0, -7);
        replicatorPos = base.offset(3, 0, -7);
        for (int x : new int[] {-2, 0, 2}) level.setBlockAndUpdate(base.offset(x, 0, -7), MOBlocks.NETWORK_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(storagePos, MOBlocks.PATTERN_STORAGE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(monitorPos, MOBlocks.PATTERN_MONITOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(replicatorPos, MOBlocks.REPLICATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        var storage = (matteroverdrive.block.entity.PatternStorageBlockEntity) level.getBlockEntity(storagePos);
        storage.getEnergy().set(60000);
        Item[][] drives = {{Items.IRON_INGOT, Items.DIAMOND}, {Items.COBBLESTONE, Items.OAK_LOG}, {Items.GOLD_INGOT, Items.REDSTONE}};
        int[][] progress = {{100, 60}, {100, 100}, {40, 100}};
        for (int i = 0; i < drives.length; i++) {
            ItemStack d = new ItemStack(MOItems.PATTERN_DRIVE.get());
            for (int j = 0; j < 2; j++) MOItems.PATTERN_DRIVE.get().addProgress(d, drives[i][j], progress[i][j]);
            storage.getInventory().setStack(i, d);
        }
        var replicator = (matteroverdrive.block.entity.ReplicatorBlockEntity) level.getBlockEntity(replicatorPos);
        replicator.getEnergy().set(500000);
        replicator.getMatterTank().setMatter(700);
        replicator.getInventory().setStack(matteroverdrive.block.entity.ReplicatorBlockEntity.SHIELDING, new ItemStack(MOItems.TRITANIUM_PLATE.get(), 5));
        replicator.setTask(new matteroverdrive.block.entity.ReplicatorBlockEntity.Task(
                new matteroverdrive.matter.ItemPattern(Items.IRON_INGOT.builtInRegistryHolder(), 100), 12));
        level.setBlockAndUpdate(analyzerPos, MOBlocks.ANALYZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        var analyzer = (matteroverdrive.block.entity.AnalyzerBlockEntity) level.getBlockEntity(analyzerPos);
        analyzer.getEnergy().set(500000);
        analyzer.getInventory().setStack(matteroverdrive.block.entity.AnalyzerBlockEntity.INPUT, new ItemStack(Items.DIAMOND, 3));
        ItemStack drive = new ItemStack(MOItems.PATTERN_DRIVE.get());
        MOItems.PATTERN_DRIVE.get().addProgress(drive, Items.IRON_INGOT, 100);
        analyzer.getInventory().setStack(matteroverdrive.block.entity.AnalyzerBlockEntity.DATABASE, drive);
        // phase 4: an anomaly above the far end with a stabilizer aiming at it from the floor
        level.setBlockAndUpdate(base.offset(0, 3, -10), MOBlocks.GRAVITATIONAL_ANOMALY.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(0, 3, -6), MOBlocks.GRAVITATIONAL_STABILIZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        buildReactor(level, base.offset(0, 0, -13));
        level.setBlockAndUpdate(base.offset(0, 0, -5), MOBlocks.TRITANIUM_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(-2, 0, -5), MOBlocks.DILITHIUM_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(2, 0, -5), MOBlocks.TRITANIUM_BLOCK.get().defaultBlockState());

        InscriberBlockEntity inscriber = (InscriberBlockEntity) level.getBlockEntity(inscriberPos);
        inscriber.getEnergy().set(300000);
        inscriber.getInventory().setStack(InscriberBlockEntity.MAIN, new ItemStack(MOItems.ISOLINEAR_CIRCUIT_MK1.get(), 8));
        inscriber.getInventory().setStack(InscriberBlockEntity.SECONDARY, new ItemStack(Items.GOLD_INGOT, 8));
        inscriber.getInventory().setStack(inscriber.getBatterySlot(), MOItems.BATTERY.get().charged());
        for (int i = 0; i < inscriber.getInventory().size(); i++) {
            if (inscriber.getInventory().spec(i).role() == matteroverdrive.machine.MachineInventory.Role.UPGRADE) {
                inscriber.getInventory().setStack(i, new ItemStack(MOItems.UPGRADE_SPEED.get()));
                break;
            }
        }

        var inv = player.getInventory();
        inv.clearContent();
        ItemStack[] hotbar = {MOItems.BATTERY.get().charged(), new ItemStack(MOItems.BATTERY.get()), MOItems.HC_BATTERY.get().charged(),
                new ItemStack(MOItems.CREATIVE_BATTERY.get()), new ItemStack(MOItems.UPGRADE_SPEED.get()),
                new ItemStack(MOItems.UPGRADE_HYPER_SPEED.get()), new ItemStack(MOItems.TRITANIUM_WRENCH.get()),
                new ItemStack(MOItems.INSCRIBER.get()), new ItemStack(MOItems.SOLAR_PANEL.get())};
        for (int i = 0; i < hotbar.length; i++) inv.setItem(i, hotbar[i]);
        for (int i = 0; i < 8; i++) inv.setItem(9 + i, new ItemStack(MOItems.TAB_ORDER.get(i).get()));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MOItems.TRITANIUM_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MOItems.TRITANIUM_CHESTPLATE.get()));
        inv.setSelectedSlot(6);
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 35f, false);
    }

    /** A complete fusion reactor: controller facing the player, the ring and an anomaly behind it. */
    private static void buildReactor(ServerLevel level, BlockPos controller) {
        for (BlockPos p : BlockPos.betweenClosed(controller.offset(-6, -1, -11), controller.offset(6, -1, 1))) {
            level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : BlockPos.betweenClosed(controller.offset(-6, 0, -11), controller.offset(6, 4, 1))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }
        reactorPos = controller;
        level.setBlockAndUpdate(controller, MOBlocks.FUSION_REACTOR_CONTROLLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        var reactor = (matteroverdrive.block.entity.FusionReactorControllerBlockEntity) level.getBlockEntity(controller);
        for (int i = 0; i < matteroverdrive.block.entity.FusionReactorControllerBlockEntity.POSITION_COUNT; i++) {
            int want = matteroverdrive.block.entity.FusionReactorControllerBlockEntity.BLOCKS[i];
            var block = want == 255 ? MOBlocks.GRAVITATIONAL_ANOMALY.get() : want == 0 ? MOBlocks.MACHINE_HULL.get()
                    : want == 1 ? MOBlocks.FUSION_REACTOR_COIL.get() : MOBlocks.FUSION_REACTOR_IO.get();
            level.setBlockAndUpdate(reactor.getPosition(i), block.defaultBlockState());
            if (level.getBlockEntity(reactor.getPosition(i)) instanceof matteroverdrive.block.entity.GravitationalAnomalyBlockEntity a) {
                a.setMass(100000);
            }
        }
        reactor.getMatterTank().setMatter(1500);
        reactor.getInventory().setStack(reactor.getBatterySlot(), new ItemStack(MOItems.HC_BATTERY.get()));
    }

    /** Gallery: day or night, clear sky, a plain player (not an android, no effects or armor), flying. */
    private static void galleryReset(ServerPlayer p, long time) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(time);
        level.setWeatherParameters(6000, 0, false, false);
        p.removeAllEffects();
        matteroverdrive.android.Android.setAndroid(p, false);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            p.setItemSlot(slot, ItemStack.EMPTY);
        }
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        level.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                "difficulty normal");
    }

    /**
     * Gallery: a 23 x 23 tiled platform on the surface, 1000 + dx blocks east and 1000 south of the spawn; returns its centre on
     * the floor (the first air block). Everything above it is cleared, the ground below filled.
     */
    private static BlockPos gallerySite(ServerPlayer p, int dx) {
        ServerLevel level = p.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        // far from the spawn: fresh terrain, none of the older scene runs' leftovers in view
        int x = spawn.getX() + 1000 + dx, z = spawn.getZ() + 1000;
        level.getChunkSource().getChunk(x >> 4, z >> 4, true);
        // the ground at the platform's corner: the centre may hold the previous run's machines
        level.getChunkSource().getChunk((x - 11) >> 4, (z - 11) >> 4, true);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x - 11, z - 11);
        BlockPos c = new BlockPos(x, y, z);
        for (BlockPos b : BlockPos.betweenClosed(c.offset(-11, -4, -11), c.offset(11, 48, 11))) {
            int dy = b.getY() - y;
            boolean edge = Math.abs(b.getX() - x) == 11 || Math.abs(b.getZ() - z) == 11;
            var state = dy >= 0 ? Blocks.AIR.defaultBlockState()
                    : dy == -1 ? (edge ? matteroverdrive.init.MODecorative.TRITANIUM_PLATE_STRIPE.get() : matteroverdrive.init.MODecorative.FLOOR_TILES.get()).defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
            level.setBlock(b, state, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        }
        level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(c).inflate(14),
                e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
        return c;
    }

    private static void galleryCamera(ServerPlayer p, BlockPos at, double dx, double dy, double dz, float yaw, float pitch) {
        p.teleportTo(p.serverLevel(), at.getX() + dx, at.getY() + dy, at.getZ() + dz, Set.of(), yaw, pitch, false);
    }

    private static ItemStack findWeapon(ServerPlayer p, net.minecraft.world.item.Item item) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) return inv.removeItemNoUpdate(i);
        }
        return ItemStack.EMPTY;
    }

    private static void openMachine(Minecraft mc, BlockPos pos) {
        server(mc, player -> {
            if (player.level().getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                player.openMenu(machine, pos);
            }
        });
    }

    private static void page(Minecraft mc, MachineMenu.Page page) {
        if (mc.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof MachineMenu<?> menu) {
            menu.page = page;
        }
    }

    /** A spot for each building, spread far east of the scene so they don't overlap. */
    private static BlockPos buildingSite(matteroverdrive.world.Building building) {
        return origin.offset(300 + building.ordinal() * 120, 0, 0);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.getMainRenderTarget(),
                msg -> MatterOverdrive.LOGGER.info("[scene] {}", msg.getString()));
    }

    private DevScene() {}
}
