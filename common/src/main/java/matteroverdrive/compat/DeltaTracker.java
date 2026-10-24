package matteroverdrive.compat;

/** 1.20.1 stand-in for the 1.21 {@code matteroverdrive.compat.DeltaTracker} the HUD hooks get: the frame's partial tick. */
public record DeltaTracker(float partialTick, float deltaTicks) {
    public float getGameTimeDeltaPartialTick(boolean ignoreFrozen) {
        return partialTick;
    }

    public float getGameTimeDeltaTicks() {
        return deltaTicks;
    }

    /** From the current frame of the client */
    public static DeltaTracker now(float partialTick) {
        return new DeltaTracker(partialTick, net.minecraft.client.Minecraft.getInstance().getDeltaFrameTime());
    }
}
