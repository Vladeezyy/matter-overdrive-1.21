package matteroverdrive.starmap;

import java.util.ArrayList;
import java.util.List;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 1.7.10 PacketUpdateGalaxy, PacketUpdatePlanet, PacketStarLoading, PacketUpdateTravelEvents. */
public final class StarMapPayloads {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    /** The whole galaxy: quadrants, stars (no planets), travel events. */
    public record GalaxySync(Galaxy galaxy) implements CustomPacketPayload {
        public static final Type<GalaxySync> TYPE = new Type<>(id("galaxy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GalaxySync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> p.galaxy.write(buf), buf -> new GalaxySync(Galaxy.read(buf)));

        @Override
        public Type<GalaxySync> type() {
            return TYPE;
        }
    }

    /** One planet (its 1.7.10 NBT), optionally reloading the homeworld list. */
    public record PlanetUpdate(int quadrant, int star, int planet, boolean updateHomeworlds, CompoundTag data) implements CustomPacketPayload {
        public static final Type<PlanetUpdate> TYPE = new Type<>(id("planet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlanetUpdate> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, PlanetUpdate::quadrant, ByteBufCodecs.INT, PlanetUpdate::star, ByteBufCodecs.INT, PlanetUpdate::planet,
                ByteBufCodecs.BOOL, PlanetUpdate::updateHomeworlds, ByteBufCodecs.COMPOUND_TAG, PlanetUpdate::data, PlanetUpdate::new);

        public static PlanetUpdate of(Planet planet, HolderLookup.Provider registries, boolean updateHomeworlds) {
            GalacticPosition pos = GalacticPosition.of(planet);
            return new PlanetUpdate(pos.quadrantID(), pos.starID(), pos.planetID(), updateHomeworlds, planet.toNBT(registries));
        }

        @Override
        public Type<PlanetUpdate> type() {
            return TYPE;
        }
    }

    /** Client -> server: send me this star's planets. */
    public record StarRequest(int quadrant, int star) implements CustomPacketPayload {
        public static final Type<StarRequest> TYPE = new Type<>(id("star_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StarRequest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, StarRequest::quadrant, ByteBufCodecs.INT, StarRequest::star, StarRequest::new);

        @Override
        public Type<StarRequest> type() {
            return TYPE;
        }
    }

    /** Server -> client: the star's planets. */
    public record StarPlanets(int quadrant, int star, List<CompoundTag> planets) implements CustomPacketPayload {
        public static final Type<StarPlanets> TYPE = new Type<>(id("star_planets"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StarPlanets> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, StarPlanets::quadrant, ByteBufCodecs.INT, StarPlanets::star,
                ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()), StarPlanets::planets, StarPlanets::new);

        @Override
        public Type<StarPlanets> type() {
            return TYPE;
        }
    }

    public record TravelEvents(Galaxy galaxy) implements CustomPacketPayload {
        public static final Type<TravelEvents> TYPE = new Type<>(id("travel_events"));
        // the client applies the events to its own galaxy: decoded into a holder galaxy
        public static final StreamCodec<RegistryFriendlyByteBuf, TravelEvents> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> p.galaxy.writeTravelEvents(buf), buf -> {
                    Galaxy holder = new Galaxy();
                    holder.readTravelEvents(buf);
                    return new TravelEvents(holder);
                });

        @Override
        public Type<TravelEvents> type() {
            return TYPE;
        }
    }

    /** 1.7.10 PacketStarMapClientCommands: the screen sets the map's zoom, position and destination. */
    public record Command(net.minecraft.core.BlockPos pos, int zoom, GalacticPosition position, GalacticPosition destination)
            implements CustomPacketPayload {
        public static final Type<Command> TYPE = new Type<>(id("star_map_command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Command> STREAM_CODEC = StreamCodec.composite(
                net.minecraft.core.BlockPos.STREAM_CODEC, Command::pos, ByteBufCodecs.BYTE.map(b -> (int) b, i -> (byte) (int) i), Command::zoom,
                GalacticPosition.STREAM_CODEC, Command::position, GalacticPosition.STREAM_CODEC, Command::destination, Command::new);

        @Override
        public Type<Command> type() {
            return TYPE;
        }
    }

    private static void handleCommand(Command command, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.canInteractWithBlock(command.pos(), 4)
                || !(player.level().getBlockEntity(command.pos()) instanceof matteroverdrive.block.entity.StarMapBlockEntity starMap)
                || !starMap.isUseableByPlayer(player)) return;
        starMap.setZoomLevel(command.zoom());
        starMap.setGalacticPosition(command.position());
        starMap.setDestination(command.destination());
        starMap.sync();
    }

    /** 1.7.10 PacketStarMapAttack: send ship shipID of planet "from" to planet "to". */
    public record Attack(GalacticPosition from, GalacticPosition to, int shipID) implements CustomPacketPayload {
        public static final Type<Attack> TYPE = new Type<>(id("star_map_attack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Attack> STREAM_CODEC = StreamCodec.composite(
                GalacticPosition.STREAM_CODEC, Attack::from, GalacticPosition.STREAM_CODEC, Attack::to, ByteBufCodecs.INT, Attack::shipID, Attack::new);

        @Override
        public Type<Attack> type() {
            return TYPE;
        }
    }

    /** Only the ship's owner sends it (1.7.10 trusted the client). */
    private static void handleAttack(Attack attack, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Galaxy galaxy = GalaxyServer.getGalaxy();
        Planet from = galaxy == null ? null : galaxy.getPlanet(attack.from());
        if (from == null || attack.shipID() < 0 || attack.shipID() >= from.getFleet().size()) return;
        var ship = from.getShip(attack.shipID());
        if (!(ship.getItem() instanceof ShipItem item) || !item.isOwner(ship, player)) return;
        if (GalaxyServer.createTravelEvent(player.level(), attack.from(), attack.to(), attack.shipID()) != null) {
            GalaxyServer.sendTravelEvents(player.level().getServer());
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(GalaxySync.TYPE, GalaxySync.STREAM_CODEC, (p, c) -> GalaxyClient.setGalaxy(p.galaxy()))
                .playToClient(PlanetUpdate.TYPE, PlanetUpdate.STREAM_CODEC, StarMapPayloads::handlePlanet)
                .playToServer(StarRequest.TYPE, StarRequest.STREAM_CODEC, StarMapPayloads::handleStarRequest)
                .playToServer(Command.TYPE, Command.STREAM_CODEC, StarMapPayloads::handleCommand)
                .playToServer(Attack.TYPE, Attack.STREAM_CODEC, StarMapPayloads::handleAttack)
                .playToClient(StarPlanets.TYPE, StarPlanets.STREAM_CODEC, StarMapPayloads::handleStarPlanets)
                .playToClient(TravelEvents.TYPE, TravelEvents.STREAM_CODEC, (p, c) -> {
                    Galaxy galaxy = GalaxyClient.getGalaxy();
                    if (galaxy == null) return;
                    galaxy.getTravelEvents().clear();
                    galaxy.getTravelEvents().addAll(p.galaxy().getTravelEvents());
                });
    }

    /** 1.7.10 PacketUpdatePlanet.ClientHandler: update the planet or add it to its star. */
    private static void handlePlanet(PlanetUpdate update, IPayloadContext context) {
        Galaxy galaxy = GalaxyClient.getGalaxy();
        if (galaxy == null) return;
        Star star = galaxy.getStar(new GalacticPosition(update.quadrant(), update.star(), -1));
        if (star != null) {
            Planet planet = star.planet(update.planet());
            if (planet == null) {
                planet = new Planet();
                planet.readNBT(update.data(), context.player().registryAccess());
                star.addPlanet(planet);
            } else {
                planet.readNBT(update.data(), context.player().registryAccess());
            }
            onPlanetChanged.accept(planet);
        }
        if (update.updateHomeworlds()) GalaxyClient.loadClaimedPlanets();
    }

    /** The star map screen refreshes on a planet change (1.7.10 GuiStarMap.onPlanetChange). */
    public static java.util.function.Consumer<Planet> onPlanetChanged = planet -> {};

    private static void handleStarRequest(StarRequest request, IPayloadContext context) {
        Galaxy galaxy = GalaxyServer.getGalaxy();
        if (galaxy == null || !(context.player() instanceof ServerPlayer player)) return;
        Star star = galaxy.getStar(new GalacticPosition(request.quadrant(), request.star(), -1));
        if (star == null) return;
        List<CompoundTag> planets = new ArrayList<>();
        for (Planet planet : star.getPlanets()) planets.add(planet.toNBT(player.registryAccess()));
        PacketDistributor.sendToPlayer(player, new StarPlanets(request.quadrant(), request.star(), planets));
    }

    private static void handleStarPlanets(StarPlanets message, IPayloadContext context) {
        Star star = GalaxyClient.getStar(new GalacticPosition(message.quadrant(), message.star(), -1));
        if (star == null) return;
        for (CompoundTag data : message.planets()) {
            Planet planet = new Planet();
            planet.readNBT(data, context.player().registryAccess());
            star.addPlanet(planet);
        }
        GalaxyClient.loadClaimedPlanets();
    }

    private StarMapPayloads() {}
}
