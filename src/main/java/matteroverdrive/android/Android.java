package matteroverdrive.android;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.init.MOAttachments;
import matteroverdrive.init.MOAttributes;
import matteroverdrive.init.MOItems;
import matteroverdrive.init.MOSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 1.7.10 AndroidPlayer's behaviour: energy (the battery slot, or the built-in store), the transformation, and what
 * being an android changes every tick (food from energy, no potion effects, sinking, no drowning, jumps cost energy,
 * half fall distance, glitching when hurt, slow when out of power).
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID)
public final class Android {
    public static final int RECHARGE_AMOUNT_ON_RESPAWN = 64000;
    public static final int BUILTIN_ENERGY_TRANSFER = 1024;
    public static final int TRANSFORM_TIME = 20 * 34;
    public static final int ENERGY_FOOD_MULTIPLY = 256;
    public static final int ENERGY_PER_JUMP = 512;
    public static final float FALL_NEGATE = 0.5f;
    public static final boolean TRANSFORMATION_DEATH = true;
    public static final boolean REMOVE_POTION_EFFECTS = true;
    public static final ResourceKey<DamageType> TRANSFORMATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_transformation"));
    public static final ResourceKey<DamageType> SHOCKWAVE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_shockwave"));
    private static final ResourceLocation OUT_OF_POWER = ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android_out_of_power");

    public static AndroidData get(Player player) {
        return player.getData(MOAttachments.ANDROID);
    }

    public static boolean isAndroid(Player player) {
        return get(player).isAndroid();
    }

    /** Pushes a change to the player and everyone tracking them. */
    public static void sync(Player player) {
        get(player).dirty = false;
        if (!player.level().isClientSide()) player.syncData(MOAttachments.ANDROID);
    }

    // --- energy -------------------------------------------------------------------------------------

    private static IEnergyStorage battery(AndroidData data) {
        if (data.getStack(AndroidData.SLOT_BATTERY).isEmpty()) return null;
        return data.getStack(AndroidData.SLOT_BATTERY).getCapability(Capabilities.EnergyStorage.ITEM);
    }

    public static int getEnergy(Player player) {
        AndroidData data = get(player);
        if (player.isCreative()) return getMaxEnergy(player);
        IEnergyStorage battery = battery(data);
        return battery != null ? battery.getEnergyStored() : data.energy;
    }

    public static int getMaxEnergy(Player player) {
        IEnergyStorage battery = battery(get(player));
        return battery != null ? battery.getMaxEnergyStored() : AndroidData.MAX_ENERGY;
    }

    /** 1.7.10 extractEnergyRaw: from the battery, else at most 1024 FE from the built-in store; free in creative. */
    public static int extractEnergy(Player player, int amount, boolean simulate) {
        if (player.isCreative()) return amount;
        AndroidData data = get(player);
        IEnergyStorage battery = battery(data);
        int extracted;
        if (battery != null) {
            extracted = battery.extractEnergy(amount, simulate);
        } else {
            extracted = Math.min(Math.min(data.energy, amount), BUILTIN_ENERGY_TRANSFER);
            if (!simulate) data.energy = Mth.clamp(data.energy - extracted, 0, AndroidData.MAX_ENERGY);
        }
        if (extracted > 0 && !simulate) data.dirty = true;
        return extracted;
    }

    public static int receiveEnergy(Player player, int amount, boolean simulate) {
        AndroidData data = get(player);
        IEnergyStorage battery = battery(data);
        int received;
        if (battery != null) {
            received = battery.receiveEnergy(amount, simulate);
        } else {
            received = Math.min(Math.min(AndroidData.MAX_ENERGY - data.energy, amount), BUILTIN_ENERGY_TRANSFER);
            if (!simulate) data.energy += received;
        }
        if (received > 0 && !simulate) data.dirty = true;
        return received;
    }

    /** 1.7.10 extractEnergyScaled: scaled by the battery-use attribute. */
    public static void extractEnergyScaled(Player player, int amount) {
        extractEnergy(player, (int) (amount * player.getAttributeValue(MOAttributes.BATTERY_USE)), false);
    }

    public static boolean hasEnoughEnergyScaled(Player player, int amount) {
        int needed = (int) Math.ceil(amount * player.getAttributeValue(MOAttributes.BATTERY_USE));
        return extractEnergy(player, amount, true) >= needed;
    }

