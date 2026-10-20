package matteroverdrive.client;

import net.minecraft.client.Minecraft;

/** 26.2: small client helpers for state that moved (the HUD's hidden flag is a toggle on Minecraft.gui.hud). */
public final class MOClientCompat {
    private MOClientCompat() {}

    /** Hides or shows the HUD (F1) */
    public static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }
}
