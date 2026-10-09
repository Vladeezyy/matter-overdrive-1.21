package matteroverdrive.compat.render;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.21.1 stand-in for the 1.21.4+ ItemModelResolver. */
public final class ItemModelResolver {
    public static final ItemModelResolver INSTANCE = new ItemModelResolver();

    private ItemModelResolver() {}

    public void updateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, @Nullable Level level,
                                 @Nullable LivingEntity entity, int seed) {
        state.stack = stack;
        state.context = context;
        state.level = level;
        state.seed = seed;
    }
}
