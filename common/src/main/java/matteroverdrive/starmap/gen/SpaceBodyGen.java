package matteroverdrive.starmap.gen;

import java.util.Random;

import matteroverdrive.starmap.SpaceBody;

/** 1.7.10 ISpaceBodyGen (generateMissing is dropped: this port's saves always have every value). */
public interface SpaceBodyGen<T extends SpaceBody> {
    void generateSpaceBody(T body, Random random);

    double getWeight(T body);
}
