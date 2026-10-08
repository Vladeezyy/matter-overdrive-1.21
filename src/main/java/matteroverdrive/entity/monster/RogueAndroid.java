package matteroverdrive.entity.monster;

import java.util.List;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.Android;
import matteroverdrive.init.MOItems;
import matteroverdrive.init.MOSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 EntityRougeAndroidMob: a hostile android of level 0-3 (5% x level chance to be legendary: 128 health, 1.5x
 * size), with a visor colour by level and a robot name. It hunts players who aren't androids.
 */
public abstract class RogueAndroid extends Monster {
    /** 1.7.10 info/android_names.txt */
    static final String[] NAMES = {"KITT", "GERTY", "Mega Man", "Awesom-O", "HK-47", "ED-209", "Bishop", "Clank", "Johnny 5", "The Robot",
            "Marvin", "Robbie", "Astro", "Optimus Prime", "K-9", "T-1000", "ASIMO", "GLaDOS", "HAL 9000", "Data", "R2D2", "Bender", "Wall-E",
            "C-3PO", "Gort", "IG-88", "T-800", "ED-209", "B-4", "Ultron", "CHAPPiE", "Sonny", "BB-8"};
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(RogueAndroid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LEGENDARY = SynchedEntityData.defineId(RogueAndroid.class, EntityDataSerializers.BOOLEAN);
    public static final int MAX_ANDROIDS_PER_CHUNK = 4;
    /** 1.7.10 Reference colours: COLOR_HOLO, COLOR_HOLO_YELLOW, COLOR_HOLO_PURPLE, white; legendary COLOR_HOLO_RED. */
    private static final int[] VISOR = {0xA9E2FB, 0xFCE48A, 0xBA8BDB, 0xFFFFFF};

    protected RogueAndroid(EntityType<? extends RogueAndroid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 64).add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEVEL, 0);
        builder.define(LEGENDARY, false);
    }

    public int getAndroidLevel() {
        return entityData.get(LEVEL);
    }

    public boolean isLegendary() {
        return entityData.get(LEGENDARY);
    }

    public int getVisorColor() {
        return isLegendary() ? 0xE65014 : VISOR[Mth.clamp(getAndroidLevel(), 0, 3)];
    }

    /** 1.7.10 init(): level, legendary, health, damage, name. */
    public void setup(int level, boolean legendary) {
        entityData.set(LEVEL, level);
        entityData.set(LEGENDARY, legendary);
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(legendary ? 128 : level * 10 + 32);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(legendary ? 8 : 4 + level);
        setHealth(getMaxHealth());
        MutableComponent name = Component.empty();
        if (legendary) {
            name.append(Component.literal("\u2605 ").append(Component.translatable("rarity." + MatterOverdrive.MODID + ".legendary")).append(" ")
                    .withStyle(ChatFormatting.GOLD));
        }
        name.append(Component.literal("[" + level + "] " + NAMES[random.nextInt(NAMES.length)])
                .withStyle(legendary ? ChatFormatting.GOLD : level == 0 ? ChatFormatting.GRAY : level == 1 ? ChatFormatting.DARK_AQUA
                        : level == 2 ? ChatFormatting.DARK_PURPLE : ChatFormatting.WHITE));
        setCustomName(name);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        // 1.7.10: |gaussian x (1 + difficulty x 0.25)| clamped to 0..3; legendary 5% per level
        int androidLevel = (int) Mth.clamp(Math.abs(random.nextGaussian() * (1 + level.getDifficulty().getId() * 0.25)), 0, 3);
        setup(androidLevel, random.nextDouble() < 0.05 * androidLevel);
        populateDefaultEquipmentSlots(random, difficulty);
        populateDefaultEquipmentEnchantments(level, random, difficulty);
        return data;
    }

    /** 1.7.10 addRandomArmor: like zombies, but the best tier is tritanium half the time. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        if (random.nextBoolean()) {
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                ItemStack current = getItemBySlot(slot);
                if (current.is(net.minecraft.world.item.Items.DIAMOND_HELMET) || current.is(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE)
                        || current.is(net.minecraft.world.item.Items.DIAMOND_LEGGINGS) || current.is(net.minecraft.world.item.Items.DIAMOND_BOOTS)) {
                    setItemSlot(slot, new ItemStack(switch (slot) {
                        case HEAD -> MOItems.TRITANIUM_HELMET.get();
                        case CHEST -> MOItems.TRITANIUM_CHESTPLATE.get();
                        case LEGS -> MOItems.TRITANIUM_LEGGINGS.get();
                        default -> MOItems.TRITANIUM_BOOTS.get();
                    }));
                }
            }
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isLegendary() ? super.getDefaultDimensions(pose).scale(1.5f) : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LEGENDARY.equals(key)) refreshDimensions();
    }

    /** 1.7.10 isPotionApplicable: no potion effects. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    /** 1.7.10 AndroidTargetSelector: humans (not androids) and mutant scientists. */
    public static boolean isEnemy(LivingEntity target) {
        return target instanceof Player player && !Android.isAndroid(player) && !player.isCreative() && !player.isSpectator();
    }

    /** A random part (1.7.10 AndroidPartsFactory: head/arms/legs/chest 100 each, spine 20). */
    protected ItemStack randomPart() {
        int roll = random.nextInt(420);
        if (roll < 100) return new ItemStack(MOItems.ROGUE_ANDROID_HEAD.get());
        if (roll < 200) return new ItemStack(MOItems.ROGUE_ANDROID_ARMS.get());
        if (roll < 300) return new ItemStack(MOItems.ROGUE_ANDROID_LEGS.get());
        if (roll < 400) return new ItemStack(MOItems.ROGUE_ANDROID_CHEST.get());
        return new ItemStack(MOItems.TRITANIUM_SPINE.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("android_level", getAndroidLevel());
        output.putBoolean("legendary", isLegendary());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(LEVEL, input.getIntOr("android_level", 0));
        entityData.set(LEGENDARY, input.getBooleanOr("legendary", false));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MOSounds.ROGUE_ANDROID_SAY.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MOSounds.ROGUE_ANDROID_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 20 * 24;
    }

    /** 1.7.10 hasToManyAndroids + getCanSpawnHere: normal monster rules, at most 4 per chunk. */
    public static boolean checkSpawnRules(EntityType<? extends RogueAndroid> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                          net.minecraft.core.BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) return false;
        var chunk = new net.minecraft.world.phys.AABB(pos.getX() & ~15, level.getMinY(), pos.getZ() & ~15,
                (pos.getX() & ~15) + 16, level.getMaxY(), (pos.getZ() & ~15) + 16);
        return level.getEntitiesOfClass(RogueAndroid.class, chunk).size() < MAX_ANDROIDS_PER_CHUNK;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }
}
