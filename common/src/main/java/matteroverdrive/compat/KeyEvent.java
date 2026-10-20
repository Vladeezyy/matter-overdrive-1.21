package matteroverdrive.compat;

/** 1.21.1 stand-in for the 1.21.9+ net.minecraft.client.input.KeyEvent (built from keyPressed's arguments). */
public record KeyEvent(int key, int scancode, int modifiers) {
    public boolean hasShiftDown() {
        return (modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0;
    }
}
