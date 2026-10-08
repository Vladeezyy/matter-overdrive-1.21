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
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        helper.assertTrue(ok, Component.literal(message));
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
        var starMap = helper.getBlockEntity(pos, StarMapBlockEntity.class);
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

    /** A new player gets a homeworld (8 building / 10 fleet spaces); a star map placed by them shows it and zooms 0-4. */
    private static void homeworld(GameTestHelper helper) {
        check(helper, GalaxyServer.getGalaxy() != null, "no server galaxy");
        ServerPlayer player = AndroidGameTests.player(helper);
        GalaxyServer.tryAndClaimPlanet(player);
        Planet home = GalaxyServer.getHomeworld(player);
        check(helper, home != null && home.isHomeworld(player) && home.getBuildingSpaces() == 8 && home.getFleetSpaces() == 10, "homeworld " + home);
        check(helper, !GalaxyServer.tryAndClaimPlanet(player) && GalaxyServer.getHomeworld(player) == home, "claimed twice");
        for (Planet planet : home.getStar().getPlanets()) {
            check(helper, planet == home || !planet.hasOwner() || planet.isOwner(player), "someone else's system");
        }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, MOBlocks.STAR_MAP.get());
        var starMap = helper.getBlockEntity(pos, StarMapBlockEntity.class);
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
