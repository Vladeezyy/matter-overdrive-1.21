package matteroverdrive.starmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.starmap.gen.GalaxyGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 GalaxyServer: the overworld's galaxy, generated from the world seed into galaxy.dat in the world folder,
 * saved with the world when it changed, updated every overworld tick. A joining player gets the galaxy and, the
 * first time, a homeworld.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID)
public final class GalaxyServer {
    public static final int GALAXY_VERSION = 1;
    private static @Nullable Galaxy galaxy;
    private static final Map<UUID, Planet> homePlanets = new HashMap<>();
    private static final Random random = new Random();
    private static final GalaxyGenerator generator = new GalaxyGenerator();

    private GalaxyServer() {}

    public static @Nullable Galaxy getGalaxy() {
        return galaxy;
    }

    public static void setGalaxy(@Nullable Galaxy newGalaxy) {
        galaxy = newGalaxy;
        homePlanets.clear();
        if (galaxy != null) loadClaimedPlanets(galaxy, homePlanets);
    }

    static void loadClaimedPlanets(Galaxy galaxy, Map<UUID, Planet> homePlanets) {
        homePlanets.clear();
        for (Quadrant quadrant : galaxy.getQuadrants()) {
            for (Star star : quadrant.getStars()) {
                for (Planet planet : star.getPlanets()) {
                    if (planet.isHomeworld() && planet.hasOwner()) homePlanets.put(planet.getOwnerUUID(), planet);
                }
            }
        }
    }

    public static @Nullable Planet getHomeworld(Player player) {
        return homePlanets.get(player.getUUID());
    }

    public static GalaxyGenerator getGenerator() {
        return generator;
    }

    // --- load / save -------------------------------------------------------------------------------

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("galaxy.dat");
    }

    /** 1.7.10 createGalaxy: id = dimension 0, seed = the world seed. */
    public static Galaxy createGalaxy(long seed) {
        Galaxy created = generator.generateGalaxy("Galaxy", 0, seed);
        created.setVersion(GALAXY_VERSION);
        return created;
    }

    private static boolean save(MinecraftServer server) {
        if (galaxy == null) return false;
        try {
            NbtIo.writeCompressed(galaxy.toNBT(server.registryAccess()), file(server));
            return true;
        } catch (IOException e) {
            MatterOverdrive.LOGGER.error("Galaxy could not be saved", e);
            return false;
        }
    }

    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        MinecraftServer server = level.getServer();
        Path path = file(server);
        long start = System.nanoTime();
        if (Files.isRegularFile(path)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                Galaxy loaded = new Galaxy();
                loaded.readNBT(tag, server.registryAccess());
                setGalaxy(loaded);
                MatterOverdrive.LOGGER.info("Galaxy Loaded from '{}'. Took {} milliseconds", path, (System.nanoTime() - start) / 1000000);
                return;
            } catch (IOException e) {
                MatterOverdrive.LOGGER.error("Could not load galaxy from file", e);
            }
        }
        setGalaxy(createGalaxy(level.getSeed()));
        save(server);
        MatterOverdrive.LOGGER.info("Galaxy Generated and saved to '{}'. Took {} milliseconds", path, (System.nanoTime() - start) / 1000000);
    }

    @SubscribeEvent
    static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || galaxy == null) return;
        if (galaxy.isDirty() && save(level.getServer())) galaxy.onSave();
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        setGalaxy(null);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (galaxy != null && event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) galaxy.update(level);
    }

    // --- players -------------------------------------------------------------------------------------

    @SubscribeEvent
    static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (galaxy == null) {
            MatterOverdrive.LOGGER.warn("Galaxy is missing.");
            return;
        }
        send(player, new StarMapPayloads.GalaxySync(galaxy));
        tryAndClaimPlanet(player);
        // the client only knows the stars: send it the claimed planets (1.7.10 sent only a newly claimed homeworld)
        for (Planet planet : homePlanets.values()) {
            send(player, StarMapPayloads.PlanetUpdate.of(planet, player.registryAccess(), true));
        }
    }

    /** 1.7.10 tryAndClaimPlanet: a player without a homeworld gets one. */
    public static boolean tryAndClaimPlanet(Player player) {
        if (galaxy == null || homePlanets.containsKey(player.getUUID())) return false;
        Planet planet = claimPlanet(player);
        if (planet == null) {
            MatterOverdrive.LOGGER.warn("{} could not claim planet.", player.getName().getString());
            return false;
        }
        homePlanets.put(player.getUUID(), planet);
        return true;
    }

    /** 1.7.10 claimPlanet: a random quadrant's first star with planets and no one else's planet; a random planet of it. */
    private static @Nullable Planet claimPlanet(Player player) {
        int quadrantID = random.nextInt(galaxy.getQuadrants().size());
        for (Quadrant quadrant : galaxy.getQuadrants()) {
            if (quadrant.getId() != quadrantID) continue;
            for (Star star : quadrant.getStars()) {
                if (star.getPlanets().isEmpty()) continue;
                boolean claimed = false;
                for (Planet planet : star.getPlanets()) {
                    if (planet.hasOwner() && !planet.getOwnerUUID().equals(player.getUUID())) {
                        claimed = true;
                        break;
                    }
                }
                if (!claimed) {
                    Planet planet = (Planet) star.getPlanets().toArray()[random.nextInt(star.getPlanets().size())];
                    buildHomeworld(planet, player);
                    return planet;
                }
            }
        }
        return null;
    }

    /** 1.7.10 buildHomeworld: 8 building and 10 fleet spaces (the starting base and scout ship come with phase 7u). */
    private static void buildHomeworld(Planet planet, Player player) {
        planet.setOwnerUUID(player.getUUID());
        planet.setHomeworld(true);
        planet.setBuildingSpaces(8);
        planet.setFleetSpaces(10);
        planet.markDirty();
    }

    /** Only to clients that know the payload (GameTest mock players don't). */
    static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(player, payload);
    }

    // --- events / sync -------------------------------------------------------------------------------

    /** 1.7.10 Planet.onTravelEvent: tell the ship's owner it arrived. */
    static void onTravelEvent(Level level, Planet to, ItemStack ship, GalacticPosition from) {
        to.markForUpdate();
    }

    static void sendPlanet(MinecraftServer server, Planet planet) {
        var update = StarMapPayloads.PlanetUpdate.of(planet, server.registryAccess(), false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, update);
    }

    static void sendTravelEvents(MinecraftServer server) {
        if (galaxy == null) return;
        var events = new StarMapPayloads.TravelEvents(galaxy);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, events);
    }
}
