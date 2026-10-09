package matteroverdrive.item.food;

import matteroverdrive.android.Android;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.7.10 RomulanAle: food 4 / 0.6, always drinkable; humans get nausea VIII for 8 s. */
public class RomulanAleItem extends Item {
    public RomulanAleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && !(entity instanceof Player player && Android.isAndroid(player))) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 8));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
