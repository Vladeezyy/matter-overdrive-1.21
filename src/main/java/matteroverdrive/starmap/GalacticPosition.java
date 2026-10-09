package matteroverdrive.starmap;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 1.7.10 GalacticPosition: quadrant / star / planet ids, -1 where unset. */
public record GalacticPosition(int quadrantID, int starID, int planetID) {
    public static final GalacticPosition NONE = new GalacticPosition(-1, -1, -1);
    // 1.7.10 writeToBuffer order: planet, star, quadrant
    public static final StreamCodec<ByteBuf, GalacticPosition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GalacticPosition::planetID, ByteBufCodecs.INT, GalacticPosition::starID,
            ByteBufCodecs.INT, GalacticPosition::quadrantID, (p, s, q) -> new GalacticPosition(q, s, p));

    public static GalacticPosition of(Quadrant quadrant) {
        return new GalacticPosition(quadrant.getId(), -1, -1);
    }

    public static GalacticPosition of(Star star) {
        return new GalacticPosition(star.getQuadrant() == null ? -1 : star.getQuadrant().getId(), star.getId(), -1);
    }

    public static GalacticPosition of(Planet planet) {
        Star star = planet.getStar();
        if (star == null) return new GalacticPosition(-1, -1, planet.getId());
        return new GalacticPosition(star.getQuadrant() == null ? -1 : star.getQuadrant().getId(), star.getId(), planet.getId());
    }

    /** 1.7.10 equals(Star): same star id in the same (known) quadrant. */
    public boolean is(Star star) {
        return star != null && starID == star.getId() && quadrantID >= 0 && star.getQuadrant() != null && star.getQuadrant().getId() == quadrantID;
    }

    public boolean is(Planet planet) {
        return planet != null && planetID == planet.getId() && is(planet.getStar());
    }

    public boolean is(Quadrant quadrant) {
        return quadrant != null && quadrantID == quadrant.getId();
    }

    public int distanceToLY(Galaxy galaxy, GalacticPosition position) {
        Star from = galaxy.getStar(this), to = galaxy.getStar(position);
        if (from != null && to != null && from != to) return (int) (from.getPosition().distanceTo(to.getPosition()) * Galaxy.GALAXY_SIZE_TO_LY);
        return 0;
    }

    public int distanceToAU(Galaxy galaxy, GalacticPosition position) {
        Planet from = galaxy.getPlanet(this), to = galaxy.getPlanet(position);
        if (from != null && to != null) return (int) (Math.abs(from.getOrbit() - to.getOrbit()) * Galaxy.PLANET_SYSTEM_SIZE_TO_AU);
        return 0;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("GalacticPositionPlanet", planetID);
        tag.putInt("GalacticPositionStar", starID);
        tag.putInt("GalacticPositionQuadrant", quadrantID);
        return tag;
    }

    public static GalacticPosition fromNBT(CompoundTag tag) {
        return new GalacticPosition(intOr(tag, "GalacticPositionQuadrant"), intOr(tag, "GalacticPositionStar"),
                intOr(tag, "GalacticPositionPlanet"));
    }

    private static int intOr(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getInt(key) : -1;
    }
}
