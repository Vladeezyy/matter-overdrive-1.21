package matteroverdrive.gametest;

import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.init.MOBlocks;
import matteroverdrive.starmap.GalacticPosition;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.GalaxyServer;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.Quadrant;
import matteroverdrive.starmap.Star;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Phase 7s checks: galaxy generation, persistence, homeworlds and the star map block. */
final class StarMapGameTests {
    static void addAll() {
        MOGameTests.add("galaxy_generation", 20, false, StarMapGameTests::generation);
        MOGameTests.add("galaxy_homeworld", 20, false, StarMapGameTests::homeworld);
        MOGameTests.add("star_map_menu", 20, false, StarMapGameTests::menu);
        MOGameTests.add("star_map_building", 20, false, StarMapGameTests::building);
        MOGameTests.add("star_map_travel", 20, false, StarMapGameTests::travel);
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, message);
    }

    /** 1.7.10 defaults: 2048-2303 stars in 27 quadrants, 1-3 planets each; the same seed gives the same galaxy; NBT round trip. */
    private static void generation(GameTestHelper helper) {
        Galaxy galaxy = GalaxyServer.createGalaxy(12345L);
        Galaxy again = GalaxyServer.createGalaxy(12345L);
        int stars = galaxy.getStarCount();
        check(helper, galaxy.getQuadrants().size() == 27, "quadrants " + galaxy.getQuadrants().size());
        check(helper, stars >= 2000 && stars < 2304, "stars " + stars);
        for (Quadrant quadrant : galaxy.getQuadrants()) {
            for (Star star : quadrant.getStars()) {
                check(helper, !star.getName().isBlank() && star.getPlanets().size() >= 1 && star.getPlanets().size() <= 3,
                        "star " + star.getName() + " planets " + star.getPlanets().size());
                check(helper, Math.abs(star.getX()) <= 1 && Math.abs(star.getY()) <= 1 && Math.abs(star.getZ()) <= 1, "star outside " + star.getPosition());
                for (Planet planet : star.getPlanets()) {
                    check(helper, planet.getType() == 2 && planet.getSize() > 0 && planet.getBuildingSpaces() > 0, "planet " + planet.getName());
                    check(helper, planet.getName().equals(star.getName() + " " + planet.getId()), "planet name " + planet.getName());
                }
            }
        }
        var registries = helper.getLevel().registryAccess();
        check(helper, galaxy.toNBT(registries).equals(again.toNBT(registries)), "not deterministic");
        Galaxy loaded = new Galaxy();
        loaded.readNBT(galaxy.toNBT(registries), registries);
        check(helper, loaded.toNBT(registries).equals(galaxy.toNBT(registries)) && loaded.getVersion() == GalaxyServer.GALAXY_VERSION,
                "NBT round trip");
        check(helper, !galaxy.toNBT(registries).equals(GalaxyServer.createGalaxy(54321L).toNBT(registries)), "seed ignored");
        helper.succeed();
    }

    /** The menu's 4 slots are the selected planet's construction slots; on someone else's planet nothing goes in or out. */
    private static void menu(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        GalaxyServer.tryAndClaimPlanet(player);
        Planet home = GalaxyServer.getHomeworld(player);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, MOBlocks.STAR_MAP.get());
        var starMap = helper.<StarMapBlockEntity>getBlockEntity(pos);
        starMap.onPlaced(player);
        var menu = new matteroverdrive.menu.StarMapMenu(0, player.getInventory(), starMap);
        check(helper, menu.slots.size() == 4 + 36, "slots " + menu.slots.size());
        var diamond = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
        check(helper, !menu.getSlot(0).mayPlace(diamond), "only buildings / ships go in");
        home.setStackInSlot(2, diamond);
        check(helper, menu.getSlot(2).getItem().is(net.minecraft.world.item.Items.DIAMOND) && menu.getSlot(2).mayPickup(player), "planet slot");
        // someone else's planet: the slots show it but can't be taken from
        Planet other = null;
        for (Quadrant quadrant : GalaxyServer.getGalaxy().getQuadrants()) {
            for (Star star : quadrant.getStars()) for (Planet planet : star.getPlanets()) if (!planet.hasOwner() && other == null) other = planet;
        }
        other.setOwnerUUID(java.util.UUID.randomUUID());
        starMap.setDestination(GalacticPosition.of(other));
        check(helper, !menu.getSlot(0).mayPickup(player) && menu.getSlot(2).getItem().isEmpty(), "someone else's planet");
        other.setOwnerUUID(null);
        // no planet selected: the map's own slots
        starMap.setDestination(GalacticPosition.NONE);
        check(helper, menu.getSlot(2).getItem().isEmpty() && menu.getSlot(2).mayPickup(player), "the map's own slots");
        home.setStackInSlot(2, net.minecraft.world.item.ItemStack.EMPTY);
        player.discard();
        helper.succeed();
    }

    private static net.minecraft.world.item.ItemStack aged(GameTestHelper helper, net.minecraft.world.item.Item item) {
        var stack = new net.minecraft.world.item.ItemStack(item);
        ((matteroverdrive.starmap.Buildable) item).setBuildStart(stack, helper.getLevel().getGameTime() - 1_000_000);
        return stack;
    }

    /** Construction slots: buildings need a base, ships a factory; finished ones join the planet, owned by its owner. */
    private static void building(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        GalaxyServer.tryAndClaimPlanet(player);
        Planet home = GalaxyServer.getHomeworld(player);
        var level = helper.getLevel();
        check(helper, home.hasBuildingType(matteroverdrive.starmap.BuildingType.BASE) && home.getFleet().size() == 1
                && home.getFleet().get(0).is(matteroverdrive.init.MOItems.SCOUT_SHIP.get()), "homeworld base + scout " + home.getFleet());
        check(helper, home.getBuildingSpaces() == 10, "8 + 2 from the base: " + home.getBuildingSpaces());
        // a ship without a factory can't be built: its clock restarts
        home.setStackInSlot(2, aged(helper, matteroverdrive.init.MOItems.SCOUT_SHIP.get()));
        home.update(level);
        var waiting = home.getStackInSlot(2);
        check(helper, !waiting.isEmpty() && ((matteroverdrive.starmap.Buildable) waiting.getItem()).getBuildStart(waiting) == level.getGameTime(),
                "ship built without a factory");
        home.setStackInSlot(0, aged(helper, matteroverdrive.init.MOItems.SHIP_FACTORY.get()));
        home.setStackInSlot(1, aged(helper, matteroverdrive.init.MOItems.BUILDING_RESIDENTIAL.get()));
        home.update(level);
        check(helper, home.getStackInSlot(0).isEmpty() && home.hasBuildingType(matteroverdrive.starmap.BuildingType.SHIP_FACTORY)
                && home.getBuildings().size() == 3, "buildings " + home.getBuildings());
        var factory = home.getBuildings().get(1);
        check(helper, player.getUUID().equals(((matteroverdrive.starmap.Buildable) factory.getItem()).getOwnerID(factory)), "owner");
        check(helper, home.getPopulation() == 10000 && home.getPowerProduction() == -4 && home.getBuildingSpaces() == 14
                && Math.abs(home.getHappiness() - (-0.4f - 0.6f)) < 1e-4, "stats pop " + home.getPopulation() + " power " + home.getPowerProduction()
                + " spaces " + home.getBuildingSpaces() + " happiness " + home.getHappiness());
        // the ship now has a factory (and a fleet space: 1.7.10 compared the fleet with itself)
        home.setStackInSlot(2, aged(helper, matteroverdrive.init.MOItems.SCOUT_SHIP.get()));
        home.update(level);
        check(helper, home.getStackInSlot(2).isEmpty() && home.getFleet().size() == 2, "ship not built " + home.getFleet());
        // a second base can't be built
        var info = new java.util.ArrayList<Component>();
        var base = new net.minecraft.world.item.ItemStack(matteroverdrive.init.MOItems.BUILDING_BASE.get());
        check(helper, !home.canBuild((matteroverdrive.starmap.BuildingItem) base.getItem(), base, info) && !info.isEmpty(), "second base " + info);
        player.discard();
        helper.succeed();
    }

    /** A scout flies to another planet and arrives; a colonizer claims a free planet with a base. */
    private static void travel(GameTestHelper helper) {
        ServerPlayer player = AndroidGameTests.player(helper);
        GalaxyServer.tryAndClaimPlanet(player);
        Planet home = GalaxyServer.getHomeworld(player);
        var level = helper.getLevel();
        Galaxy galaxy = GalaxyServer.getGalaxy();
        Planet target = null;
        for (Quadrant quadrant : galaxy.getQuadrants()) {
            for (Star star : quadrant.getStars()) {
                for (Planet planet : star.getPlanets()) if (!planet.hasOwner() && planet.getFleet().isEmpty() && target == null) target = planet;
            }
        }
        var from = GalacticPosition.of(home);
        var to = GalacticPosition.of(target);
        var scout = GalaxyServer.createTravelEvent(level, from, to, 0);
        check(helper, scout != null && home.getFleet().isEmpty() && galaxy.getTravelEvents().contains(scout), "scout didn't leave");
        check(helper, scout.getTimeLength() > 0, "travel time " + scout.getTimeLength());
        scout.setTimeStart(level.getGameTime() - 10_000_000);
        galaxy.update(level);
        check(helper, target.getFleet().size() == 1 && !galaxy.getTravelEvents().contains(scout), "scout didn't arrive " + target.getFleet());
        // back home, then a colonizer from home claims the target
        var colonizer = new net.minecraft.world.item.ItemStack(matteroverdrive.init.MOItems.COLONIZER_SHIP.get());
        ((matteroverdrive.starmap.Buildable) colonizer.getItem()).setOwner(colonizer, player.getUUID());
        home.addShip(colonizer);
        var event = GalaxyServer.createTravelEvent(level, from, to, home.getFleet().size() - 1);
        check(helper, event != null, "colonizer didn't leave");
        event.setTimeStart(level.getGameTime() - 10_000_000);
        galaxy.update(level);
        check(helper, target.isOwner(player) && target.hasBuildingType(matteroverdrive.starmap.BuildingType.BASE) && target.getFleet().size() == 1,
                "not colonized: owner " + target.getOwnerUUID() + " fleet " + target.getFleet());
        // an owned homeworld only takes its owner's ships
        var stranger = new net.minecraft.world.item.ItemStack(matteroverdrive.init.MOItems.SCOUT_SHIP.get());
        check(helper, home.canAddShip(stranger, player) && !home.canAddShip(stranger, AndroidGameTests.player(helper)), "homeworld fleet");
        player.discard();
        helper.succeed();
    }

    /** A new player gets a homeworld (8 building / 10 fleet spaces); a star map placed by them shows it and zooms 0-4. */
    private static void homeworld(GameTestHelper helper) {
        check(helper, GalaxyServer.getGalaxy() != null, "no server galaxy");
        ServerPlayer player = AndroidGameTests.player(helper);
        GalaxyServer.tryAndClaimPlanet(player);
        Planet home = GalaxyServer.getHomeworld(player);
        check(helper, home != null && home.isHomeworld(player) && home.getBuildingSpaces() == 10 && home.getFleetSpaces() == 10, "homeworld " + home);
        check(helper, !GalaxyServer.tryAndClaimPlanet(player) && GalaxyServer.getHomeworld(player) == home, "claimed twice");
        for (Planet planet : home.getStar().getPlanets()) {
            check(helper, planet == home || !planet.hasOwner() || planet.isOwner(player), "someone else's system");
        }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, MOBlocks.STAR_MAP.get());
        var starMap = helper.<StarMapBlockEntity>getBlockEntity(pos);
        starMap.onPlaced(player);
        check(helper, starMap.getGalaxyPosition().equals(GalacticPosition.of(home)) && starMap.getDestination().equals(starMap.getGalaxyPosition()),
                "position " + starMap.getGalaxyPosition());
        check(helper, player.getUUID().equals(starMap.getOwner()), "the placer owns the map");
        int[] zooms = new int[6];
        for (int i = 0; i < 6; i++) {
            zooms[i] = starMap.getZoomLevel();
            starMap.zoom();
        }
        check(helper, java.util.Arrays.equals(zooms, new int[] {0, 1, 2, 3, 4, 0}), "zoom " + java.util.Arrays.toString(zooms));
        starMap.setDestination(GalacticPosition.of(home.getStar()));
        starMap.setZoomLevel(2);
        starMap.zoom();
        check(helper, starMap.getZoomLevel() == 0, "a star only zooms to 2");
        player.discard();
        helper.succeed();
    }
}
