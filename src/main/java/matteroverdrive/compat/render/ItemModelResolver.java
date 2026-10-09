package matteroverdrive.compat.render;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Stand-in for the 1.21.4+ ItemModelResolver (before 1.21.4). */
public final class ItemModelResolver {
    public static final ItemModelResolver INSTANCE = new ItemModelResolver();

    private ItemModelResolver() {}

    public void updateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, boolean leftHand, @Nullable Level level,
                                 @Nullable LivingEntity entity, int seed) {
        state.stack = stack;
        state.context = context;
        state.level = level;
        state.seed = seed;
    }
}
