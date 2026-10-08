package matteroverdrive.item.android;

import java.util.function.Consumer;

import matteroverdrive.MatterOverdrive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * 1.7.10 RougeAndroidParts: a rogue android's head, arm, leg or torso. Installed in the matching android slot it adds
 * one point of max health.
 */
public class BionicPartItem extends Item {
    private final int slot;
    private final double health;
    private final double glitch;

    public BionicPartItem(int slot, Properties properties) {
        this(slot, 1, 0, properties);
    }

    /** @param glitch glitch-time change (multiplied in; 1.7.10 TritaniumSpine -0.5) */
    public BionicPartItem(int slot, double health, double glitch, Properties properties) {
        super(properties.stacksTo(1));
        this.slot = slot;
        this.health = health;
        this.glitch = glitch;
    }

    /** 1.7.10 BionicPart.loadCustomAttributes: a part's own bonuses replace the defaults (the Hardened Tritanium Spine). */
    public record Stats(double health, double glitch) {
        public static final com.mojang.serialization.Codec<Stats> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.DOUBLE.fieldOf("health").forGetter(Stats::health),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("glitch").forGetter(Stats::glitch)).apply(i, Stats::new));
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Stats> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.DOUBLE, Stats::health,
                        net.minecraft.network.codec.ByteBufCodecs.DOUBLE, Stats::glitch, Stats::new);
    }

    /**
     * 1.7.10 legendary part attributes, each 0..level: max health +n, attack damage +n, knockback resistance +0.1n,
     * movement speed +10% n, glitch time -20% n, battery use -3% n (operation 1 = multiply base).
     */
    public record Legendary(double health, double attack, double knockback, double speed, double glitch, double battery) {
        public static final com.mojang.serialization.Codec<Legendary> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.DOUBLE.fieldOf("health").forGetter(Legendary::health),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("attack").forGetter(Legendary::attack),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("knockback").forGetter(Legendary::knockback),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("speed").forGetter(Legendary::speed),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("glitch").forGetter(Legendary::glitch),
                com.mojang.serialization.Codec.DOUBLE.fieldOf("battery").forGetter(Legendary::battery)).apply(i, Legendary::new));

        public boolean isEmpty() {
            return health == 0 && attack == 0 && knockback == 0 && speed == 0 && glitch == 0 && battery == 0;
        }

        /** 1.7.10 addLegendaryAttributesToPart: six rolls of 0..level, each in this order. */
        public static Legendary roll(net.minecraft.util.RandomSource random, int level) {
            return new Legendary(random.nextInt(level + 1), random.nextInt(level + 1), random.nextInt(level + 1) * 0.1,
                    random.nextInt(level + 1) * 0.1, -random.nextInt(level + 1) * 0.2, -random.nextInt(level + 1) * 0.03);
        }
    }

    /** An attribute change of the part; suffix "" for max health and glitch keeps the original modifier ids. */
    public record Mod(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount,
                      net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation, String suffix) {}

    public static final String[] MOD_SUFFIXES = {"", "glitch", "attack", "knockback", "speed", "battery"};

    /** 1.7.10 getModifiers: a part's own attributes (legendary, or the Hardened Spine's) replace its defaults. */
    public java.util.List<Mod> modifiers(ItemStack stack) {
        var ADD = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        var BASE = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        java.util.List<Mod> mods = new java.util.ArrayList<>();
        Legendary legendary = stack.get(matteroverdrive.init.MODataComponents.LEGENDARY_PART.get());
        if (legendary != null && !legendary.isEmpty()) {
            if (legendary.health() != 0) mods.add(new Mod(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, legendary.health(), ADD, ""));
            if (legendary.attack() != 0) mods.add(new Mod(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, legendary.attack(), ADD, "attack"));
            if (legendary.knockback() != 0) {
                mods.add(new Mod(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, legendary.knockback(), ADD, "knockback"));
            }
            if (legendary.speed() != 0) mods.add(new Mod(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, legendary.speed(), BASE, "speed"));
            if (legendary.glitch() != 0) mods.add(new Mod(matteroverdrive.init.MOAttributes.GLITCH_TIME, legendary.glitch(), BASE, "glitch"));
            if (legendary.battery() != 0) mods.add(new Mod(matteroverdrive.init.MOAttributes.BATTERY_USE, legendary.battery(), BASE, "battery"));
            return mods;
        }
        mods.add(new Mod(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, maxHealthBonus(stack), ADD, ""));
        if (glitchBonus(stack) != 0) {
            mods.add(new Mod(matteroverdrive.init.MOAttributes.GLITCH_TIME, glitchBonus(stack),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, ""));
        }
        return mods;
    }

    public static ResourceLocation modifierId(int slot, String suffix) {
        return suffix.isEmpty() ? modifierId(slot) : ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "bionic_part_" + slot + "_" + suffix);
    }

    public double glitchBonus(ItemStack stack) {
        Stats custom = stack.get(matteroverdrive.init.MODataComponents.BIONIC_STATS.get());
        return custom != null ? custom.glitch() : glitch;
    }

    /** The android slot it fits: {@link matteroverdrive.android.AndroidData#SLOT_HEAD} etc. */
    public int getSlot() {
        return slot;
    }

    /** 1.7.10 getModifiers: max health +1 (one modifier per slot so parts stack). */
    public double maxHealthBonus(ItemStack stack) {
        Stats custom = stack.get(matteroverdrive.init.MODataComponents.BIONIC_STATS.get());
        return custom != null ? custom.health() : health;
    }

    public static ResourceLocation modifierId(int slot) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "bionic_part_" + slot);
    }

    /** 1.7.10 BionicPart.addDetails: "+n" for additions, "+10%" for operation 1, "50%" (amount + 1) for operation 2. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (slot < 4) tooltip.accept(Component.translatable("item." + MatterOverdrive.MODID + ".rogue_android_part.melee").withStyle(ChatFormatting.GOLD));
        for (Mod mod : modifiers(stack)) {
            String value = switch (mod.operation()) {
                case ADD_VALUE -> "+" + (mod.amount() == Math.rint(mod.amount()) ? String.valueOf((int) mod.amount()) : String.valueOf(mod.amount()));
                case ADD_MULTIPLIED_BASE -> (mod.amount() >= 0 ? "+" : "") + java.text.NumberFormat.getPercentInstance().format(mod.amount());
                default -> java.text.NumberFormat.getPercentInstance().format(mod.amount() + 1);
            };
            tooltip.accept(Component.translatable(mod.attribute().value().getDescriptionId()).append(": " + value).withStyle(ChatFormatting.GREEN));
        }
    }
}
