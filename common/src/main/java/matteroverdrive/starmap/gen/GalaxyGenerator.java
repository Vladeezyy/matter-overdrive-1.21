package matteroverdrive.starmap.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.Quadrant;
import matteroverdrive.starmap.Star;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 GalaxyGenerator with its default config: 2048-2303 stars (gaussian around the centre, sigma 1/3, clamped to
 * -1..1) in 3^3 quadrants, 1-3 planets per star. Uses java.util.Random like 1.7.10, so a world seed gives the same galaxy.
 */
public class GalaxyGenerator {
    private final Random random = new Random(), starRandom = new Random(), planetRandom = new Random(), starNameRandom = new Random();
    private final WeightedRandomSpaceGen<Planet> planetGen = new WeightedRandomSpaceGen<>();
    private final WeightedRandomSpaceGen<Star> starGen = new WeightedRandomSpaceGen<>();
    public static float starPrefixChance = 1, starSuffixChance = 0.8f;
    public static int minStars = 2048, maxStars = 2048 + 256, minPlanets = 1, maxPlanets = 4, quadrantCount = 3;

    public GalaxyGenerator() {
        planetGen.addGen(new PlanetGen.GasGiant());
        planetGen.addGen(new PlanetGen.Dwarf());
        planetGen.addGen(new PlanetGen.Normal());
        for (StarGen gen : StarGen.getStarGens()) starGen.addGen(gen);
    }

    public Galaxy generateGalaxy(String name, int id, long seed) {
        Galaxy galaxy = new Galaxy(name, id, seed);
        random.setSeed(seed);
        generateQuadrants(galaxy, quadrantCount);
        return galaxy;
    }

    public void generateStar(Star star) {
        star.clearPlanets();
        starRandom.setSeed(star.getSeed());
        Vec3 pos = generateStarPosition(starRandom);
        star.setPosition((float) pos.x, (float) pos.y, (float) pos.z);
        starGen.getRandomGen(star, starRandom).generateSpaceBody(star, starRandom);
        generatePlanets(star, minPlanets + random.nextInt(maxPlanets - minPlanets));
    }

    public void generatePlanet(Planet planet) {
        planetRandom.setSeed(planet.getSeed());
        planet.setOrbit(planetRandom.nextFloat());
        planetGen.getRandomGen(planet, planetRandom).generateSpaceBody(planet, planetRandom);
    }

    public void generateQuadrants(Galaxy galaxy, int size3d) {
        starNameRandom.setSeed(galaxy.getSeed());
        List<Star> stars = generateStars(minStars + random.nextInt(maxStars - minStars));
        Quadrant[] quadrants = new Quadrant[size3d * size3d * size3d];
        float piece = 2f / size3d;
        for (int i = 0; i < quadrants.length; i++) {
            float z = (i % size3d) * piece - 1;
            float y = ((i / size3d) % size3d) * piece - 1;
            float x = (i / (size3d * size3d)) * piece - 1;
            quadrants[i] = new Quadrant("Q" + i, i);
            galaxy.addQuadrant(quadrants[i]);
            quadrants[i].setSize(piece);
            quadrants[i].setPosition(x, y, z);
        }
        // 1.7.10: a star exactly at +1 on an axis fits no quadrant and is dropped
        for (Star star : stars) {
            for (Quadrant quadrant : quadrants) {
                if (star.getX() >= quadrant.getX() && star.getX() < quadrant.getX() + piece
                        && star.getY() >= quadrant.getY() && star.getY() < quadrant.getY() + piece
                        && star.getZ() >= quadrant.getZ() && star.getZ() < quadrant.getZ() + piece) {
                    star.setId(quadrant.getStars().size());
                    quadrant.addStar(star);
                }
            }
        }
    }

    public List<Star> generateStars(int amount) {
        List<Star> stars = new ArrayList<>(amount);
        List<String> names = StarGen.generateAvailableNames(starNameRandom, 18, starPrefixChance, starSuffixChance);
        for (int i = 0; i < amount; i++) {
            Star star = new Star(names.get(i), i);
            star.setSeed(random.nextInt());
            stars.add(star);
            generateStar(star);
        }
        return stars;
    }

    public void generatePlanets(Star star, int amount) {
        for (int i = 0; i < amount; i++) {
            Planet planet = new Planet(star.getName() + " " + i, i);
            planet.setSeed(random.nextInt());
            star.addPlanet(planet);
            generatePlanet(planet);
        }
    }

    /** 1.7.10 MOMathHelper.nextGaussian(random, 0, 1/3) per axis, clamped to -1..1. */
    public static Vec3 generateStarPosition(Random random) {
        double x = Mth.clamp(0 + random.nextGaussian() * (1d / 3d), -1, 1);
        double y = Mth.clamp(0 + random.nextGaussian() * (1d / 3d), -1, 1);
        double z = Mth.clamp(0 + random.nextGaussian() * (1d / 3d), -1, 1);
        return new Vec3(x, y, z);
    }
}
