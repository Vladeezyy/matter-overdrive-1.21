package matteroverdrive.starmap.gen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.starmap.Star;
import net.minecraft.util.Mth;

/** 1.7.10 StarGen: one per star type (7, weighted), radius / mass / temperature ranges, colour from temperature; star names. */
public class StarGen implements SpaceBodyGen<Star> {
    private static final float[] RADIUSES = {8.8f, 6.6f, 6.6f, 1.8f, 1.8f, 1.4f, 1.4f, 1.15f, 1.15f, 0.96f, 0.96f, 0.7f, 0.7f, 0.2f};
    private static final int[] TEMPERATURES = {60000, 30000, 30000, 10000, 10000, 7500, 7500, 6000, 6000, 5200, 5200, 3700, 3700, 2400};
    private static final float[] MASSES = {32, 16, 16, 2.1f, 2.1f, 1.4f, 1.4f, 1.04f, 1.04f, 0.8f, 0.8f, 0.45f, 0.45f, 0.08f};
    private static final double[] WEIGHTS = {0.00003, 0.13, 0.6, 3, 7.6, 12.1, 76.45};
    private final int type;

    public StarGen(int type) {
        this.type = Math.min(type, WEIGHTS.length - 1);
    }

    public static List<StarGen> getStarGens() {
        List<StarGen> gens = new ArrayList<>();
        for (int i = 0; i < 7; i++) gens.add(new StarGen(i));
        return gens;
    }

    @Override
    public void generateSpaceBody(Star star, Random random) {
        star.setType((byte) type);
        star.setSize(RADIUSES[type * 2 + 1] + random.nextFloat() * (RADIUSES[type * 2] - RADIUSES[type * 2 + 1]));
        star.setMass(MASSES[type * 2 + 1] + random.nextFloat() * (MASSES[type * 2] - MASSES[type * 2 + 1]));
        // 1.7.10 cast the temperature to short: the hottest stars (30000-60000 K) can wrap around
        star.setTemperature((short) (TEMPERATURES[type * 2 + 1] + random.nextInt(TEMPERATURES[type * 2] - TEMPERATURES[type * 2 + 1])));
        star.setColor(getColorFromTemperature(star.getTemperature()));
    }

    @Override
    public double getWeight(Star star) {
        return WEIGHTS[type];
    }

    /** 1.7.10 getColorFromTemperature (Tanner Helland's black body approximation), as ARGB. */
    public static int getColorFromTemperature(int temperature) {
        temperature /= 100;
        int red, green, blue;
        if (temperature <= 66) {
            red = 255;
            green = Mth.clamp((int) (99.4708025861 * Math.log(temperature) - 161.1195681661), 0, 255);
        } else {
            red = Mth.clamp((int) (329.698727446 * Math.pow(temperature - 60, -0.1332047592)), 0, 255);
            green = Mth.clamp((int) (288.1221695283 * Math.pow(temperature - 60, -0.0755148492)), 0, 255);
        }
        if (temperature >= 66) {
            blue = 255;
        } else if (temperature <= 19) {
            blue = 0;
        } else {
            blue = Mth.clamp((int) (138.5177312231 * Math.log(temperature - 10) - 305.0447927307), 0, 255);
        }
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    // --- names (1.7.10 info/star_names.txt, star_prefixes.txt, star_suffixes.txt) ---------------------

    private static String[] starNames, prefixes, suffixes;

    private static String read(String file) {
        try {
            byte[] bytes = net.neoforged.fml.ModList.get().getModFileById(MatterOverdrive.MODID).getFile().getContents()
                    .readFile("assets/" + MatterOverdrive.MODID + "/info/" + file);
            if (bytes == null) return "";
            // 1.7.10 readTextFile joined the lines without separators
            return new String(bytes, StandardCharsets.UTF_8).replace("\r", "").replace("\n", "");
        } catch (java.io.IOException e) {
            MatterOverdrive.LOGGER.error("Could not read {}", file, e);
            return "";
        }
    }

    private static void loadNames() {
        if (starNames != null) return;
        starNames = read("star_names.txt").split(",");
        prefixes = read("star_prefixes.txt").split(",");
        suffixes = read("star_suffixes.txt").split(",");
    }

    private static String addPrefix(String name, String prefix) {
        if (prefix.endsWith("-")) return prefix.substring(0, prefix.length() - 2) + Character.toLowerCase(name.charAt(0)) + name.substring(1);
        return prefix + " " + name;
    }

    private static String addSuffix(String name, String suffix) {
        if (suffix.startsWith("-")) return name + suffix.substring(1);
        return name + " " + suffix;
    }

    /** 1.7.10 generateAvailableNames: names, prefixed / suffixed variants (by chance), shuffled. */
    public static List<String> generateAvailableNames(Random random, int maxLength, float prefixChance, float suffixChance) {
        loadNames();
        List<String> names = new ArrayList<>();
        for (String name : starNames) {
            if (name.length() <= maxLength) names.add(name.replace("*", ""));
            for (String prefix : prefixes) {
                if (!name.startsWith("*") && random.nextFloat() < prefixChance) {
                    String n = addPrefix(name, prefix);
                    if (n.length() <= maxLength) names.add(n.replace("*", ""));
                }
                for (String suffix : suffixes) {
                    if (!name.endsWith("*") && random.nextFloat() < suffixChance) {
                        String n = addSuffix(name, suffix);
                        if (n.length() <= maxLength) names.add(n.replace("*", ""));
                    }
                    if (!name.startsWith("*") && !name.endsWith("*") && random.nextFloat() < prefixChance && random.nextFloat() < suffixChance) {
                        String n = addPrefix(addSuffix(name, suffix), prefix);
                        if (n.length() <= maxLength) names.add(n);
                    }
                }
            }
        }
        Collections.shuffle(names, random);
        return names;
    }
}
