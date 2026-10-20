package matteroverdrive.compat;

import net.minecraft.util.Mth;

/** 1.21.1 stand-in for 1.21.2+ net.minecraft.util.ARGB (the methods the mod uses). */
public final class ARGB {
    private ARGB() {}

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    public static int color(int red, int green, int blue) {
        return color(255, red, green, blue);
    }

    /** Replaces the alpha of an RGB colour. */
    public static int color(int alpha, int rgb) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    public static int scaleRGB(int color, float scale) {
        return color(alpha(color), channel(red(color) * scale), channel(green(color) * scale), channel(blue(color) * scale));
    }

    private static int channel(float value) {
        return Mth.clamp((int) value, 0, 255);
    }

    public static int lerp(float delta, int from, int to) {
        return color(Mth.lerpInt(delta, alpha(from), alpha(to)), Mth.lerpInt(delta, red(from), red(to)),
                Mth.lerpInt(delta, green(from), green(to)), Mth.lerpInt(delta, blue(from), blue(to)));
    }
}
