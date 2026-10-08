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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (slot < 4) tooltip.accept(Component.translatable("item." + MatterOverdrive.MODID + ".rogue_android_part.melee").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("attribute.name.max_health").append(": +" + (int) maxHealthBonus(stack)).withStyle(ChatFormatting.GREEN));
        if (glitchBonus(stack) != 0) {
            tooltip.accept(Component.translatable("attribute.name." + MatterOverdrive.MODID + ".android_glitch_time")
                    .append(": " + Math.round((1 + glitchBonus(stack)) * 100) + "%").withStyle(ChatFormatting.GREEN));
        }
    }
}