    // --- becoming (and stopping being) an android -------------------------------------------------------

    /** 1.7.10 startConversion: the red pill starts a 34 s transformation. */
    public static void startTransformation(ServerPlayer player) {
        AndroidData data = get(player);
        if (data.isAndroid() || data.isTurning()) return;
        data.turning = TRANSFORM_TIME;
        sync(player);
    }

    public static void setAndroid(Player player, boolean android) {
        AndroidData data = get(player);
        data.android = android;
        if (!android) {
            removeOutOfPower(player);
            removeParts(player);
            removeStatAttributes(player);
        }
        sync(player);
    }

    /** 1.7.10 resetUnlocked: forget every stat, returning half the XP levels they cost. */
    public static int resetStats(Player player) {
        AndroidData data = get(player);
        int xp = getResetXP(data);
        data.stats.clear();
        removeStatAttributes(player);
        sync(player);
        return xp;
    }

    public static int getResetXP(AndroidData data) {
        int xp = 0;
        for (var entry : data.stats.entrySet()) {
            BioticStat stat = BioticStats.get(entry.getKey());
            if (stat != null) xp += stat.xp();
        }
        return xp / 2;
    }

    public static boolean tryUnlock(Player player, BioticStat stat, int level) {
        AndroidData data = get(player);
        if (!stat.canBeUnlocked(player, data, level)) return false;
        data.stats.put(stat.id(), level);
        stat.onUnlock(player, level);
        sync(player);
        return true;
    }

    /** Sets the transformation countdown (tests, commands). */
    public static void setTurning(Player player, int ticks) {
        get(player).turning = ticks;
        sync(player);
    }

    public static void glitch(Player player, int ticks) {
        get(player).glitchTime = ticks;
    }

