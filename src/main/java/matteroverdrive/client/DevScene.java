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
        at(358, mc -> server(mc, p -> p.teleportTo(p.level(), p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 1.5, Set.of(), 140f, 30f, false)));
        at(366, mc -> shot(mc, "pipes"));
        at(368, mc -> server(mc, p -> p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 3.5, Set.of(), 180f, 15f, false)));
        at(378, mc -> shot(mc, "network"));
        at(379, mc -> server(mc, p -> p.teleportTo(p.level(), origin.getX() + 3.5, origin.getY() + 3, origin.getZ() - 7.5, Set.of(), 120f, 5f, false)));
        at(386, mc -> shot(mc, "anomaly"));
        at(387, mc -> openMachine(mc, reactorPos));
        at(400, mc -> shot(mc, "reactor_gui"));
        at(402, mc -> mc.player.closeContainer());
        at(404, mc -> server(mc, p -> p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY() + 14, origin.getZ() - 6.5, Set.of(), 180f, 60f, false)));
        at(414, mc -> shot(mc, "reactor"));
        // phase 5: weapons in hand, a bolt in flight, inventory icons
        at(416, mc -> server(mc, p -> {
            p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
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
                var bolt = new matteroverdrive.entity.PlasmaBolt(p.level(), p, 0, 64, matteroverdrive.item.weapon.WeaponColorModuleItem.COLORS[i]);
                var look = p.getLookAngle();
                bolt.setPos(p.getX() + (i - 1) * 0.8, p.getEyeY() - 0.2, p.getZ() - 2.5);
                bolt.shoot(look.x, look.y, look.z, 0.005f, 0);
                p.level().addFreshEntity(bolt);
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
            p.teleportTo(p.level(), p.getX(), p.getY(), p.getZ(), Set.of(), 180f, 25f, false);
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
        at(473, mc -> server(mc, p -> p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 35f, false)));
        // phase 5c: weapon station with a fully fitted rifle, its GUI, then a phaser (no sights slot) with a barrel
        at(475, mc -> server(mc, p -> {
            BlockPos station = origin.offset(0, 0, -2);
            p.level().setBlockAndUpdate(station, MOBlocks.WEAPON_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 0, MOItems.HC_BATTERY.get().charged());
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 1, new ItemStack(MOItems.COLOR_MODULES.get(0).get()));
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 2, new ItemStack(MOItems.BARREL_FIRE.get()));
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(rifle, 3, new ItemStack(MOItems.SNIPER_SCOPE.get()));
            ((matteroverdrive.block.entity.WeaponStationBlockEntity) p.level().getBlockEntity(station)).getInventory()
                    .setStack(matteroverdrive.block.entity.WeaponStationBlockEntity.WEAPON, rifle);
            p.getInventory().add(new ItemStack(MOItems.BARREL_DAMAGE.get()));
            p.getInventory().add(new ItemStack(MOItems.SNIPER_SCOPE.get()));
            p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 30f, false);
        }));
        at(484, mc -> shot(mc, "weapon_station"));
        at(485, mc -> openMachine(mc, origin.offset(0, 0, -2)));
        at(492, mc -> shot(mc, "weapon_station_gui"));
        at(493, mc -> server(mc, p -> {
            ItemStack phaser = new ItemStack(MOItems.PHASER.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(phaser, 2, new ItemStack(MOItems.BARREL_EXPLOSION.get()));
            ((matteroverdrive.block.entity.WeaponStationBlockEntity) p.level().getBlockEntity(origin.offset(0, 0, -2))).getInventory()
                    .setStack(matteroverdrive.block.entity.WeaponStationBlockEntity.WEAPON, phaser);
        }));
        at(498, mc -> shot(mc, "weapon_station_phaser"));
        at(499, mc -> mc.setScreen(null));
        at(500, mc -> server(mc, p -> {
            ItemStack phaser = new ItemStack(MOItems.PHASER.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(phaser, 32000);
            matteroverdrive.item.weapon.EnergyWeaponItem.setModule(phaser, 2, new ItemStack(MOItems.BARREL_HEAL.get()));
            p.getInventory().setItem(0, phaser);
            p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 0f, false);
        }));
        at(508, mc -> shot(mc, "phaser_barrel"));
        // phase 6: become an android, buy a few stats, fit parts, android station GUI
        at(510, mc -> server(mc, p -> {
            BlockPos station = origin.offset(0, 0, -2);
            p.level().setBlock(station, MOBlocks.ANDROID_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH),
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
            p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), 180f, 30f, false);
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
            var pig = net.minecraft.world.entity.EntityType.PIG.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            pig.setPos(origin.getX() + 3.5, origin.getY(), origin.getZ() - 4.5);
            p.level().addFreshEntity(pig);
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
            p.level().setBlockAndUpdate(charger, state);
            state.getBlock().setPlacedBy(p.level(), charger, state, p, ItemStack.EMPTY);
        }));
        at(558, mc -> shot(mc, "charging_station"));
        // 7a: every decorative block in a wall, tritanium glass, a holo sign with text
        at(560, mc -> server(mc, p -> {
            var blocks = matteroverdrive.init.MODecorative.ALL;
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos at = origin.offset(-9 + i % 19, i / 19, 3);
                p.level().setBlockAndUpdate(at, blocks.get(i).get().defaultBlockState());
            }
            BlockPos wall = origin.offset(0, 2, 3);
            p.level().setBlockAndUpdate(wall, matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
            BlockPos sign = wall.north();
            p.level().setBlockAndUpdate(sign, MOBlocks.HOLO_SIGN.get().defaultBlockState()
                    .setValue(matteroverdrive.block.HoloSignBlock.FACING, Direction.NORTH));
            if (p.level().getBlockEntity(sign) instanceof matteroverdrive.block.entity.HoloSignBlockEntity holo) {
                holo.setText("Matter\nOverdrive\n1.21.10");
            }
            p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 4.5, Set.of(), 0f, 10f, false);
        }));
        at(568, mc -> shot(mc, "decorative"));
        // 7b: rogue androids of each level (one legendary), melee and ranged
        at(570, mc -> server(mc, p -> {
            int[][] spec = {{0, 0}, {1, 0}, {2, 0}, {3, 1}};
            for (int i = 0; i < 4; i++) {
                for (boolean ranged : new boolean[] {false, true}) {
                    var type = ranged ? matteroverdrive.init.MOEntities.RANGED_ROGUE_ANDROID.get() : matteroverdrive.init.MOEntities.ROGUE_ANDROID.get();
                    var android = type.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    android.snapTo(origin.getX() - 4.5 + i * 3 + (ranged ? 1.2 : 0), origin.getY(), origin.getZ() - 8.5, 0, 0);
                    android.finalizeSpawn(p.level(), p.level().getCurrentDifficultyAt(android.blockPosition()),
                            net.minecraft.world.entity.EntitySpawnReason.COMMAND, null);
                    android.setup(spec[i][0], spec[i][1] == 1);
                    android.setNoAi(true);
                    android.setYRot(0);
                    android.setYHeadRot(0);
                    android.yBodyRot = 0;
                    p.level().addFreshEntity(android);
                }
            }
            p.teleportTo(p.level(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() - 1.5, Set.of(), 180f, 10f, false);
        }));
        at(578, mc -> shot(mc, "rogue_androids"));
        // 7c: failed animals (adults and a piglet) on a platform above the scene
        at(580, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            for (int x = -7; x <= 7; x++) {
                for (int z = -10; z <= 1; z++) {
                    p.level().setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 0; y < 4; y++) p.level().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
            var types = java.util.List.of(matteroverdrive.init.MOEntities.FAILED_PIG.get(), matteroverdrive.init.MOEntities.FAILED_COW.get(),
                    matteroverdrive.init.MOEntities.FAILED_CHICKEN.get(), matteroverdrive.init.MOEntities.FAILED_SHEEP.get(),
                    matteroverdrive.init.MOEntities.FAILED_PIG.get());
            for (int i = 0; i < types.size(); i++) {
                var animal = types.get(i).create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                animal.snapTo(base.getX() - 4.5 + i * 2.4, base.getY(), base.getZ() - 6.5, 0, 0);
                if (i == 4) animal.setAge(-24000);
                animal.setNoAi(true);
                animal.setYRot(-40);
                animal.setYHeadRot(-40);
                animal.yBodyRot = -40;
                p.level().addFreshEntity(animal);
            }
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 20f, false);
        }));
        at(588, mc -> shot(mc, "failed_animals"));
        // 7d: the mutant scientist on the same platform
        at(590, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            p.level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, p.getBoundingBox().inflate(16)).forEach(e -> e.discard());
            var mutant = matteroverdrive.init.MOEntities.MUTANT_SCIENTIST.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            mutant.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 4.5, 0, 0);
            mutant.setNoAi(true);
            mutant.setYRot(25);
            mutant.setYHeadRot(25);
            mutant.yBodyRot = 25;
            p.level().addFreshEntity(mutant);
        }));
        at(598, mc -> shot(mc, "mutant_scientist"));
        // 7e: the 16 tritanium crates in two rows, facing the camera
        at(600, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            p.level().getEntitiesOfClass(matteroverdrive.entity.monster.MutantScientist.class, p.getBoundingBox().inflate(16)).forEach(e -> e.discard());
            for (int i = 0; i < 16; i++) {
                BlockPos pos = base.offset(-4 + i % 8, i < 8 ? 0 : 1, -5 - (i < 8 ? 0 : 1));
                if (i >= 8) p.level().setBlockAndUpdate(pos.below(), matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
                p.level().setBlockAndUpdate(pos, matteroverdrive.init.MOBlocks.TRITANIUM_CRATES.get(i).get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 25f, false);
        }));
        at(608, mc -> shot(mc, "tritanium_crates"));
        // 7f: every image building placed on the terrain far east of the scene, seen from above a corner
        var buildings = matteroverdrive.world.Building.values();
        for (int i = 0; i < buildings.length; i++) {
            var building = buildings[i];
            int t0 = 610 + i * 80;
            at(t0, mc -> server(mc, p -> {
                BlockPos site = buildingSite(building);
                p.teleportTo(p.level(), site.getX(), 200, site.getZ(), Set.of(), 0f, 0f, false);
            }));
            at(t0 + 60, mc -> server(mc, p -> {
                var t = building.template();
                BlockPos site = buildingSite(building);
                int y = p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.getX(), site.getZ()) - 3;
                BlockPos at = new BlockPos(site.getX(), y, site.getZ());
                var piece = new matteroverdrive.world.ImageStructurePiece(building, at, 42);
                piece.postProcess(p.level(), p.level().structureManager(), p.level().getChunkSource().getGenerator(), p.level().getRandom(),
                        piece.getBoundingBox(), new net.minecraft.world.level.ChunkPos(at), at);
                double cx = at.getX() + t.width() / 2.0, cy = at.getY() + t.height() / 2.0, cz = at.getZ() + t.depth() / 2.0;
                double size = Math.max(t.width(), t.depth());
                double ex = cx - size * 0.75, ey = cy + size * 0.55 + 4, ez = cz - size * 0.75;
                float yaw = (float) Math.toDegrees(Math.atan2(-(cx - ex), cz - ez));
                float pitch = (float) Math.toDegrees(Math.atan2(ey - cy, Math.hypot(cx - ex, cz - ez)));
                p.teleportTo(p.level(), ex, ey, ez, Set.of(), yaw, pitch, false);
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
                p.level().getServer().getCommands().performPrefixedCommand(p.level().getServer().createCommandSourceStack(),
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
                var registry = p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                var holder = registry.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("matteroverdrive", id)));
                var result = p.level().getChunkSource().getGenerator().findNearestMapStructure(p.level(),
                        net.minecraft.core.HolderSet.direct(holder), first ? searchFrom : found0[0], 100, false);
                if (result == null) return;
                found[0] = result.getFirst();
                p.teleportTo(p.level(), found[0].getX(), 160, found[0].getZ(), Set.of(), 0f, 0f, false);
            }));
            at(t0 + 120, mc -> server(mc, p -> {
                if (found[0] == null) return;
                var holder = p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                        .getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("matteroverdrive", id));
                var start = p.level().structureManager().getStructureAt(
                        new BlockPos(found[0].getX(), p.level().getMinY() + 1, found[0].getZ()), holder);
                BlockPos lookup = found[0];
                if (!start.isValid()) {
                    for (var s2 : p.level().structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(lookup), st -> st == holder)) {
                        start = s2;
                    }
                }
                if (!start.isValid()) {
                    MatterOverdrive.LOGGER.info("[scene] {} near {}: no start", id, lookup);
                    return;
                }
                var box = start.getBoundingBox();
                var aabb = AABB.of(box).inflate(4);
                int androids = p.level().getEntitiesOfClass(matteroverdrive.entity.monster.RogueAndroid.class, aabb).size();
                int mutants = p.level().getEntitiesOfClass(matteroverdrive.entity.monster.MutantScientist.class, aabb).size();
                int crates = 0, lootCrates = 0;
                for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                    if (p.level().getBlockEntity(pos) instanceof matteroverdrive.block.entity.TritaniumCrateBlockEntity crate) {
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
                p.teleportTo(p.level(), ex, ey, ez, Set.of(), yaw, pitch, false);
            }));
            at(t0 + 121, mc -> mc.options.hideGui = true);
            at(t0 + 150, mc -> shot(mc, "natural_" + id));
            at(t0 + 151, mc -> mc.options.hideGui = false);
        }
        // 7g: a Matter Plasma pool on the platform, the containers in the hotbar
        at(1340, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 35f, false);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-4, 0, -9), base.offset(4, 1, -4))) {
                p.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, -1, -7), base.offset(2, -1, -5))) {
                p.level().setBlockAndUpdate(pos, matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
                p.level().setBlockAndUpdate(pos.below(), matteroverdrive.init.MODecorative.TRITANIUM_PLATE.get().defaultBlockState());
                p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.level().setBlockAndUpdate(base.offset(0, -1, -6), matteroverdrive.init.MOBlocks.MATTER_PLASMA.get().defaultBlockState());
            p.level().setBlockAndUpdate(base.offset(-1, -1, -6), matteroverdrive.init.MOBlocks.MATTER_PLASMA.get().defaultBlockState());
            p.getInventory().setItem(0, new ItemStack(MOItems.MATTER_CONTAINER.get(), 8));
            p.getInventory().setItem(1, new ItemStack(MOItems.MATTER_CONTAINER_FULL.get(), 3));
        }));
        at(1400, mc -> shot(mc, "matter_plasma"));
        // 7h: the omni tool digging a wall 6 blocks away
        at(1402, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, 0, -7), base.offset(2, 2, -7))) {
                p.level().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            ItemStack tool = new ItemStack(MOItems.OMNI_TOOL.get());
            matteroverdrive.item.weapon.EnergyWeaponItem.setEnergy(tool, 32000);
            p.getInventory().setItem(0, tool);
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 0f, false);
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
                p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.level().setBlockAndUpdate(base.offset(-1, 0, -4), MOBlocks.MICROWAVE.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            p.level().setBlockAndUpdate(base.offset(1, 0, -4), MOBlocks.MICROWAVE.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST));
            ItemStack decomposer = new ItemStack(MOItems.PORTABLE_DECOMPOSER.get());
            decomposer.set(matteroverdrive.init.MODataComponents.ENERGY.get(), 90000);
            p.getInventory().setItem(0, decomposer);
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() - 1.0, Set.of(), 180f, 40f, false);
        }));
        at(1450, mc -> shot(mc, "microwave"));
        // 7j: a matter scanner linked to a pattern storage with a few patterns, its screen open
        at(1452, mc -> server(mc, p -> {
            BlockPos base = origin.above(14);
            BlockPos storagePos = base.offset(2, 0, -5);
            p.level().setBlockAndUpdate(storagePos, MOBlocks.PATTERN_STORAGE.get().defaultBlockState());
            if (p.level().getBlockEntity(storagePos) instanceof matteroverdrive.block.entity.PatternStorageBlockEntity storage) {
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
            matteroverdrive.item.MatterScannerItem.link(scanner, p.level(), storagePos);
            matteroverdrive.item.MatterScannerItem.select(p.level(), scanner, net.minecraft.world.item.Items.IRON_BLOCK);
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
                p.level().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.level().setBlockAndUpdate(pad, MOBlocks.TRANSPORTER.get().defaultBlockState());
            if (p.level().getBlockEntity(pad) instanceof matteroverdrive.block.entity.TransporterBlockEntity t) {
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
            if (p.level().getBlockEntity(pad) instanceof matteroverdrive.block.entity.TransporterBlockEntity t) {
                p.openMenu(t, buf -> buf.writeBlockPos(pad));
            }
        }));
        at(1486, mc -> shot(mc, "transporter_gui"));
        at(1485, mc -> server(mc, p -> {
            p.closeContainer();
            BlockPos base = origin.above(14);
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() - 3.5, Set.of(), 180f, 30f, false);
        }));
        at(1488, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        at(1540, mc -> shot(mc, "transporter_transport"));
        at(1541, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        // 7l: an android spawner on a team with a red colour module and a path; its screen (home + config), then the androids
        at(1544, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -1, -14), base.offset(8, 5, 2))) {
                p.level().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
            var scoreboard = p.level().getScoreboard();
            if (scoreboard.getPlayerTeam("rogues") == null) scoreboard.addPlayerTeam("rogues").setColor(net.minecraft.ChatFormatting.RED);
            BlockPos spawnerPos = base.offset(-2, -1, -5);
            p.level().setBlockAndUpdate(spawnerPos, MOBlocks.ANDROID_SPAWNER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (p.level().getBlockEntity(spawnerPos) instanceof matteroverdrive.block.entity.AndroidSpawnerBlockEntity s) {
                s.getInventory().setStack(matteroverdrive.block.entity.AndroidSpawnerBlockEntity.COLOR_MODULE_SLOT,
                        new ItemStack(MOItems.COLOR_MODULES.get(0).get()));
                ItemStack drive = new ItemStack(MOItems.TRANSPORT_FLASH_DRIVE.get());
                drive.set(matteroverdrive.init.MODataComponents.TRANSPORT_TARGET.get(), base.offset(3, -1, -6));
                s.getInventory().setStack(matteroverdrive.block.entity.AndroidSpawnerBlockEntity.FLASH_DRIVE_SLOT_START, drive);
                s.setConfig(4, 4, 0, "rogues");
            }
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 25f, false);
        }));
        at(1560, mc -> server(mc, p -> {
            BlockPos spawnerPos = origin.above(30).offset(-2, -1, -5);
            if (p.level().getBlockEntity(spawnerPos) instanceof matteroverdrive.block.entity.AndroidSpawnerBlockEntity s) {
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
            if (p.level().getBlockEntity(spawnerPos) instanceof matteroverdrive.block.entity.AndroidSpawnerBlockEntity s) s.removeAllAndroids();
        }));
        // 7m: the mad scientist: a human's conversation, taking Puny Humans (HUD "Started"), the junkie's cocktail story, trades
        at(1636, mc -> server(mc, p -> {
            BlockPos base = origin.above(30);
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -1, -14), base.offset(8, 5, 2))) {
                p.level().setBlock(pos, pos.getY() < base.getY() ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
            p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(base).inflate(20)).forEach(e -> e.discard());
            matteroverdrive.quest.PlayerQuests.get(p).getActiveQuests().clear();
            matteroverdrive.quest.PlayerQuests.get(p).getCompletedQuests().clear();
            matteroverdrive.quest.PlayerQuests.sync(p);
            matteroverdrive.android.Android.setAndroid(p, false);
            p.teleportTo(p.level(), base.getX() + 0.5, base.getY(), base.getZ() + 0.5, Set.of(), 180f, 5f, false);
            var npc = matteroverdrive.init.MOEntities.MAD_SCIENTIST.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            npc.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 2.5, 0, 0);
            npc.setJunkie(false);
            npc.setNoAi(true);
            p.level().addFreshEntity(npc);
        }));
        at(1644, mc -> server(mc, p -> p.level().getEntitiesOfClass(matteroverdrive.entity.MadScientist.class, p.getBoundingBox().inflate(6))
                .forEach(npc -> matteroverdrive.dialog.DialogPayloads.startConversation(p, npc))));
        at(1652, mc -> shot(mc, "dialog_human"));
        at(1653, mc -> {
            if (mc.screen instanceof matteroverdrive.client.quest.DialogScreen d) d.choose(0);
        });
        at(1680, mc -> shot(mc, "dialog_puny_humans"));
        at(1681, mc -> mc.setScreen(null));
        at(1684, mc -> server(mc, p -> p.level().getEntitiesOfClass(matteroverdrive.entity.MadScientist.class, p.getBoundingBox().inflate(6))
                .forEach(npc -> {
                    npc.setJunkie(true);
                    matteroverdrive.dialog.DialogPayloads.startConversation(p, npc);
                })));
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
        at(1764, mc -> mc.stop());
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
        ServerLevel level = player.level();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        player.setGameMode(GameType.CREATIVE);
        BlockPos base = player.blockPosition();
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
        Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.getMainRenderTarget(), 1,
                msg -> MatterOverdrive.LOGGER.info("[scene] {}", msg.getString()));
    }

    private DevScene() {}
}
