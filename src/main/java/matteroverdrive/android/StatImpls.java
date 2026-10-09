package matteroverdrive.android;

import java.util.Map;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The 1.7.10 biotic stats that do something (data/biostats/*), with their numbers. */
final class StatImpls {
    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, path);
    }

    static String percent(float value) {
        return Math.round(value * 100) + "%";
    }

    /** 1.7.10 BioticStatSpeed: +10% speed per level (multiplied), only with energy. */
    static class Speed extends BioticStat {
        Speed() {
            super("speed", 18);
        }

        float speedModify(int level) {
            return level * 0.1f;
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {percent(speedModify(level))};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && Android.getEnergy(player) > 0;
        }

        @Override
        public Map<Holder<Attribute>, AttributeModifier> attributes(int level) {
            return Map.of(Attributes.MOVEMENT_SPEED, new AttributeModifier(StatImpls.id("android_speed"), speedModify(level),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** 1.7.10 BioticStatAttack: attack damage x(1 + (level + 1) * 5%). */
    static class Attack extends BioticStat {
        Attack() {
            super("attack", 30);
        }

        float attackPower(int level) {
            return (level + 1) * 0.05f;
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {percent(attackPower(level))};
        }

        @Override
        public Map<Holder<Attribute>, AttributeModifier> attributes(int level) {
            return Map.of(Attributes.ATTACK_DAMAGE, new AttributeModifier(StatImpls.id("android_attack"), attackPower(level),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** 1.7.10 BioticStatNanoArmor: (level + 1) * 6% less damage, while powered. */
    static class NanoArmor extends BioticStat {
        NanoArmor() {
            super("nano_armor", 30);
        }

        float damageNegate(int level) {
            return (1 + level) * 0.06f;
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {percent(damageNegate(level))};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && Android.getEnergy(player) > 0;
        }

        @Override
        public float onIncomingDamage(ServerPlayer player, AndroidData data, int level, DamageSource source, float amount) {
            return amount * (1 - damageNegate(level));
        }
    }

    /** 1.7.10 BiostatNanobots: every second heals 0.6 for 640 FE (32 FE x 20 ticks), while hurt. */
    static class Nanobots extends BioticStat {
        static final float REGEN_AMOUNT_PER_TICK = 0.03f;
        static final int ENERGY_PER_REGEN = 32;

        Nanobots() {
            super("nanobots", 26);
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && Android.getEnergy(player) > 0;
        }

        @Override
        public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {
            if (player.level().getGameTime() % 20 == 0 && player.isAlive() && player.getHealth() < player.getMaxHealth()
                    && Android.hasEnoughEnergyScaled(player, ENERGY_PER_REGEN)) {
                player.heal(REGEN_AMOUNT_PER_TICK * 20);
                Android.extractEnergyScaled(player, ENERGY_PER_REGEN * 20);
            }
        }
    }

    /** 1.7.10 BioticStatFlotation: cancels the android's sinking in water. */
    static class Flotation extends BioticStat {
        Flotation() {
            super("floatation", 14);
        }

        @Override
        public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {
            if (player.isInWater()) player.setDeltaMovement(player.getDeltaMovement().add(0, 0.007, 0));
        }
    }

    /** 1.7.10 BioticStatHighJump: sneak-jumps go 0.5 higher for 1024 FE. */
    static class HighJump extends BioticStat {
        static final int ENERGY_PER_JUMP = 1024;

        HighJump() {
            super("high_jump", 36);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {ENERGY_PER_JUMP + " FE"};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && Android.hasEnoughEnergyScaled(player, ENERGY_PER_JUMP);
        }

        @Override
        public void onJump(Player player, AndroidData data, int level) {
            // 1.7.10 checked sneaking in the HUD text only; the boost itself applied on every jump
            if (!player.level().isClientSide()) Android.extractEnergyScaled(player, ENERGY_PER_JUMP);
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.5, 0));
        }
    }

    /** 1.7.10 BioticStatNightVision: toggled with the ability key; 16 FE per tick while on. */
    static class NightVision extends BioticStat {
        static final int ENERGY_PER_TICK = 16;
        static final String KEY = "Nightvision";

        NightVision() {
            super("nightvision", 28);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {ENERGY_PER_TICK + " FE"};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && Android.hasEnoughEnergyScaled(player, ENERGY_PER_TICK);
        }

        @Override
        public boolean isActive(Player player, AndroidData data, int level) {
            return data.getFlag(KEY);
        }

        @Override
        public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {
            if (isActive(player, data, level)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 500, 0, false, false));
                Android.extractEnergyScaled(player, ENERGY_PER_TICK);
            }
        }

        @Override
        public void changeAndroidStats(ServerPlayer player, AndroidData data, int level, boolean enabled) {
            if (!enabled && isActive(player, data, level)) setActive(player, data, false);
        }

        @Override
        public void onActionKey(ServerPlayer player, AndroidData data, int level) {
            if (id().equals(data.getActiveStat())) setActive(player, data, !data.getFlag(KEY));
        }

        void setActive(ServerPlayer player, AndroidData data, boolean active) {
            data.setEffect(KEY, active ? 1 : 0);
            if (!active) player.removeEffect(MobEffects.NIGHT_VISION);
            player.level().playSound(null, player.blockPosition(), active ? MOSounds.NIGHT_VISION.get() : MOSounds.POWER_DOWN.get(),
                    SoundSource.PLAYERS, 0.05f + player.getRandom().nextFloat() * 0.1f, 0.95f + player.getRandom().nextFloat() * 0.1f);
            Android.sync(player);
        }
    }

    /** 1.7.10 BioticStatCloak: toggled invisibility, 128 FE per tick, off while using an item. */
    static class Cloak extends BioticStat {
        static final int ENERGY_PER_TICK = 128;
        static final String KEY = "Cloaked";

        Cloak() {
            super("cloak", 36);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {ENERGY_PER_TICK + " FE"};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && Android.hasEnoughEnergyScaled(player, ENERGY_PER_TICK);
        }

        @Override
        public boolean isActive(Player player, AndroidData data, int level) {
            return data.getFlag(KEY) && !player.isUsingItem();
        }

        @Override
        public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {
            if (isActive(player, data, level)) {
                if (!player.isInvisible()) player.level().playSound(null, player.blockPosition(), MOSounds.CLOAK_ON.get(), SoundSource.PLAYERS, 1, 1);
                player.setInvisible(true);
                Android.extractEnergyScaled(player, ENERGY_PER_TICK);
            } else if (player.isInvisible()) {
                player.level().playSound(null, player.blockPosition(), MOSounds.CLOAK_OFF.get(), SoundSource.PLAYERS, 1, 1);
                player.setInvisible(false);
            }
        }

        @Override
        public void changeAndroidStats(ServerPlayer player, AndroidData data, int level, boolean enabled) {
            if (!enabled && data.getFlag(KEY)) {
                data.setEffect(KEY, 0);
                player.setInvisible(false);
                Android.sync(player);
            }
        }

        @Override
        public void onActionKey(ServerPlayer player, AndroidData data, int level) {
            if (id().equals(data.getActiveStat())) {
                data.setEffect(KEY, data.getFlag(KEY) ? 0 : 1);
                Android.sync(player);
            }
        }
    }

    /** 1.7.10 BioticStatShield: 8 s of protection from projectiles and explosions, 16 s cooldown, 64 FE/t + 256 FE per damage. */
    static class Shield extends BioticStat {
        static final int ENERGY_PER_TICK = 64;
        static final int ENERGY_PER_DAMAGE = 256;
        static final int SHIELD_COOLDOWN = 20 * 16;
        static final int SHIELD_TIME = 20 * 8;
        static final String KEY = "Shield", LAST_USE = "ShieldLastUse";

        Shield() {
            super("shield", 36);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[0];
        }

        @Override
        public boolean isActive(Player player, AndroidData data, int level) {
            return data.getFlag(KEY);
        }

        @Override
        public boolean showOnHud(AndroidData data, int level) {
            return id().equals(data.getActiveStat()) || data.getFlag(KEY);
        }

        long remaining(Player player, AndroidData data) {
            return data.getEffect(LAST_USE) - player.level().getGameTime();
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            long left = remaining(player, data);
            return super.isEnabled(player, data, level) && Android.hasEnoughEnergyScaled(player, ENERGY_PER_TICK)
                    && (left <= 0 || left > SHIELD_COOLDOWN);
        }

        @Override
        public int getDelay(Player player, AndroidData data, int level) {
            return (int) Math.max(0, remaining(player, data));
        }

        @Override
        public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {
            if (data.getFlag(KEY)) Android.extractEnergyScaled(player, ENERGY_PER_TICK);
        }

        @Override
        public void changeAndroidStats(ServerPlayer player, AndroidData data, int level, boolean enabled) {
            if (data.getFlag(KEY) && remaining(player, data) < SHIELD_COOLDOWN) {
                data.setEffect(KEY, 0);
                Android.sync(player);
                player.level().playSound(null, player.blockPosition(), MOSounds.SHIELD_POWER_DOWN.get(), SoundSource.PLAYERS,
                        0.6f + player.getRandom().nextFloat() * 0.2f, 1);
            }
        }

        @Override
        public void onActionKey(ServerPlayer player, AndroidData data, int level) {
            if (id().equals(data.getActiveStat()) && remaining(player, data) <= 0) {
                data.setEffect(KEY, 1);
                data.setEffect(LAST_USE, player.level().getGameTime() + SHIELD_COOLDOWN + SHIELD_TIME);
                Android.sync(player);
                player.level().playSound(null, player.blockPosition(), MOSounds.SHIELD_POWER_UP.get(), SoundSource.PLAYERS,
                        0.6f + player.getRandom().nextFloat() * 0.2f, 1);
            }
        }

        static boolean isDamageValid(DamageSource source) {
            return source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_PROJECTILE);
        }

        @Override
        public float onIncomingDamage(ServerPlayer player, AndroidData data, int level, DamageSource source, float amount) {
            if (!data.getFlag(KEY) || !isDamageValid(source)) return amount;
            if (source.getDirectEntity() != null) {
                net.minecraft.world.entity.Entity attacker = source.getDirectEntity();
                matteroverdrive.network.AndroidPayloads.sendShieldHit(player, new net.minecraft.world.phys.Vec3(attacker.getX() - player.getX(),
                        attacker.getY() - (player.getY() + 1.5), attacker.getZ() - player.getZ()));
                player.level().playSound(null, player.blockPosition(), MOSounds.SHIELD_HIT.get(), SoundSource.PLAYERS, 0.5f,
                        0.9f + player.getRandom().nextFloat() * 0.2f);
            }
            int required = Mth.ceil(amount * ENERGY_PER_DAMAGE);
            // 1.7.10 LivingAttackEvent: enough energy blocks the hit entirely
            if (Android.hasEnoughEnergyScaled(player, required)) {
                Android.extractEnergyScaled(player, required);
                return -1;
            }
            // 1.7.10 LivingHurtEvent: otherwise the damage shrinks by the share of energy available
            int scaled = (int) (required * player.getAttributeValue(matteroverdrive.init.MOAttributes.BATTERY_USE));
            int available = Android.extractEnergy(player, scaled, true);
            return scaled <= 0 ? amount : amount * available / scaled;
        }
    }

    /** 1.7.10 BioticStatShockwave: knocks back and hurts everything within 5-10 blocks; 12 s cooldown. */
    static class Shockwave extends BioticStat {
        static final int DELAY = 20 * 12;
        static final String LAST_USE = "ShockLastUse";

        Shockwave() {
            super("shockwave", 32);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {10};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && getDelay(player, data, level) <= 0;
        }

        @Override
        public int getDelay(Player player, AndroidData data, int level) {
            return (int) Math.max(0, data.getEffect(LAST_USE) - player.level().getGameTime());
        }

        /** Sneaking while falling with the shockwave selected dives faster. */
        @Override
        public void onAndroidTick(ServerPlayer player, AndroidData data, int level) {
            Vec3 m = player.getDeltaMovement();
            if (id().equals(data.getActiveStat()) && !player.onGround() && m.y < 0 && player.isShiftKeyDown()) {
                Vec3 dir = m.subtract(0, 1, 0).normalize();
                player.setDeltaMovement(m.add(-dir.x * 0.2, -dir.y * 0.2, -dir.z * 0.2));
                player.hurtMarked = true;
            }
        }

        @Override
        public void onActionKey(ServerPlayer player, AndroidData data, int level) {
            if (id().equals(data.getActiveStat())) create(player, data, 5);
        }

        @Override
        public void onFall(ServerPlayer player, AndroidData data, int level, double distance) {
            if (player.isShiftKeyDown()) create(player, data, (float) distance);
        }

        void create(ServerPlayer player, AndroidData data, float distance) {
            if (data.getEffect(LAST_USE) >= player.level().getGameTime()) return;
            float range = Mth.clamp(distance, 5, 10);
            float power = Mth.clamp(distance, 1, 3) * 0.8f;
            AABB area = player.getBoundingBox().inflate(range);
            var source = player.level().damageSources().source(Android.SHOCKWAVE_DAMAGE, player);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                Vec3 dir = player.position().subtract(target.position());
                double multiply = range / Math.max(1, dir.length());
                dir = dir.normalize();
                target.push(dir.x * power * multiply, power * 0.2f, dir.z * power * multiply);
                target.hurtMarked = true;
                target.hurtServer(player.serverLevel(), source, power * 3);
            }
            data.setEffect(LAST_USE, player.level().getGameTime() + DELAY);
            Android.sync(player);
            player.level().playSound(null, player.blockPosition(), MOSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 1,
                    0.9f + player.getRandom().nextFloat() * 0.1f);
            player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.6, 0.6, 0.6, 0.02);
        }
    }

    /** 1.7.10 BioticStatTeleport: release the ability key to jump up to 32 blocks to the targeted spot; 4096 FE, 2 s. */
    static class Teleport extends BioticStat {
        static final int TELEPORT_DELAY = 40;
        static final int ENERGY_PER_TELEPORT = 4096;
        static final int MAX_TELEPORT_HEIGHT_CHECK = 8;
        static final int MAX_TELEPORT_DISTANCE = 32;
        static final String LAST = "LastTeleport";

        Teleport() {
            super("teleport", 48);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {AndroidClientHooks.abilityKeyName.get(), ENERGY_PER_TELEPORT + " FE"};
        }

        @Override
        public boolean isEnabled(Player player, AndroidData data, int level) {
            return super.isEnabled(player, data, level) && data.getEffect(LAST) <= player.level().getGameTime()
                    && Android.hasEnoughEnergyScaled(player, ENERGY_PER_TELEPORT) && id().equals(data.getActiveStat());
        }

        @Override
        public boolean showOnHud(AndroidData data, int level) {
            return id().equals(data.getActiveStat());
        }
    }

    /** 1.7.10 BioticStatFlashCooling: 20% chance that an overheat is cancelled and the weapon cooled at once. */
    static class FlashCooling extends BioticStat {
        static final float COOLDOWN_CHANGE = 0.2f;

        FlashCooling() {
            super("flash_cooling", 28);
        }

        @Override
        public Object[] detailArgs(int level) {
            return new Object[] {percent(COOLDOWN_CHANGE)};
        }
    }

    private StatImpls() {}
}
