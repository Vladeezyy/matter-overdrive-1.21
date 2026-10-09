package matteroverdrive.item.weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import matteroverdrive.entity.PlasmaBolt;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOSounds;
import matteroverdrive.util.MOText;
import matteroverdrive.item.BatteryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 1.7.10 EnergyWeapon. Fires with the attack key (held for automatic fire), aims/zooms with use. Every shot costs
 * energy and adds heat; at max heat the weapon overheats until it cools below 2. Modules installed at a weapon
 * station change the stats. Right-clicking with too little energy reloads from an energy pack in the inventory.
 */
public abstract class EnergyWeaponItem extends Item {
    public static final int CAPACITY = 32000;
    public static final int TRANSFER = 128;

    private final int defaultRange;
    private final int baseCooldown;
    private final float baseDamage;
    private final int energyPerShot;
    private final int baseMaxHeat;
    private final float shotSpeed;
    private final float zoom;

    protected EnergyWeaponItem(Properties properties, int defaultRange, int baseCooldown, float baseDamage, int energyPerShot,
                               int baseMaxHeat, float shotSpeed, float zoom) {
        super(properties.stacksTo(1));
        this.defaultRange = defaultRange;
        this.baseCooldown = baseCooldown;
        this.baseDamage = baseDamage;
        this.energyPerShot = energyPerShot;
        this.baseMaxHeat = baseMaxHeat;
        this.shotSpeed = shotSpeed;
        this.zoom = zoom;
    }

    // --- stats (1.7.10 getters, all passed through the installed modules) -----------------------

    /** The installed modules, indexed by {@link WeaponModule} slot (empty stacks where nothing is installed). */
    public static NonNullList<ItemStack> getModuleSlots(ItemStack weapon) {
        NonNullList<ItemStack> slots = NonNullList.withSize(WeaponModule.SLOTS, ItemStack.EMPTY);
        weapon.getOrDefault(MODataComponents.WEAPON_MODULES.get(), ItemContainerContents.EMPTY).copyInto(slots);
        return slots;
    }

