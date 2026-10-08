package matteroverdrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import matteroverdrive.block.entity.WeaponStationBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7.10 TileEntityRendererWeaponStation: the weapon on the station floats above it, slowly turning
 * (it was rendered as a bobbing item entity at y + 0.8).
 */
public class WeaponStationRenderer implements BlockEntityRenderer<WeaponStationBlockEntity, WeaponStationRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public WeaponStationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
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
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(station, state, partialTick, camera, crumbling);
        itemModelResolver.updateForTopItem(state.item, station.getWeapon(), ItemDisplayContext.GROUND, station.getLevel(), null,
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
        state.item.submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
