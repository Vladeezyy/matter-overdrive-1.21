package matteroverdrive.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.matter.ItemPattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

/**
 * 1.7.10 PatternDrive: stores up to {@code capacity} item patterns (2 for the pattern drive). The icon shows
 * empty / partially full / full through custom model data, which the client item definition dispatches on.
 */
public class PatternDriveItem extends Item {
    private final int capacity;

    public PatternDriveItem(int capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static List<ItemPattern> getPatterns(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.PATTERNS.get(), List.of());
    }

    /** Whether analysing one more {@code item} can add progress here (an incomplete pattern or a free slot). */
    public boolean canAccept(ItemStack drive, Item item) {
        List<ItemPattern> patterns = getPatterns(drive);
        for (ItemPattern p : patterns) {
            if (p.is(item)) return !p.isComplete();
        }
        return patterns.size() < capacity;
    }

    /** Adds analysis progress for {@code item}; returns false if the drive can't take it. */
    public boolean addProgress(ItemStack drive, Item item, int amount) {
        if (!canAccept(drive, item)) return false;
        List<ItemPattern> patterns = new ArrayList<>(getPatterns(drive));
        boolean found = false;
        for (int i = 0; i < patterns.size(); i++) {
            if (patterns.get(i).is(item)) {
                patterns.set(i, patterns.get(i).withProgress(amount));
                found = true;
            }
        }
        if (!found) {
            patterns.add(new ItemPattern(item.builtInRegistryHolder(), Math.min(ItemPattern.MAX_PROGRESS, amount)));
        }
        setPatterns(drive, patterns);
        return true;
    }

    public void setPatterns(ItemStack drive, List<ItemPattern> patterns) {
        drive.set(MODataComponents.PATTERNS.get(), List.copyOf(patterns));
        float state = patterns.isEmpty() ? 0 : patterns.size() < capacity ? 1 : 2;
        drive.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(state), List.of(), List.of(), List.of()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        for (ItemPattern p : getPatterns(stack)) {
            tooltip.accept(Component.empty().append(p.toStack().getHoverName()).append(" " + p.progress() + "%")
                    .withStyle(p.isComplete() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        tooltip.accept(Component.translatable("item.matteroverdrive.pattern_drive.details", capacity).withStyle(ChatFormatting.GRAY));
    }
}
