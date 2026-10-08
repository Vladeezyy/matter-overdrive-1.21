package matteroverdrive.entity.animal;

import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 EntityFailedPig: a vanilla pig with the failed texture and sounds; breeds failed pigs. */
public class FailedPig extends Pig {
    public FailedPig(EntityType<? extends FailedPig> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MOSounds.FAILED_ANIMAL_IDLE_PIG.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MOSounds.FAILED_ANIMAL_IDLE_PIG.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MOSounds.FAILED_ANIMAL_DIE.get();
    }

    @Override
    public @Nullable Pig getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return MOEntities.FAILED_PIG.get().create(level, EntitySpawnReason.BREEDING);
    }
}
