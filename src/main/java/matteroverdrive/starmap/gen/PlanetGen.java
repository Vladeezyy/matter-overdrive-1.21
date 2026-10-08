package matteroverdrive.starmap.gen;

import java.util.Random;

import matteroverdrive.starmap.Planet;

/**
 * 1.7.10 PlanetAbstractGen + PlanetNormalGen / PlanetGasGiantGen / PlanetDwarfGen: size and building / fleet spaces by
 * kind, weighted by the orbit. 1.7.10 quirk kept: the type is always set to 2.
 */
public abstract class PlanetGen implements SpaceBodyGen<Planet> {
    private final int buildingSpaces, fleetSpaces;

    protected PlanetGen(int buildingSpaces, int fleetSpaces) {
        this.buildingSpaces = buildingSpaces;
        this.fleetSpaces = fleetSpaces;
    }

    @Override
    public void generateSpaceBody(Planet planet, Random random) {
        planet.setType((byte) 2);
        setSize(planet, random);
        planet.setBuildingSpaces(buildingSpaces);
        planet.setFleetSpaces(fleetSpaces);
    }

    protected abstract void setSize(Planet planet, Random random);

    public static class Normal extends PlanetGen {
        public Normal() {
            super(6, 6);
        }

        @Override
        protected void setSize(Planet planet, Random random) {
            planet.setSize(0.7f + random.nextFloat() * 0.6f);
        }

        @Override
        public double getWeight(Planet planet) {
            return planet.getOrbit() < 0.6f && planet.getOrbit() > 0.4f ? 0.3f : 0.1f;
        }
    }

    public static class GasGiant extends PlanetGen {
        public GasGiant() {
            super(2, 8);
        }

        @Override
        protected void setSize(Planet planet, Random random) {
            planet.setSize(2 + random.nextFloat());
        }

        @Override
        public double getWeight(Planet planet) {
            return planet.getOrbit() > 0.6f ? 0.3f : 0.1f;
        }
    }

    public static class Dwarf extends PlanetGen {
        public Dwarf() {
            super(4, 4);
        }

        @Override
        protected void setSize(Planet planet, Random random) {
            planet.setSize(0.2f + random.nextFloat() * 0.4f);
        }

        @Override
        public double getWeight(Planet planet) {
            return planet.getOrbit() > 0.6f || planet.getOrbit() < 0.4f ? 0.3f : 0.1f;
        }
    }
}