    public static List<ItemStack> getModules(ItemStack weapon) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack module : getModuleSlots(weapon)) {
            if (!module.isEmpty()) list.add(module);
        }
        return list;
    }

    public static ItemStack getModule(ItemStack weapon, int slot) {
        return getModuleSlots(weapon).get(slot);
    }

    public static void setModule(ItemStack weapon, int slot, ItemStack module) {
        NonNullList<ItemStack> slots = getModuleSlots(weapon);
        slots.set(slot, module.copy());
        weapon.set(MODataComponents.WEAPON_MODULES.get(), ItemContainerContents.fromItems(slots));
    }

    /** 1.7.10 supportsModule(slot, weapon): which module slots this weapon has. */
    public boolean supportsSlot(int slot) {
        return true;
    }

    /** 1.7.10 supportsModule(weapon, module): explosion and heal barrels are phaser-only. */
    public boolean supportsModule(ItemStack module) {
        return !(module.getItem() instanceof WeaponBarrelItem barrel && barrel.phaserOnly());
    }

    /** 1.7.10 ModuleSlot.isValidForSlot: a battery (an energy item that isn't a weapon) or a module made for that slot. */
    public boolean canInstall(int slot, ItemStack module) {
        if (!supportsSlot(slot)) return false;
        if (slot == WeaponModule.SLOT_BATTERY) return module.getItem() instanceof BatteryItem;
        return module.getItem() instanceof WeaponModule m && m.getSlot(module) == slot && supportsModule(module);
    }

    public static float modifyStat(WeaponStat stat, ItemStack weapon, float value) {
        for (ItemStack module : getModules(weapon)) {
            if (module.getItem() instanceof WeaponModule m) value = m.modifyStat(stat, module, weapon, value);
        }
        return value;
    }

    public static boolean hasStat(WeaponStat stat, ItemStack weapon) {
        return modifyStat(stat, weapon, 0) > 0;
    }

    /** 1.7.10: x the legendary range multiplier read as an int (floor: 1.15-1.45 -> 1). */
    public int getRange(ItemStack weapon) {
        return Math.round(modifyStat(WeaponStat.RANGE, weapon, defaultRange)) * (int) WeaponFactory.legendary(weapon).range();
    }

    public int getShootCooldown(ItemStack weapon) {
        int cooldown = (int) modifyStat(WeaponStat.FIRE_RATE, weapon, baseCooldown);
        cooldown = (int) (cooldown * WeaponFactory.legendary(weapon).speed());
        return Math.max(1, cooldown);
    }

    /** Energy per tick of cooldown (1.7.10 getEnergyUse: ENERGY_PER_SHOT / cooldown, x AMMO modules). */
    public int getEnergyUse(ItemStack weapon) {
        return Math.max(0, (int) modifyStat(WeaponStat.AMMO, weapon, energyPerShot / (float) getShootCooldown(weapon)));
    }

    public int getEnergyPerShot(ItemStack weapon) {
        return getEnergyUse(weapon) * getShootCooldown(weapon);
    }

    public float getMaxHeat(ItemStack weapon) {
        return modifyStat(WeaponStat.MAX_HEAT, weapon, baseMaxHeat);
    }

    public float getDamage(ItemStack weapon, LivingEntity shooter) {
        float damage = modifyStat(WeaponStat.DAMAGE, weapon, baseDamage) * WeaponFactory.legendary(weapon).damage();
        damage += (float) shooter.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return shooter instanceof WeaponShooter mob ? damage * mob.weaponDamageScale() : damage;
    }

    /** 1.7.10 getAccuracy: base + 10 x movement, x0.6 sneaking, then modules and the scope. */
    public float getAccuracy(ItemStack weapon, LivingEntity shooter, boolean zoomed) {
        Vec3 motion = shooter.getDeltaMovement();
        float accuracy = baseAccuracy(weapon, zoomed) + (float) new Vec3(motion.x, motion.y * 0.1, motion.z).length() * 10;
        if (shooter.isShiftKeyDown()) accuracy *= 0.6f;
        accuracy = modifyStat(WeaponStat.ACCURACY, weapon, accuracy);
        ItemStack sights = getModule(weapon, WeaponModule.SLOT_SIGHTS);
        if (sights.getItem() instanceof WeaponScope scope) accuracy = scope.getAccuracyModify(sights, weapon, zoomed, accuracy);
        accuracy *= WeaponFactory.legendary(weapon).accuracy();
        if (shooter instanceof WeaponShooter mob) accuracy += mob.weaponAccuracyAdd();
        return accuracy;
    }

    protected abstract float baseAccuracy(ItemStack weapon, boolean zoomed);

    public float getShotSpeed() {
        return shotSpeed;
    }

    /** 1.7.10 getZoomMultiply: the scope's zoom when one is installed. */
    public float getZoom(ItemStack weapon) {
        ItemStack sights = getModule(weapon, WeaponModule.SLOT_SIGHTS);
        if (sights.getItem() instanceof WeaponScope scope) return scope.getZoomAmount(sights, weapon);
        return zoom;
    }

    public int getColor(ItemStack weapon) {
        for (ItemStack module : getModules(weapon)) {
            if (module.getItem() instanceof WeaponColorModuleItem color) return color.getColor();
        }
        return 0xFFFFFF;
    }

    // --- energy, heat --------------------------------------------------------------------------

    /** Charges and drains through {@link #getEnergy}/{@link #setEnergy}, so a battery module is used when installed. */
    public IEnergyStorage createEnergyHandler(ItemStack weapon) {
        return new ComponentEnergyStorage(weapon, MODataComponents.ENERGY.get(), getCapacity(weapon), TRANSFER, TRANSFER) {
            @Override
            public int getEnergyStored() {
                return weapon.is(EnergyWeaponItem.this) ? Math.min(getEnergy(weapon), getMaxEnergyStored()) : 0;
            }

            @Override
            protected void setEnergy(int energy) {
                EnergyWeaponItem.setEnergy(weapon, Math.max(0, Math.min(energy, getMaxEnergyStored())));
            }
        };
    }

    /** 1.7.10 EnergyWeapon: with a battery module installed the weapon runs on (and charges) the battery instead. */
    public static int getEnergy(ItemStack weapon) {
        ItemStack battery = getModule(weapon, WeaponModule.SLOT_BATTERY);
        if (battery.getItem() instanceof BatteryItem b) return b.getEnergy(battery);
        return weapon.getOrDefault(MODataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack weapon, int energy) {
        ItemStack battery = getModule(weapon, WeaponModule.SLOT_BATTERY);
        if (battery.getItem() instanceof BatteryItem b) {
            if (b.isCreative()) return;
            battery.set(MODataComponents.ENERGY.get(), Mth.clamp(energy, 0, b.getCapacity()));
            setModule(weapon, WeaponModule.SLOT_BATTERY, battery);
            return;
        }
        weapon.set(MODataComponents.ENERGY.get(), Mth.clamp(energy, 0, CAPACITY));
    }

    public static int getCapacity(ItemStack weapon) {
        ItemStack battery = getModule(weapon, WeaponModule.SLOT_BATTERY);
        return battery.getItem() instanceof BatteryItem b ? b.getCapacity() : CAPACITY;
    }

    public static float getHeat(ItemStack weapon) {
        return weapon.getOrDefault(MODataComponents.HEAT.get(), 0f);
    }

    public static void setHeat(ItemStack weapon, float heat) {
        weapon.set(MODataComponents.HEAT.get(), Math.max(0, heat));
    }

    public static boolean isOverheated(ItemStack weapon) {
        return weapon.getOrDefault(MODataComponents.OVERHEATED.get(), false);
    }

    public boolean canFire(ItemStack weapon) {
        return !isOverheated(weapon) && getEnergy(weapon) >= getEnergyPerShot(weapon);
    }

    /** 1.7.10 manageCooling: heat drops by 4 * easeOutQuart(heat / max) per tick; overheat ends below 2. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!(level instanceof ServerLevel)) return;
        float heat = getHeat(stack);
        if (heat > 0) {
            float t = Math.min(1, heat / getMaxHeat(stack));
            float cooled = heat - 4 * (1 - (float) Math.pow(1 - t, 4));
            setHeat(stack, cooled < 0.001f ? 0 : cooled);
        }
        if (isOverheated(stack) && getHeat(stack) < 2) {
            stack.set(MODataComponents.OVERHEATED.get(), false);
        }
    }

    protected void addHeatAfterShot(ItemStack weapon, ServerLevel level, LivingEntity shooter, float newHeat) {
        setHeat(weapon, newHeat);
        if (newHeat >= getMaxHeat(weapon)) {
            // 1.7.10 MOEventEnergyWeapon.Overheat: an android's flash cooling may cool the weapon at once instead
            if (shooter instanceof Player player && matteroverdrive.android.BioticStats.flashCool(player)) {
                setHeat(weapon, 0);
                level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), MOSounds.OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
                return;
            }
            weapon.set(MODataComponents.OVERHEATED.get(), true);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), MOSounds.OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), MOSounds.OVERHEAT_ALARM.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    // --- firing ----------------------------------------------------------------------------------

    /** Server side, from the fire key: fires if the weapon is off cooldown, cool enough and charged. */
    public void tryFire(ServerPlayer player, ItemStack weapon, boolean zoomed) {
        if (player.getCooldowns().isOnCooldown(weapon.getItem()) || !canFire(weapon)) return;
        setEnergy(weapon, getEnergy(weapon) - getEnergyPerShot(weapon));
        fire((ServerLevel) player.level(), player, weapon, zoomed);
        player.getCooldowns().addCooldown(weapon.getItem(), getShootCooldown(weapon));
    }

    /**
     * 1.7.10 EntityRangedRogueAndroidMob.attackEntityWithRangedAttack: a mob fires along its look (zoomed), the
     * weapon is cooled at once and, with unlimited ammo, kept charged.
     */
    public void fireFromMob(ServerLevel level, LivingEntity mob, ItemStack weapon) {
        fire(level, mob, weapon, true);
        setHeat(weapon, 0);
        setEnergy(weapon, getCapacity(weapon));
    }

    protected abstract void fire(ServerLevel level, LivingEntity shooter, ItemStack weapon, boolean zoomed);

    /** Spawns one bolt along the shooter's look; 1.7.10 spread was gaussian * 0.0075 * accuracy. */
    protected PlasmaBolt spawnBolt(ServerLevel level, LivingEntity shooter, ItemStack weapon, float damage, float accuracy) {
        return spawnBolt(level, shooter, weapon, damage, accuracy, getRange(weapon));
    }

    protected PlasmaBolt spawnBolt(ServerLevel level, LivingEntity shooter, ItemStack weapon, float damage, float accuracy, float range) {
        PlasmaBolt bolt = new PlasmaBolt(level, shooter, damage, range, getColor(weapon));
        bolt.setFireMultiplier(modifyStat(WeaponStat.FIRE_DAMAGE, weapon, 0));
        Vec3 look = shooter.getLookAngle();
        // vanilla shoot() spreads by 0.0172275 * inaccuracy; scale so the spread matches 1.7.10
        bolt.shoot(look.x, look.y, look.z, shotSpeed * 1.5f, accuracy * 0.0075f / 0.0172275f);
        level.addFreshEntity(bolt);
        return bolt;
    }

    protected void playShot(ServerLevel level, LivingEntity shooter, SoundEvent sound) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), sound, SoundSource.PLAYERS, 1, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    // --- use: aim, reload --------------------------------------------------------------------------

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (getEnergy(weapon) < getEnergyPerShot(weapon) && EnergyPackItem.reload(player, weapon)) {
            player.getCooldowns().addCooldown(weapon.getItem(), 40);
            return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
        }
        if (getZoom(weapon) > 0) {
            player.startUsingItem(hand);
            return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** 1.7.10 kept the weapon's own pose while aiming or firing the beam (no bow draw). */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // --- display -----------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getEnergy(stack) / getCapacity(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isOverheated(stack) ? 0xFF3333 : Mth.hsvToRgb(getEnergy(stack) / (float) getCapacity(stack) / 3f, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack weapon, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.energy_stored", MOText.energy(getEnergy(weapon)),
                MOText.energy(getCapacity(weapon))).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.weapon.power_use", MOText.energy(getEnergyUse(weapon) * 20L))
                .withStyle(ChatFormatting.DARK_RED));
        WeaponFactory.Legendary legendary = WeaponFactory.legendary(weapon);
        float damage = modifyStat(WeaponStat.DAMAGE, weapon, baseDamage) * legendary.damage() + 1;
        int cooldown = getShootCooldown(weapon);
        // 1.7.10 addStatWithMultiplyInfo: the change against the base as a green / red percentage
        tooltip.accept(stat("damage", String.format(Locale.ROOT, "%.2f", damage), damage / baseDamage));
        tooltip.accept(stat("dps", String.format(Locale.ROOT, "%.2f", damage / cooldown * 20), 1));
        tooltip.accept(stat("speed", (int) (20d / cooldown * 60), (double) baseCooldown / cooldown));
        tooltip.accept(stat("range", getRange(weapon), (double) getRange(weapon) / defaultRange));
        tooltip.accept(stat("accuracy", "", 1 / (modifyStat(WeaponStat.ACCURACY, weapon, 1) * legendary.accuracy())));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 32 * Math.min(1, getHeat(weapon) / getMaxHeat(weapon)); i++) bar.append('|');
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.weapon.heat", bar.toString()).withStyle(ChatFormatting.DARK_RED));
        for (ItemStack module : getModules(weapon)) {
            tooltip.accept(Component.literal("  ").append(module.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component stat(String key, Object value, double multiply) {
        var line = Component.translatable("tooltip.matteroverdrive.weapon." + key, Component.literal(String.valueOf(value)).withStyle(ChatFormatting.DARK_AQUA));
        if (Math.abs(multiply - 1) > 1e-6) {
            line.append(Component.literal(" (" + java.text.NumberFormat.getPercentInstance().format(multiply) + ")")
                    .withStyle(multiply > 1 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
        }
        return line;
    }
}
