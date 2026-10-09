package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.block.entity.WeaponStationBlockEntity;
import matteroverdrive.compat.render.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import matteroverdrive.compat.render.BlockEntityRenderState;
import matteroverdrive.compat.render.ItemModelResolver;
import matteroverdrive.compat.render.ItemStackRenderState;
import matteroverdrive.compat.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 TileEntityRendererWeaponStation: the weapon on the station floats above it, slowly turning
 * (it was rendered as a bobbing item entity at y + 0.8).
 */
public class WeaponStationRenderer implements matteroverdrive.compat.render.StateBlockEntityRenderer<WeaponStationBlockEntity, WeaponStationRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public WeaponStationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
    }

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WeaponStationBlockEntity station, State state, float partialTick, Vec3 camera,
                                   @Nullable Object crumbling) {
        matteroverdrive.compat.render.StateBlockEntityRenderer.super.extractRenderState(station, state, partialTick, camera, crumbling);
        itemModelResolver.updateForTopItem(state.item, station.getWeapon(), ItemDisplayContext.GROUND, false, station.getLevel(), null,
                (int) station.getBlockPos().asLong());
        // 1.7.10 hoverStart = worldTime * 0.05 + noise: the item entity renderer turned it by age + hoverStart
        state.time = station.getLevel() == null ? 0 : station.getLevel().getGameTime() + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5f, 0.8f + 0.1f * (float) Math.sin(state.time / 10), 0.5f);
        pose.mulPose(Axis.YP.rotation(state.time / 20));
        state.item.render(pose, collector.buffers(), 0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
