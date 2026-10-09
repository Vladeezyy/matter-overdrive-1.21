package matteroverdrive.compat.render;

import org.joml.Quaternionf;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Stand-in for the 1.21.9+ CameraRenderState, read from the main camera. */
public class CameraRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public Vec3 pos = Vec3.ZERO;
    public boolean initialized;
    public Quaternionf orientation = new Quaternionf();

    public static CameraRenderState current() {
        CameraRenderState state = new CameraRenderState();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        state.initialized = camera.isInitialized();
        state.pos = camera.getPosition();
        state.blockPos = camera.getBlockPosition();
        state.orientation = new Quaternionf(camera.rotation());
        return state;
    }
}
