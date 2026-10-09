package matteroverdrive.compat.render;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.21.1 stand-in for the 1.21.4+ ItemStackRenderState: remembers the stack and draws it with the ItemRenderer. */
public class ItemStackRenderState {
    ItemStack stack = ItemStack.EMPTY;
    ItemDisplayContext context = ItemDisplayContext.NONE;
    @Nullable Level level;
    int seed;

    public void clear() {
        stack = ItemStack.EMPTY;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        if (stack.isEmpty()) return;
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, context, light, overlay, pose, collector.buffers(), level, seed);
    }
}
