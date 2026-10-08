package matteroverdrive.machine;

import matteroverdrive.init.MODataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 1.7.10 MOMachineBlockItem: a machine carried with its data is "[Configured]" and shows its stored energy as the
 * durability bar. The Shift tooltip (details, energy, matter) is client side: {@code client/MachineTooltip}.
 */
public class MachineBlockItem extends BlockItem {
    public MachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 1.7.10 hasTagCompound: anything carried with the machine (energy, matter, owner, destinations, a name). */
    public static boolean isConfigured(ItemStack stack) {
        return !stack.getComponentsPatch().isEmpty();
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        if (!isConfigured(stack)) return name;
        return name.copy().append(Component.literal(" [").append(Component.translatable("item.matteroverdrive.info.configured"))
                .append("]").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        MachineStorage storage = stack.get(MODataComponents.MACHINE_STORAGE.get());
        return stack.has(MODataComponents.ENERGY.get()) && storage != null && storage.maxEnergy() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13f * energyFraction(stack)), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(energyFraction(stack) / 3f, 1, 1);
    }

    private static float energyFraction(ItemStack stack) {
        MachineStorage storage = stack.get(MODataComponents.MACHINE_STORAGE.get());
        if (storage == null || storage.maxEnergy() <= 0) return 0;
        return Mth.clamp(stack.getOrDefault(MODataComponents.ENERGY.get(), 0) / (float) storage.maxEnergy(), 0, 1);
    }
}
