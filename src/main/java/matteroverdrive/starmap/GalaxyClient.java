package matteroverdrive.starmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 1.7.10 GalaxyClient: the galaxy as the server sent it (stars without planets; a star's planets are asked for when it's
 * looked at, and changed planets are sent as they change). Holds no client classes so the payload handlers can use it.
 */
public final class GalaxyClient {
    private static @Nullable Galaxy galaxy;
    private static final Map<UUID, Planet> homePlanets = new HashMap<>();
    private static final Set<Long> requestedStars = new HashSet<>();

    private GalaxyClient() {}

    public static @Nullable Galaxy getGalaxy() {
        return galaxy;
    }

    public static void setGalaxy(@Nullable Galaxy newGalaxy) {
        galaxy = newGalaxy;
        requestedStars.clear();
        homePlanets.clear();
        if (galaxy != null) GalaxyServer.loadClaimedPlanets(galaxy, homePlanets);
    }

    public static void loadClaimedPlanets() {
        if (galaxy != null) GalaxyServer.loadClaimedPlanets(galaxy, homePlanets);
    }

    public static @Nullable Planet getHomeworld(Player player) {
        return homePlanets.get(player.getUUID());
    }

    public static @Nullable Quadrant getQuadrant(GalacticPosition position) {
        return galaxy == null ? null : galaxy.getQuadrant(position);
    }

    public static @Nullable Star getStar(GalacticPosition position) {
        return galaxy == null ? null : galaxy.getStar(position);
    }

    public static @Nullable Planet getPlanet(GalacticPosition position) {
        return galaxy == null ? null : galaxy.getPlanet(position);
    }

    /** Asks the server once for the star's planets (1.7.10 PacketStarLoading). */
    public static void requestPlanets(@Nullable Star star) {
        if (star == null || star.getQuadrant() == null) return;
        long key = (long) star.getQuadrant().getId() << 32 | star.getId() & 0xFFFFFFFFL;
        if (requestedStars.add(key)) {
            ClientPacketDistributor.sendToServer(new StarMapPayloads.StarRequest(star.getQuadrant().getId(), star.getId()));
        }
    }

    /** 1.7.10 canSeePlanetInfo: own planets (creative sees all); ship owners see their ships' planets (phase 7u). */
    public static boolean canSeePlanetInfo(Planet planet, Player player) {
        return planet.isOwner(player) || player.getAbilities().instabuild;
    }

    public static boolean canSeeStarInfo(Star star, Player player) {
        for (Planet planet : star.getPlanets()) if (canSeePlanetInfo(planet, player)) return true;
        return false;
    }
}
