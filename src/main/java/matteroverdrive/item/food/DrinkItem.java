package matteroverdrive.item.food;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 1.21.1: a drinkable food (the 1.21.10 build uses the DEFAULT_DRINK consumable). {@code clearsEffects} is the
 * 1.7.10 Earl Gray tea, which clears potion effects like milk.
 */
public class DrinkItem extends Item {
    private final boolean clearsEffects;

    public DrinkItem(boolean clearsEffects, Properties properties) {
        super(properties);
        this.clearsEffects = clearsEffects;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (clearsEffects && !level.isClientSide()) entity.removeAllEffects();
        return super.finishUsingItem(stack, level, entity);
    }
}
