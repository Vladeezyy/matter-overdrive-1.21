package matteroverdrive.item.android;

import java.util.function.Consumer;

import matteroverdrive.android.Android;
import matteroverdrive.android.AndroidData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 AndroidPill: red turns a human into an android, blue turns an android back, yellow resets an android's
 * stats for half their XP. Always edible, no food value.
 */
public class AndroidPillItem extends Item {
    public enum Type { RED, BLUE, YELLOW }

    private final Type type;

    public AndroidPillItem(Type type, Properties properties) {
        super(properties.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build()));
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        AndroidData data = Android.get(player);
        boolean allowed = type == Type.RED ? !data.isAndroid() && !data.isTurning() : data.isAndroid() && !data.isTurning();
        return allowed ? super.use(level, player, hand) : InteractionResult.FAIL;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            switch (type) {
                case RED -> Android.startTransformation(player);
                case BLUE -> Android.setAndroid(player, false);
                case YELLOW -> {
                    if (!Android.get(player).isTurning() && Android.isAndroid(player)) {
                        player.giveExperienceLevels(Android.resetStats(player));
                    }
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".details").withStyle(ChatFormatting.GRAY));
    }
}
