package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.block.HoloSignBlock;
import matteroverdrive.block.entity.HoloSignBlockEntity;
import net.minecraft.client.gui.Font;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import matteroverdrive.compat.render.BlockEntityRenderState;
import matteroverdrive.compat.render.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 TileEntityRendererHoloSign: the lines drawn in holo blue on the panel's face, all at the size that fits the
 * longest line (1.7.10 drawScreenInfoWithGlobalAutoSize, at most 4x), full bright.
 */
public class HoloSignRenderer implements matteroverdrive.compat.render.StateBlockEntityRenderer<HoloSignBlockEntity, HoloSignRenderer.State> {
    private static final int COLOR = 0xFFA9E2FB;
    private final Font font;

    public HoloSignRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    public static class State extends BlockEntityRenderState {
        String[] lines = new String[0];
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HoloSignBlockEntity sign, State state, float partialTick, Vec3 camera,
                                   @Nullable Object crumbling) {
        matteroverdrive.compat.render.StateBlockEntityRenderer.super.extractRenderState(sign, state, partialTick, camera, crumbling);
        state.lines = sign.getText().isEmpty() ? new String[0] : sign.getText().split("\n");
        state.facing = sign.getBlockState().getValue(HoloSignBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.lines.length == 0) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // local +z = the sign's facing; the 2 px panel sits at the back, its front face 6 px behind the centre
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        pose.translate(0, 0, -(0.5 - 2 / 16f) + 0.002);
        int widest = 1;
        for (String line : state.lines) widest = Math.max(widest, font.width(line));
        float usable = 1 - 2 * 10 / 128f;     // 1.7.10 margins of 10 px on a 128 px screen
        float scale = Math.min(usable / widest, Math.min(4f / 128, usable / (state.lines.length * font.lineHeight)));
        pose.scale(scale, -scale, scale);
        float top = -state.lines.length * font.lineHeight / 2f;
        for (int i = 0; i < state.lines.length; i++) {
            var text = Component.literal(state.lines[i]).getVisualOrderText();
            collector.submitText(pose, -font.width(text) / 2f, top + i * font.lineHeight, text, false, Font.DisplayMode.POLYGON_OFFSET,
                    0xF000F0, COLOR, 0, 0);
        }
        pose.popPose();
    }
}
