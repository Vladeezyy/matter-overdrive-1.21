package matteroverdrive.world;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * The 1.7.10 image-generated buildings (MOWorldGenBuilding subclasses) and where they may stand. 1.7.10 put a building
 * at a random block of the chunk, {@code y = height - 2} (the block under the top block) plus its yOffset, after
 * checking the four corners of its footprint.
 */
public enum Building implements StringRepresentable {
    /** MOAndroidHouseBuilding: yOffset -2, corners on land within 2 blocks of air; rogue androids inside. */
    ANDROID_HOUSE("android_house", -2),
    /** MOSandPit: yOffset -9, desert, corners within 3 blocks. */
    SAND_PIT("sand_pit", -9),
    /** MOWorldGenCrashedSpaceShip: yOffset -1, corners on land within 2 blocks of air. */
    CRASHED_SHIP("crashed_ship", -1),
    /** MOWorldGenUnderwaterBase: deep ocean, more than 26 blocks of water at every corner, built on the sea floor. */
    UNDERWATER_BASE("underwater_base", 0),
    /** MOWorldGenCargoShip: 86 blocks up (at most 18 below the build limit) with open air, only 10% of the tries. */
    CARGO_SHIP("cargo_ship", 86);

    public static final com.mojang.serialization.Codec<Building> CODEC = StringRepresentable.fromEnum(Building::values);

    private final String id;
    private final int yOffset;

    Building(String id, int yOffset) {
        this.id = id;
        this.yOffset = yOffset;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public BuildingTemplate template() {
        return BuildingTemplate.get(id);
    }

    /** The building's lowest corner for a 1.7.10 start column at x, z, or empty when 1.7.10 wouldn't build there. */
    public Optional<Integer> baseY(Structure.GenerationContext context, int x, int z) {
        BuildingTemplate t = template();
        int[][] corners = {{x, z}, {x + t.width(), z}, {x + t.width(), z + t.depth()}, {x, z + t.depth()}};
        int top = surface(context, x, z) - 1;
        int y = top - 1;
        switch (this) {
            case ANDROID_HOUSE, CRASHED_SHIP, SAND_PIT -> {
                // 1.7.10 locationIsValidSpawn: from y, at most maxDistanceToAir blocks to air, ending on stone/dirt/grass
                // (sand for the pit), not in water
                int maxDistanceToAir = this == SAND_PIT ? 3 : 2;
                for (int[] c : corners) {
                    int cornerTop = surface(context, c[0], c[1]) - 1;
                    if (cornerTop != floor(context, c[0], c[1]) - 1) return Optional.empty();
                    if (cornerTop - y < -1 || cornerTop - y > maxDistanceToAir) return Optional.empty();
                }
                return Optional.of(y + yOffset);
            }
            case UNDERWATER_BASE -> {
                // 1.7.10 isPointDeepEnough wanted more than 26 water blocks at every corner, which today's deep oceans
                // (mostly 15-27 deep) rarely have: 16 are enough, and the base sinks into the sea floor until its top
                // is under water. Built from the sea floor at the centre, like 1.7.10.
                for (int[] c : corners) {
                    if (surface(context, c[0], c[1]) - floor(context, c[0], c[1]) < 16) return Optional.empty();
                }
                int seaFloor = floor(context, x + t.width() / 2, z + t.depth() / 2) - 1;
                return Optional.of(Math.min(seaFloor, surface(context, x + t.width() / 2, z + t.depth() / 2) - 1 - t.height()));
            }
            case CARGO_SHIP -> {
                if (context.random().nextDouble() >= 0.1) return Optional.empty();
                int shipY = Math.min(y + yOffset, context.heightAccessor().getMaxY() + 1 - 18);
                for (int[] c : corners) {
                    if (surface(context, c[0], c[1]) > shipY) return Optional.empty();
                }
                return Optional.of(shipY);
            }
        }
        return Optional.empty();
    }

    /** First free block above the ground or water (1.7.10 World.getHeightValue). */
    private static int surface(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    /** First block above the solid ground under any water. */
    private static int floor(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }
}
