package matteroverdrive.item.weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import matteroverdrive.entity.PlasmaBolt;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOSounds;
import matteroverdrive.util.MOText;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

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

    public static List<ItemStack> getModules(ItemStack weapon) {
        ItemContainerContents contents = weapon.getOrDefault(MODataComponents.WEAPON_MODULES.get(), ItemContainerContents.EMPTY);
        List<ItemStack> list = new ArrayList<>();
        contents.stream().forEach(list::add);
        return list;
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

    public int getRange(ItemStack weapon) {
        return Math.round(modifyStat(WeaponStat.RANGE, weapon, defaultRange));
    }

    public int getShootCooldown(ItemStack weapon) {
        return Math.max(1, (int) modifyStat(WeaponStat.FIRE_RATE, weapon, baseCooldown));
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
        float damage = modifyStat(WeaponStat.DAMAGE, weapon, baseDamage);
        return damage + (float) shooter.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public float getAccuracy(ItemStack weapon, boolean zoomed) {
        return baseAccuracy(weapon, zoomed) * modifyStat(WeaponStat.ACCURACY, weapon, 1);
    }

    protected abstract float baseAccuracy(ItemStack weapon, boolean zoomed);

    public float getShotSpeed() {
        return shotSpeed;
    }

    public float getZoom() {
        return zoom;
    }

    public int getColor(ItemStack weapon) {
        for (ItemStack module : getModules(weapon)) {
            if (module.getItem() instanceof WeaponColorModuleItem color) return color.getColor();
        }
        return 0xFFFFFF;
    }

    // --- energy, heat --------------------------------------------------------------------------

    public EnergyHandler createEnergyHandler(ItemAccess access) {
        return new ItemAccessEnergyHandler(access, MODataComponents.ENERGY.get(), CAPACITY, TRANSFER, TRANSFER);
    }

    public static int getEnergy(ItemStack weapon) {
        return weapon.getOrDefault(MODataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack weapon, int energy) {
        weapon.set(MODataComponents.ENERGY.get(), Mth.clamp(energy, 0, CAPACITY));
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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
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
            weapon.set(MODataComponents.OVERHEATED.get(), true);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), MOSounds.OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), MOSounds.OVERHEAT_ALARM.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    // --- firing ----------------------------------------------------------------------------------

    /** Server side, from the fire key: fires if the weapon is off cooldown, cool enough and charged. */
    public void tryFire(ServerPlayer player, ItemStack weapon, boolean zoomed) {
        if (player.getCooldowns().isOnCooldown(weapon) || !canFire(weapon)) return;
        setEnergy(weapon, getEnergy(weapon) - getEnergyPerShot(weapon));
        fire((ServerLevel) player.level(), player, weapon, zoomed);
        player.getCooldowns().addCooldown(weapon, getShootCooldown(weapon));
    }

    protected abstract void fire(ServerLevel level, Player shooter, ItemStack weapon, boolean zoomed);

    /** Spawns one bolt along the shooter's look; 1.7.10 spread was gaussian * 0.0075 * accuracy. */
    protected PlasmaBolt spawnBolt(ServerLevel level, Player shooter, ItemStack weapon, float damage, float accuracy) {
        PlasmaBolt bolt = new PlasmaBolt(level, shooter, damage, getRange(weapon), getColor(weapon));
        bolt.setFireMultiplier(modifyStat(WeaponStat.FIRE_DAMAGE, weapon, 0));
        Vec3 look = shooter.getLookAngle();
        // vanilla shoot() spreads by 0.0172275 * inaccuracy; scale so the spread matches 1.7.10
        bolt.shoot(look.x, look.y, look.z, shotSpeed * 1.5f, accuracy * 0.0075f / 0.0172275f);
        level.addFreshEntity(bolt);
        return bolt;
    }

    protected void playShot(ServerLevel level, Player shooter, SoundEvent sound) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), sound, SoundSource.PLAYERS, 1, 0.9f + level.getRandom().nextFloat() * 0.2f);
    }

    // --- use: aim, reload --------------------------------------------------------------------------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (getEnergy(weapon) < getEnergyPerShot(weapon) && EnergyPackItem.reload(player, weapon)) {
            player.getCooldowns().addCooldown(weapon, 40);
            return InteractionResult.SUCCESS;
        }
        if (zoom > 0) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
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
        return Math.round(13f * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isOverheated(stack) ? 0xFF3333 : Mth.hsvToRgb(getEnergy(stack) / (float) CAPACITY / 3f, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack weapon, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.energy_stored", MOText.energy(getEnergy(weapon)),
                MOText.energy(CAPACITY)).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.weapon.power_use", MOText.energy(getEnergyUse(weapon) * 20L))
                .withStyle(ChatFormatting.DARK_RED));
        float damage = modifyStat(WeaponStat.DAMAGE, weapon, baseDamage) + 1;
        int cooldown = getShootCooldown(weapon);
        tooltip.accept(stat("damage", String.format(Locale.ROOT, "%.2f", damage)));
        tooltip.accept(stat("dps", String.format(Locale.ROOT, "%.2f", damage / cooldown * 20)));
        tooltip.accept(stat("speed", (int) (20d / cooldown * 60)));
        tooltip.accept(stat("range", getRange(weapon)));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 32 * Math.min(1, getHeat(weapon) / getMaxHeat(weapon)); i++) bar.append('|');
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.weapon.heat", bar.toString()).withStyle(ChatFormatting.DARK_RED));
        for (ItemStack module : getModules(weapon)) {
            tooltip.accept(Component.literal("  ").append(module.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component stat(String key, Object value) {
        return Component.translatable("tooltip.matteroverdrive.weapon." + key, Component.literal(String.valueOf(value)).withStyle(ChatFormatting.DARK_AQUA));
    }
}
