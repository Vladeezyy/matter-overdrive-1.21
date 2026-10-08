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

    public double glitchBonus() {
        return glitch;
    }

    /** The android slot it fits: {@link matteroverdrive.android.AndroidData#SLOT_HEAD} etc. */
    public int getSlot() {
        return slot;
    }

    /** 1.7.10 getModifiers: max health +1 (one modifier per slot so parts stack). */
    public double maxHealthBonus() {
        return health;
    }

    public static ResourceLocation modifierId(int slot) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "bionic_part_" + slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (slot < 4) tooltip.accept(Component.translatable("item." + MatterOverdrive.MODID + ".rogue_android_part.melee").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("attribute.name.max_health").append(": +" + (int) maxHealthBonus()).withStyle(ChatFormatting.GREEN));
        if (glitch != 0) {
            tooltip.accept(Component.translatable("attribute.name." + MatterOverdrive.MODID + ".android_glitch_time")
                    .append(": " + Math.round((1 + glitch) * 100) + "%").withStyle(ChatFormatting.GREEN));
        }
    }
}
