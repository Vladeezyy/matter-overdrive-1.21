package matteroverdrive.item;

import java.util.function.Consumer;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.util.MOText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.energy.IEnergyStorage;
import matteroverdrive.compat.InfiniteEnergyHandler;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;

/**
 * 1.7.10 Battery / CreativeBattery: an FE container item. The overlay layer is tinted per battery in its client item JSON.
 */
public class BatteryItem extends Item {
    private final int capacity;
    private final int maxInsert;
    private final int maxExtract;
    private final boolean creative;

    public BatteryItem(int capacity, int maxInsert, int maxExtract, boolean creative, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.creative = creative;
    }

    public IEnergyStorage createEnergyHandler(ItemStack stack) {
        if (creative) {
            return InfiniteEnergyHandler.INSTANCE;
        }
        return new ComponentEnergyStorage(stack, MODataComponents.ENERGY.get(), capacity, maxInsert, maxExtract);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getEnergy(ItemStack stack) {
        return creative ? capacity : stack.getOrDefault(MODataComponents.ENERGY.get(), 0);
    }

    /** A fully charged stack, as listed in the creative tab next to the empty one. */
    public ItemStack charged() {
        ItemStack stack = new ItemStack(this);
        stack.set(MODataComponents.ENERGY.get(), capacity);
        return stack;
    }

    public boolean isCreative() {
        return creative;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !creative;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getEnergy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0f, (float) getEnergy(stack) / capacity) / 3f, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.energy_stored",
                creative ? Component.literal("∞") : MOText.energy(getEnergy(stack)), MOText.energy(capacity)).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.energy_io", maxInsert, maxExtract).withStyle(ChatFormatting.GRAY));
    }
}
