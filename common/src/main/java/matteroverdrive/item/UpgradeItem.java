package matteroverdrive.item;

import java.util.Map;
import java.util.function.Consumer;

import matteroverdrive.machine.UpgradeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** 1.7.10 ItemUpgrade: one damage value per upgrade there, one item per upgrade here. */
public class UpgradeItem extends Item {
    private final Map<UpgradeType, Double> upgrades;

    public UpgradeItem(Map<UpgradeType, Double> upgrades, Properties properties) {
        super(properties.stacksTo(16));
        this.upgrades = Map.copyOf(upgrades);
    }

    public Map<UpgradeType, Double> getUpgrades() {
        return upgrades;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        upgrades.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> tooltip.accept(describe(e.getKey(), e.getValue())));
    }

    /** "+25% Speed" in green when it helps, red when it hurts. */
    public static Component describe(UpgradeType type, double multiplier) {
        int percent = (int) Math.round((multiplier - 1) * 100);
        boolean good = type.lowerIsBetter() ? multiplier < 1 : multiplier > 1;
        String sign = percent > 0 ? "+" : "";
        return Component.literal(sign + percent + "% ").append(Component.translatable(type.translationKey()))
                .withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
