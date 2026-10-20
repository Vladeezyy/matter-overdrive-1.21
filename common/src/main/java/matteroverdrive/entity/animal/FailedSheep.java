package matteroverdrive.entity.animal;

import matteroverdrive.init.MOEntities;
import matteroverdrive.init.MOSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 EntityFailedSheep: a vanilla sheep with the failed texture and sounds; lambs mix the parents' wool colours. */
public class FailedSheep extends Sheep {
    public FailedSheep(EntityType<? extends FailedSheep> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return MOSounds.FAILED_ANIMAL_IDLE_SHEEP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MOSounds.FAILED_ANIMAL_IDLE_SHEEP.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MOSounds.FAILED_ANIMAL_DIE.get();
    }

    @Override
    public @Nullable Sheep getBreedOffspring(ServerLevel level, AgeableMob mate) {
        FailedSheep lamb = MOEntities.FAILED_SHEEP.get().create(level, EntitySpawnReason.BREEDING);
        if (lamb != null && mate instanceof Sheep other) {
            lamb.setColor(getOffspringColor(level, this, other));   // AT before 1.21.4
        }
        return lamb;
    }
}
