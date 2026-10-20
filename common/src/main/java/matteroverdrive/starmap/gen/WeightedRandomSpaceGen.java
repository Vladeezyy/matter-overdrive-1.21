package matteroverdrive.starmap.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.starmap.SpaceBody;

/** 1.7.10 WeightedRandomSpaceGen: picks a gen by its weight for the body. */
public class WeightedRandomSpaceGen<T extends SpaceBody> {
    private final List<SpaceBodyGen<T>> gens = new ArrayList<>();

    public @Nullable SpaceBodyGen<T> getRandomGen(T body, Random random) {
        double total = 0;
        for (SpaceBodyGen<T> gen : gens) total += gen.getWeight(body);
        double roll = random.nextDouble() * total;
        for (SpaceBodyGen<T> gen : gens) {
            roll -= gen.getWeight(body);
            if (roll <= 0) return gen;
        }
        return null;
    }

    public void addGen(SpaceBodyGen<T> gen) {
        gens.add(gen);
    }

    public List<SpaceBodyGen<T>> getGens() {
        return gens;
    }
}
