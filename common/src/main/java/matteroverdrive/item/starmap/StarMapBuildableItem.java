package matteroverdrive.item.starmap;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.starmap.Buildable;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.Planet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1.7.10 ItemBuildableAbstract (+ ItemBuildingAbstract / ItemShipAbstract): stacks of 1, build start and owner on the stack. */
public abstract class StarMapBuildableItem extends Item implements Buildable {
    protected StarMapBuildableItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    protected abstract int getBuildLengthUnscaled(ItemStack stack, Planet planet);

    @Override
    public int getBuildLength(ItemStack stack, Planet planet) {
        return Mth.ceil(getBuildLengthUnscaled(stack, planet) * Galaxy.GALAXY_BUILD_TIME_MULTIPLY);
    }

    @Override
    public long getBuildStart(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.BUILD_START.get(), 0L);
    }

    @Override
    public void setBuildStart(ItemStack stack, long buildStart) {
        stack.set(MODataComponents.BUILD_START.get(), buildStart);
    }

    /** 1.7.10: only a stack that has a start time (any NBT) can finish. */
    @Override
    public boolean isReadyToBuild(Level level, ItemStack stack, Planet planet) {
        return stack.has(MODataComponents.BUILD_START.get()) && getBuildStart(stack) + getBuildLength(stack, planet) < level.getGameTime();
    }

    @Override
    public @Nullable UUID getOwnerID(ItemStack stack) {
        return stack.get(MODataComponents.SECURITY_OWNER.get());
    }

    @Override
    public void setOwner(ItemStack stack, UUID owner) {
        stack.set(MODataComponents.SECURITY_OWNER.get(), owner);
    }

    /** 1.7.10 MOBaseItem.addDetails: the ".details" text, "/n" separated. */
    protected String details() {
        return Component.translatable(getDescriptionId() + ".details").getString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        String key = getDescriptionId() + ".details";
        if (!net.minecraft.locale.Language.getInstance().has(key)) return;
        for (String line : details().split("/n")) tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
}
