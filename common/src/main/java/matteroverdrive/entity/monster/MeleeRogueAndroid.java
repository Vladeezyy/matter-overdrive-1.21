package matteroverdrive.entity.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityMeleeRougeAndroidMob: speed 0.3; 10% (+looting) chance to drop a part, always when legendary. */
public class MeleeRogueAndroid extends RogueAndroid {
    public MeleeRogueAndroid(EntityType<? extends MeleeRogueAndroid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return RogueAndroid.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(3, new matteroverdrive.entity.ai.MoveAlongPathGoal(this, 1.0));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, true,
                target -> RogueAndroid.isEnemy(target)));
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit && (random.nextFloat() < 0.1f + lootingBonus(looting) || isLegendary())) {
            spawnAtLocation(randomPart());
        }
    }

    static float lootingBonus(int looting) {
        return Math.min(looting, 10) / 10f;
    }
}
