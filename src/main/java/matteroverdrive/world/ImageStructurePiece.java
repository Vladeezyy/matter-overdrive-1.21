package matteroverdrive.world;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.HoloSignBlockEntity;
import matteroverdrive.block.entity.TritaniumCrateBlockEntity;
import matteroverdrive.block.entity.WeaponStationBlockEntity;
import matteroverdrive.entity.monster.RogueAndroid;
import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOStructures;
import matteroverdrive.item.weapon.WeaponFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.Consumer;

/**
 * Places a {@link BuildingTemplate} chunk by chunk (1.7.10 placed a layer per tick with block update flag 2). Random
 * choices are seeded by the piece and the position, so every chunk agrees with its neighbours.
 */
public class ImageStructurePiece extends StructurePiece {
    public static final ResourceKey<LootTable> ANDROID_HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "chests/android_house"));
    /** 1.7.10 MOWorldGenCrashedSpaceShip.holoTexts */
    private static final String[] HOLO_TEXTS = {"Critical\nError", "Contacting\nSection 9", "System\nFailure", "Emergency\nPower\nOffline",
            "System\nReboot\nFailure", "Help Me", "I Need\nWater"};

    private final Building building;
    private final long seed;

    public ImageStructurePiece(Building building, BlockPos origin, long seed) {
        super(MOStructures.IMAGE_PIECE.get(), 0, box(building, origin));
        this.building = building;
        this.seed = seed;
    }

    public ImageStructurePiece(CompoundTag tag) {
        super(MOStructures.IMAGE_PIECE.get(), tag);
        this.building = Building.valueOf(tag.getStringOr("Building", "ANDROID_HOUSE"));
        this.seed = tag.getLongOr("Seed", 0);
    }

    private static BoundingBox box(Building building, BlockPos origin) {
        BuildingTemplate t = building.template();
        return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + t.width() - 1, origin.getY() + t.height() - 1, origin.getZ() + t.depth() - 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Building", building.name());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BuildingTemplate t = building.template();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < t.height(); y++) {
            for (int z = 0; z < t.depth(); z++) {
                for (int x = 0; x < t.width(); x++) {
                    pos.set(boundingBox.minX() + x, boundingBox.minY() + y, boundingBox.minZ() + z);
                    if (!chunkBox.isInside(pos)) continue;
                    BuildingTemplate.Entry entry = t.at(x, y, z);
                    if (entry == null) continue;
                    BlockState state = pick(entry, pos);
                    level.setBlock(pos, state, 2);
                    placed(level, entry, pos.immutable(), positional(pos));
                }
            }
        }
        if (building == Building.ANDROID_HOUSE) spawnAndroids(level, chunkBox);
    }

    /** 1.7.10 BlockMapping.getBlock: per block when noise, otherwise once per building and colour. */
    private BlockState pick(BuildingTemplate.Entry entry, BlockPos pos) {
        int n = entry.states().size();
        if (n == 1) return entry.states().get(0);
        RandomSource r = entry.noise() ? positional(pos) : RandomSource.create(seed ^ entry.group() * 0x9E3779B97F4A7C15L);
        return entry.states().get(r.nextInt(n));
    }

    private RandomSource positional(BlockPos pos) {
        return RandomSource.create(seed ^ Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()));
    }

    /** 1.7.10 onBlockPlace of the buildings. */
    private void placed(WorldGenLevel level, BuildingTemplate.Entry entry, BlockPos pos, RandomSource random) {
        if (entry.has("connect")) {
            level.getChunk(pos).markPosForPostprocessing(pos);   // pipes and fences connect once the chunk is done
        }
        if (entry.has("crate_loot") && level.getBlockEntity(pos) instanceof TritaniumCrateBlockEntity crate) {
            RandomizableContainer.setBlockEntityLootTable(level, random, pos, ANDROID_HOUSE_LOOT);
            // android house: 10 in 200 crates also hold a level 3 legendary weapon
            if (building == Building.ANDROID_HOUSE && random.nextInt(200) < 10) {
                crate.setItem(0, WeaponFactory.randomDecorated(random, 3, true));
            }
            // 1.7.10 MOWorldGenCrashedSpaceShip: every crate holds a crash landing contract that remembers the crate
            if (building == Building.CRASHED_SHIP) {
                matteroverdrive.quest.QuestStack contract = matteroverdrive.quest.Quests.CRASH_LANDING.generate(random);
                contract.getData().putIntArray("Pos", new int[] {pos.getX(), pos.getY(), pos.getZ()});
                crate.setItem(0, matteroverdrive.item.ContractItem.of(contract));
            }
        }
        if (entry.has("weapon") && random.nextInt(200) < 10 && level.getBlockEntity(pos) instanceof WeaponStationBlockEntity station) {
            station.getInventory().setStack(WeaponStationBlockEntity.WEAPON, WeaponFactory.randomDecorated(random, 3, true));
        }
        if (entry.has("holo_text") && random.nextInt(100) < 30 && level.getBlockEntity(pos) instanceof HoloSignBlockEntity sign) {
            sign.setText(HOLO_TEXTS[random.nextInt(HOLO_TEXTS.length)]);
        }
        if (entry.has("mutant")) {
            spawn(level, MOEntities.MUTANT_SCIENTIST.get(), pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                    mutant -> mutant.setCustomName(Component.literal("Mitko'Urrr")));
        }
    }

    /** 1.7.10 MOAndroidHouseBuilding.onGeneration: 3-5 rogue androids (60% ranged) in a row and a legendary level 3 one. */
    private void spawnAndroids(WorldGenLevel level, BoundingBox chunkBox) {
        RandomSource random = RandomSource.create(seed);
        int count = random.nextInt(3) + 3;
        int x = boundingBox.minX(), y = boundingBox.minY() + 4, z = boundingBox.minZ() + 10;
        for (int i = 0; i < count; i++) {
            EntityType<? extends RogueAndroid> type = random.nextInt(100) < 60 ? MOEntities.RANGED_ROGUE_ANDROID.get() : MOEntities.ROGUE_ANDROID.get();
            if (chunkBox.isInside(x + 7 + i, y, z)) spawn(level, type, x + 7 + i + 0.5, y + 0.5, z + 0.5, a -> {});
        }
        if (chunkBox.isInside(x + 12, y, z)) {
            spawn(level, MOEntities.RANGED_ROGUE_ANDROID.get(), x + 12.5, y + 0.5, z + 0.5,
                    a -> a.setupEquipped(3, true, level.getCurrentDifficultyAt(a.blockPosition())));
        }
    }

    private static <T extends Mob> void spawn(WorldGenLevel level, EntityType<T> type, double x, double y, double z, Consumer<T> setup) {
        T mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (mob == null) return;
        mob.snapTo(x, y, z, level.getRandom().nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.STRUCTURE, null);
        setup.accept(mob);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
