package matteroverdrive.block.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.entity.monster.RogueAndroid;
import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MOEntities;
import matteroverdrive.item.TransportFlashDriveItem;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.WeaponColorModuleItem;
import matteroverdrive.item.weapon.WeaponModule;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.menu.AndroidSpawnerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/**
 * 1.7.10 TileEntityAndroidSpawner (creative-only, unbreakable, no energy): keeps up to "max spawn amount" rogue androids
 * alive (30% melee), spawning the missing ones every "spawn delay" ticks within "spawn range" (only in the +x/+z
 * quarter, 1.7.10 clamped the gaussian to 0..1). They persist, join the configured scoreboard team (then a colour module
 * tints their visor and weapon) and walk the path of the marked spots in the transport flash drives, in slot order.
 * The spawner only runs while the redstone mode allows and the team (if set) exists.
 */
public class AndroidSpawnerBlockEntity extends MachineBlockEntity {
    public static final int COLOR_MODULE_SLOT = 0;
    public static final int FLASH_DRIVE_SLOT_START = 1;
    /** 1.7.10 FLASH_DRIVE_COUNT was 6, but both its GUI and assignPath only used slots 1-5. */
    public static final int FLASH_DRIVE_COUNT = 5;
    public static final int MAX_SPAWN_LIMIT = 32, SPAWN_RANGE_LIMIT = 32, SPAWN_DELAY_LIMIT = 100000;
    public static final int MAX_TEAM_LENGTH = 32;

    private final Set<UUID> spawned = new HashSet<>();
    private int maxSpawnAmount = 6;
    private int spawnRange = 4;
    private int spawnDelay = 300;
    private String team = "";

