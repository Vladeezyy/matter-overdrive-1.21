package matteroverdrive.compat.render;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Stand-in for the 1.21.4+ ItemStackRenderState (before 1.21.4): remembers the stack and draws it with the ItemRenderer. */
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

    /** The 1.21.4 call. */
    public void render(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        if (stack.isEmpty()) return;
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, context, light, overlay, pose, buffers, level, seed);
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        if (stack.isEmpty()) return;
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, context, light, overlay, pose, collector.buffers(), level, seed);
    }
}
