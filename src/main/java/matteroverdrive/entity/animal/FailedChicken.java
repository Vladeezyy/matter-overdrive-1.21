package matteroverdrive.entity.animal;

import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 EntityFailedChicken: a vanilla chicken with the failed texture and sounds; breeds failed chickens. */
public class FailedChicken extends Chicken {
    public FailedChicken(EntityType<? extends FailedChicken> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MOSounds.FAILED_ANIMAL_IDLE_CHICKEN.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MOSounds.FAILED_ANIMAL_IDLE_CHICKEN.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MOSounds.FAILED_ANIMAL_DIE.get();
    }

    @Override
    public @Nullable Chicken getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return MOEntities.FAILED_CHICKEN.get().create(level);
    }
}