    public AndroidSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.ANDROID_SPAWNER.get(), pos, state, slots(), false, 0, 0, 0, 0, Set.of());
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.OTHER, r -> r.getItem() instanceof WeaponColorModuleItem, 1);
        for (int i = 0; i < FLASH_DRIVE_COUNT; i++) {
            b.add(MachineInventory.Role.OTHER, r -> r.getItem() instanceof TransportFlashDriveItem, 1);
        }
        return b;
    }

    // --- config (1.7.10 ConfigPropertyInteger / ConfigPropertyString) ---------------------------------

    public int getMaxSpawnAmount() {
        return maxSpawnAmount;
    }

    public int getSpawnRange() {
        return spawnRange;
    }

    public int getSpawnDelay() {
        return spawnDelay;
    }

    public String getTeamName() {
        return team;
    }

    public void setConfig(int maxSpawnAmount, int spawnRange, int spawnDelay, String team) {
        this.maxSpawnAmount = Mth.clamp(maxSpawnAmount, 0, MAX_SPAWN_LIMIT);
        this.spawnRange = Mth.clamp(spawnRange, 0, SPAWN_RANGE_LIMIT);
        this.spawnDelay = Mth.clamp(spawnDelay, 0, SPAWN_DELAY_LIMIT);
        this.team = team.length() > MAX_TEAM_LENGTH ? team.substring(0, MAX_TEAM_LENGTH) : team;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        // a new path applies to the androids already out there
        if (level instanceof ServerLevel server) forEachSpawned(server, this::assignPath);
    }

    public @Nullable PlayerTeam getTeam() {
        return team.isEmpty() || level == null ? null : level.getScoreboard().getPlayerTeam(team);
    }

    public boolean isTeamValid() {
        return team.isEmpty() || getTeam() != null;
    }

    public int getSpawnedCount() {
        return spawned.size();
    }

    // --- spawning ------------------------------------------------------------------------------------

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        // 1.7.10 getServerActive
        boolean active = redstoneAllows && isTeamValid() && spawned.size() <= maxSpawnAmount;
        if (active && level instanceof ServerLevel server && (spawnDelay == 0 || server.getGameTime() % spawnDelay == 0)) {
            for (int i = spawned.size(); i < maxSpawnAmount; i++) {
                spawnOne(server);
            }
        }
        return active;
    }

    private void spawnOne(ServerLevel server) {
        RogueAndroid android = server.random.nextInt(10) < 3
                ? MOEntities.ROGUE_ANDROID.get().create(server, EntitySpawnReason.SPAWNER)
                : MOEntities.RANGED_ROGUE_ANDROID.get().create(server, EntitySpawnReason.SPAWNER);
        if (android == null) return;
        BlockPos pos = worldPosition;
        double x = pos.getX() + Mth.clamp(server.random.nextGaussian(), 0, 1) * spawnRange;
        double z = pos.getZ() + Mth.clamp(server.random.nextGaussian(), 0, 1) * spawnRange;
        int topY = Math.min(server.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)), pos.getY() + 3);
        android.snapTo(x, topY, z, server.random.nextFloat() * 360, 0);
        // 1.7.10 getCanSpawnHere(true, true, true): not peaceful, no block collision, no liquid
        if (server.getDifficulty() == Difficulty.PEACEFUL || !server.noCollision(android) || server.containsAnyLiquid(android.getBoundingBox())) {
            return;
        }
        android.finalizeSpawn(server, server.getCurrentDifficultyAt(android.blockPosition()), EntitySpawnReason.SPAWNER, null);
        android.setSpawnerPos(pos);
        android.setPersistenceRequired();
        server.levelEvent(2004, pos, 0);
        PlayerTeam team = getTeam();
        if (team != null) {
            server.getScoreboard().addPlayerToTeam(android.getScoreboardName(), team);
            ItemStack module = inventory.getStack(COLOR_MODULE_SLOT);
            if (module.getItem() instanceof WeaponColorModuleItem color) {
                android.setVisorColor(color.getColor());
                if (android.getMainHandItem().getItem() instanceof EnergyWeaponItem) {
                    EnergyWeaponItem.setModule(android.getMainHandItem(), WeaponModule.SLOT_COLOR, module.copy());
                }
            }
        }
        addSpawnedAndroid(android);
        server.addFreshEntityWithPassengers(android);
        android.spawnAnim();
    }

    /** 1.7.10 assignPath: the flash drives' marked spots in order, or just the spawner; reached within the spawn range. */
    public void assignPath(RogueAndroid android) {
        List<Vec3> path = new ArrayList<>();
        for (int i = 0; i < FLASH_DRIVE_COUNT; i++) {
            BlockPos target = TransportFlashDriveItem.getTarget(inventory.getStack(FLASH_DRIVE_SLOT_START + i));
            if (target != null) path.add(Vec3.atLowerCornerOf(target));
        }
        if (path.isEmpty()) path.add(Vec3.atLowerCornerOf(worldPosition));
        android.setPath(path, spawnRange);
    }

    public void addSpawnedAndroid(RogueAndroid android) {
        if (spawned.add(android.getUUID())) {
            assignPath(android);
            setChanged();
        }
    }

    public void removeAndroid(RogueAndroid android) {
        spawned.remove(android.getUUID());
    }

    /** 1.7.10 removeAllAndroids ("Kill All" and when the spawner is removed). */
    public void removeAllAndroids() {
        if (level instanceof ServerLevel server) {
            for (UUID id : List.copyOf(spawned)) {
                Entity e = server.getEntity(id);
                if (e != null) e.discard();
            }
        }
        spawned.clear();
    }

    private void forEachSpawned(ServerLevel server, java.util.function.Consumer<RogueAndroid> action) {
        for (UUID id : spawned) {
            if (server.getEntity(id) instanceof RogueAndroid android) action.accept(android);
        }
    }

    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();
        if (level instanceof ServerLevel server) forEachSpawned(server, this::assignPath);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        removeAllAndroids();
    }

    // --- menu / persistence -----------------------------------------------------------------------

    /** Spawned count for the open menu (1.7.10 ContainerAndroidSpawner progress bar 0). */
    public final ContainerData spawnerData = new ContainerData() {
        @Override
        public int get(int index) {
            return index < DATA_COUNT ? dataAccess.get(index) : spawned.size();
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT + 1;
        }
    };

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AndroidSpawnerMenu(id, inventory, this, spawnerData);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("max_spawn_amount", maxSpawnAmount);
        output.putInt("spawn_range", spawnRange);
        output.putInt("spawn_delay", spawnDelay);
        output.putString("team", team);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        maxSpawnAmount = input.getIntOr("max_spawn_amount", 6);
        spawnRange = input.getIntOr("spawn_range", 4);
        spawnDelay = input.getIntOr("spawn_delay", 300);
        team = input.getStringOr("team", "");
    }
}
