package matteroverdrive.compat;

/** 1.21.1 stand-in for the 1.21.9+ net.minecraft.client.input.MouseButtonEvent (built from mouseClicked's arguments). */
public record MouseButtonEvent(double x, double y, int button) {
    public boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }
}