    // --- ticking ------------------------------------------------------------------------------------

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        tick(event.getEntity());
    }

    /** One tick of android behaviour for this player (public for tests and commands). */
    public static void tick(Player player) {
        AndroidData data = get(player);
        if (data.glitchTime > 0) data.glitchTime--;
        if (!(player instanceof ServerPlayer server)) return;
        if (data.isAndroid()) {
            if (getEnergy(player) > 0) {
                if (player.getFoodData().needsFood()) {
                    int foodNeeded = 20 - player.getFoodData().getFoodLevel();
                    int extracted = extractEnergy(player, foodNeeded * ENERGY_FOOD_MULTIPLY, false);
                    player.getFoodData().eat(extracted / ENERGY_FOOD_MULTIPLY, 0);
                }
                removeOutOfPower(player);
                if (REMOVE_POTION_EFFECTS && !player.getActiveEffects().isEmpty()) player.removeAllEffects();
            } else {
                manageOutOfPower(server);
            }
            manageCharging(player);
            manageParts(player, data);
            if (player.isInWater()) player.setDeltaMovement(player.getDeltaMovement().add(0, -0.007, 0));
            if (player.getAirSupply() < 0) player.setAirSupply(0);
            for (BioticStat stat : BioticStats.all()) {
                int level = data.getUnlockedLevel(stat);
                boolean enabled = level > 0 && stat.isEnabled(player, data, level);
                applyAttributes(player, stat, level, enabled);
                if (level <= 0) continue;
                stat.changeAndroidStats(server, data, level, enabled);
                if (enabled) stat.onAndroidTick(server, data, level);
            }
        }
        manageTurning(server, data);
        if (data.dirty && player.tickCount % 10 == 0) sync(player);
    }

    /** 1.7.10 manageEquipmentAttributeModifiers: each fitted bionic part adds its attribute modifiers (one id per slot). */
    private static void manageParts(Player player, AndroidData data) {
        for (int slot = AndroidData.SLOT_HEAD; slot <= AndroidData.SLOT_OTHER; slot++) {
            ItemStack partStack = data.getStack(slot);
            var mods = partStack.getItem() instanceof matteroverdrive.item.android.BionicPartItem part ? part.modifiers(partStack)
                    : java.util.List.<matteroverdrive.item.android.BionicPartItem.Mod>of();
            java.util.Set<ResourceLocationKey> applied = new java.util.HashSet<>();
            for (var mod : mods) {
                AttributeInstance attribute = player.getAttribute(mod.attribute());
                if (attribute == null) continue;
                var id = matteroverdrive.item.android.BionicPartItem.modifierId(slot, mod.suffix());
                var modifier = new AttributeModifier(id, mod.amount(), mod.operation());
                if (!modifier.equals(attribute.getModifier(id))) attribute.addOrUpdateTransientModifier(modifier);
                applied.add(new ResourceLocationKey(mod.attribute(), id));
            }
            removePartModifiers(player, slot, applied);
        }
    }

    private record ResourceLocationKey(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                       net.minecraft.resources.ResourceLocation id) {}

    private static final java.util.List<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> PART_ATTRIBUTES = java.util.List.of(
            Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED, MOAttributes.GLITCH_TIME,
            MOAttributes.BATTERY_USE);

    /** Removes the slot's part modifiers that aren't in keep. */
    private static void removePartModifiers(Player player, int slot, java.util.Set<ResourceLocationKey> keep) {
        for (var holder : PART_ATTRIBUTES) {
            AttributeInstance attribute = player.getAttribute(holder);
            if (attribute == null) continue;
            for (String suffix : matteroverdrive.item.android.BionicPartItem.MOD_SUFFIXES) {
                var id = matteroverdrive.item.android.BionicPartItem.modifierId(slot, suffix);
                if (!keep.contains(new ResourceLocationKey(holder, id))) attribute.removeModifier(id);
            }
        }
    }

    /** 1.7.10 manageStatAttributeModifiers: a stat's modifiers are held while it is unlocked and enabled. */
    private static void applyAttributes(Player player, BioticStat stat, int level, boolean enabled) {
        for (var entry : stat.attributes(Math.max(level, 1)).entrySet()) {
            AttributeInstance attribute = player.getAttribute(entry.getKey());
            if (attribute == null) continue;
            AttributeModifier modifier = entry.getValue();
            if (enabled) {
                if (!modifier.equals(attribute.getModifier(modifier.id()))) attribute.addOrUpdateTransientModifier(modifier);
            } else {
                attribute.removeModifier(modifier.id());
            }
        }
    }

    private static void removeStatAttributes(Player player) {
        for (BioticStat stat : BioticStats.all()) applyAttributes(player, stat, 1, false);
    }

    private static void removeParts(Player player) {
        for (int slot = AndroidData.SLOT_HEAD; slot <= AndroidData.SLOT_OTHER; slot++) removePartModifiers(player, slot, java.util.Set.of());
    }

    /** 1.7.10 manageCharging: sneaking with a battery in hand drains it into the android. */
    private static void manageCharging(Player player) {
        ItemStack held = player.getMainHandItem();
        if (!player.isShiftKeyDown() || !(held.is(MOItems.BATTERY.get()) || held.is(MOItems.HC_BATTERY.get()))) return;
        IEnergyStorage item = held.getCapability(Capabilities.EnergyStorage.ITEM);
        if (item == null) return;
        int free = getMaxEnergy(player) - getEnergy(player);
        if (free <= 0) return;
        int canTake = receiveEnergy(player, free, true);
        int taken = item.extractEnergy(canTake, false);
        receiveEnergy(player, taken, false);
    }

    /** 1.7.10 manageOutOfPower: half speed (the client glitches every 3 s, see the HUD). */
    private static void manageOutOfPower(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(OUT_OF_POWER)) {
            speed.addTransientModifier(new AttributeModifier(OUT_OF_POWER, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeOutOfPower(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(OUT_OF_POWER);
    }

    /** 1.7.10 manageTurning: sickness while turning, a hit every 2 s, then android - and (by default) death. */
    private static void manageTurning(ServerPlayer player, AndroidData data) {
        if (data.turning <= 0) return;
        ServerLevel level = player.level();
        var damage = level.damageSources().source(TRANSFORMATION_DAMAGE);
        data.turning--;
        if (data.turning > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, TRANSFORM_TIME));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TRANSFORM_TIME, 1));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, TRANSFORM_TIME));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TRANSFORM_TIME));
            if (data.turning % 40 == 0) {
                player.hurtServer(level, damage, 0.1f);
                playGlitch(player, 0.2f);
            }
        } else {
            setAndroid(player, true);
            playGlitch(player, 0.8f);
            if (!player.isCreative() && !level.getLevelData().isHardcore() && TRANSFORMATION_DEATH) {
                player.hurtServer(level, damage, Float.MAX_VALUE);
            }
        }
        data.dirty = true;
        sync(player);
    }

    public static void playGlitch(Player player, float volume) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.GLITCH.get(), SoundSource.PLAYERS,
                volume, 0.9f + player.getRandom().nextFloat() * 0.2f);
    }

    // --- events -------------------------------------------------------------------------------------

    /** Fires on both sides: the client moves the player, the server pays the energy. */
    @SubscribeEvent
    static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isAndroid(player)) return;
        if (!player.level().isClientSide()) extractEnergyScaled(player, ENERGY_PER_JUMP);
        AndroidData data = get(player);
        for (BioticStat stat : BioticStats.all()) {
            int level = data.getUnlockedLevel(stat);
            if (level > 0 && stat.isEnabled(player, data, level)) stat.onJump(player, data, level);
        }
    }

    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && isAndroid(player)) {
            if (player instanceof ServerPlayer server) {
                AndroidData data = get(player);
                for (BioticStat stat : BioticStats.all()) {
                    int level = data.getUnlockedLevel(stat);
                    if (level > 0 && stat.isEnabled(player, data, level)) stat.onFall(server, data, level, event.getDistance());
                }
            }
            event.setDistance(event.getDistance() * FALL_NEGATE);
        }
    }

    /** 1.7.10 LivingAttackEvent / LivingHurtEvent on the stats: the shield and nano armour. */
    @SubscribeEvent
    static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isAndroid(player)) return;
        AndroidData data = get(player);
        float amount = event.getAmount();
        for (BioticStat stat : BioticStats.all()) {
            int level = data.getUnlockedLevel(stat);
            if (level <= 0 || !stat.isEnabled(player, data, level)) continue;
            amount = stat.onIncomingDamage(player, data, level, event.getSource(), amount);
            if (amount < 0) {
                event.setCanceled(true);
                return;
            }
        }
        event.setAmount(amount);
    }

    /** The ability key (1.7.10 PacketBioticActionKey). */
    public static void onActionKey(ServerPlayer player) {
        AndroidData data = get(player);
        if (!data.isAndroid()) return;
        for (BioticStat stat : BioticStats.all()) {
            int level = data.getUnlockedLevel(stat);
            if (level > 0 && stat.isEnabled(player, data, level)) stat.onActionKey(player, data, level);
        }
    }

    /** 1.7.10 PacketTeleportPlayer: checked again on the server (range, energy, cooldown). */
    public static void teleport(ServerPlayer player, net.minecraft.world.phys.Vec3 target) {
        AndroidData data = get(player);
        BioticStat teleport = BioticStats.TELEPORT;
        int level = data.getUnlockedLevel(teleport);
        if (!data.isAndroid() || level <= 0 || !teleport.isEnabled(player, data, level)) return;
        if (target.distanceTo(player.getEyePosition()) > StatImpls.Teleport.MAX_TELEPORT_DISTANCE + 4) return;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.ANDROID_TELEPORT.get(), SoundSource.PLAYERS, 0.2f, 1);
        player.teleportTo(target.x, target.y, target.z);
        player.fallDistance = 0;
        player.level().playSound(null, target.x, target.y, target.z, MOSounds.ANDROID_TELEPORT.get(), SoundSource.PLAYERS, 0.2f, 1);
        extractEnergyScaled(player, StatImpls.Teleport.ENERGY_PER_TELEPORT);
        data.setEffect(StatImpls.Teleport.LAST, player.level().getGameTime() + StatImpls.Teleport.TELEPORT_DELAY);
        sync(player);
    }

    /** 1.7.10 onEntityHurt: a short glitch (scaled by the glitch-time attribute) and its sound. */
    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && isAndroid(player) && event.getNewDamage() > 0) {
            glitch(player, (int) (10 * player.getAttributeValue(MOAttributes.GLITCH_TIME)));
            sync(player);
            playGlitch(player, 0.2f);
        }
    }

    /** 1.7.10 onPlayerRespawn: androids come back with at least 64000 FE. */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (event.isEndConquered() || !isAndroid(player)) return;
        for (int i = 0; i < 1000 && getEnergy(player) < RECHARGE_AMOUNT_ON_RESPAWN; i++) {
            if (receiveEnergy(player, RECHARGE_AMOUNT_ON_RESPAWN, false) <= 0) break;
        }
        sync(player);
    }

    @SubscribeEvent
    static void onAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MOAttributes.GLITCH_TIME);
        event.add(EntityType.PLAYER, MOAttributes.BATTERY_USE);
    }

    private Android() {}
}
