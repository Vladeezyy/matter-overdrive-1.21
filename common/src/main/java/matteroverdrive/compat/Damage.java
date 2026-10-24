package matteroverdrive.compat;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** The 1.21 {@code damageSources().source(type, ...)} (private in 1.20.1 vanilla, so Fabric has no AT for it) */
public final class Damage {
    private Damage() {}

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity causing, @Nullable Entity direct) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity entity) {
        return source(level, type, entity, entity);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }
}
