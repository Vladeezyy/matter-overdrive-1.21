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

    public BionicPartItem(int slot, Properties properties) {
        super(properties.stacksTo(1));
        this.slot = slot;
    }

    /** The android slot it fits: {@link matteroverdrive.android.AndroidData#SLOT_HEAD} etc. */
    public int getSlot() {
        return slot;
    }

    /** 1.7.10 getModifiers: max health +1 (one modifier per slot so parts stack). */
    public double maxHealthBonus() {
        return 1;
    }

    public static ResourceLocation modifierId(int slot) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "bionic_part_" + slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item." + MatterOverdrive.MODID + ".rogue_android_part.melee").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("attribute.name.generic.max_health").append(": +" + (int) maxHealthBonus()).withStyle(ChatFormatting.GREEN));
    }
}
