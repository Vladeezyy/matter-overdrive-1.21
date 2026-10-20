package matteroverdrive.item.weapon;

import java.util.function.Consumer;

import matteroverdrive.init.MOSounds;
import matteroverdrive.util.MOText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7.10 EnergyPack: 32000 FE of ammunition; a weapon reloads one from the inventory. */
public class EnergyPackItem extends Item {
    public static final int ENERGY = 32000;

    public EnergyPackItem(Properties properties) {
        super(properties);
    }

    /** 1.7.10 chargeFromEnergyPack: uses the first pack in the main inventory. */
    public static boolean reload(Player player, ItemStack weapon) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack pack = inv.getItem(i);
            if (pack.getItem() instanceof EnergyPackItem) {
                if (!player.level().isClientSide()) {
                    pack.shrink(1);
                    EnergyWeaponItem.setEnergy(weapon, EnergyWeaponItem.getEnergy(weapon) + ENERGY);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MOSounds.RELOAD.get(), SoundSource.PLAYERS,
                            0.7f + player.getRandom().nextFloat() * 0.2f, 0.9f + player.getRandom().nextFloat() * 0.2f);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(MOText.energy(ENERGY).copy().withStyle(ChatFormatting.YELLOW));
    }
}
