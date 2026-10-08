package matteroverdrive.entity.monster;

import matteroverdrive.init.MOItems;
import matteroverdrive.item.BatteryItem;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.item.weapon.WeaponModule;
import matteroverdrive.item.weapon.WeaponShooter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 EntityRangedRogueAndroidMob: speed 0.25, carries a random energy weapon (rifle 70, shotgun 10, sniper 5; with
 * modules by level) with unlimited energy. Its shots scale with difficulty and android level. Drops energy packs, a
 * part (15%), rarely the weapon.
 */
public class RangedRogueAndroid extends RogueAndroid implements RangedAttackMob, WeaponShooter {
    public RangedRogueAndroid(EntityType<? extends RangedRogueAndroid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return RogueAndroid.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        // 1.7.10 EntityAIPhaserBoltAttack(1.0, 60, 15)
        goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 20, 60, 15));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, true,
                (target, level) -> RogueAndroid.isEnemy(target)));
    }

    /** 1.7.10 WeaponFactory.getRandomDecoratedEnergyWeapon. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        int roll = random.nextInt(85);
        EnergyWeaponItem item = roll < 70 ? MOItems.PHASER_RIFLE.get() : roll < 80 ? MOItems.PLASMA_SHOTGUN.get() : MOItems.ION_SNIPER.get();
        ItemStack weapon = new ItemStack(item);
        int level = getAndroidLevel();
        // barrel: none 200, damage 100 (level 1+), fire 10 (level 1+), explosion 5 (level 2+)
        int barrel = random.nextInt(200 + (level >= 1 ? 110 : 0) + (level >= 2 ? 5 : 0));
        ItemStack module = barrel < 200 ? ItemStack.EMPTY : barrel < 300 ? new ItemStack(MOItems.BARREL_DAMAGE.get())
                : barrel < 310 ? new ItemStack(MOItems.BARREL_FIRE.get()) : new ItemStack(MOItems.BARREL_EXPLOSION.get());
        if (!module.isEmpty() && item.canInstall(WeaponModule.SLOT_BARREL, module)) EnergyWeaponItem.setModule(weapon, WeaponModule.SLOT_BARREL, module);
        if (level >= 1) {
            BatteryItem battery = random.nextInt(120) < 100 ? MOItems.BATTERY.get() : MOItems.HC_BATTERY.get();
            EnergyWeaponItem.setModule(weapon, WeaponModule.SLOT_BATTERY, battery.charged());
            if (random.nextInt(310) < 10) {
                ItemStack scope = new ItemStack(MOItems.SNIPER_SCOPE.get());
                if (item.canInstall(WeaponModule.SLOT_SIGHTS, scope)) EnergyWeaponItem.setModule(weapon, WeaponModule.SLOT_SIGHTS, scope);
            }
        }
        EnergyWeaponItem.setEnergy(weapon, EnergyWeaponItem.getCapacity(weapon));
        setItemSlot(EquipmentSlot.MAINHAND, weapon);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(item.getRange(weapon) - 2);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel server) || !(getMainHandItem().getItem() instanceof EnergyWeaponItem weapon)) return;
        getLookControl().setLookAt(target, 30, 30);
        lookAt(target, 360, 360);
        weapon.fireFromMob(server, this, getMainHandItem());
        swing(InteractionHand.MAIN_HAND);
    }

    /** 1.7.10: damage x (0.2 x difficulty, max 0.6) + level x 0.133 + 0.3 legendary. */
    @Override
    public float weaponDamageScale() {
        return Mth.clamp(0.6f / 3 * level().getDifficulty().getId(), 0, 0.6f) + getAndroidLevel() * (0.4f / 3) + (isLegendary() ? 0.3f : 0);
    }

    /** 1.7.10: (3 - difficulty) x 4 more spread. */
    @Override
    public float weaponAccuracyAdd() {
        return (3 - level().getDifficulty().getId()) * 4f;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // the weapon only drops by the 1.7.10 rule below, not by the vanilla 8.5% equipment drop
        ItemStack weapon = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        super.dropCustomDeathLoot(level, source, recentlyHit);
        setItemSlot(EquipmentSlot.MAINHAND, weapon);
        float looting = MeleeRogueAndroid.lootingBonus(level, source);
        int packs = random.nextInt(2 + (int) (looting * 10));
        for (int i = 0; i < packs; i++) spawnAtLocation(level, new ItemStack(MOItems.ENERGY_PACK.get()));
        if (recentlyHit && (random.nextFloat() < 0.15f + looting || isLegendary())) spawnAtLocation(level, randomPart());
        if (recentlyHit && (random.nextInt(400) - looting * 10 < 5 || isLegendary())) spawnAtLocation(level, getMainHandItem().copy());
    }
}
