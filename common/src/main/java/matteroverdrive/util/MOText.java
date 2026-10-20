package matteroverdrive.util;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public final class MOText {
    /** "512k FE" style energy formatting, as 1.7.10 MOEnergyHelper did with RF. */
    public static Component energy(long amount) {
        return Component.literal(compact(amount) + " FE");
    }

    public static String compact(long amount) {
        if (amount >= 1_000_000) return trim(amount / 1_000_000d) + "M";
        if (amount >= 1_000) return trim(amount / 1_000d) + "k";
        return Long.toString(amount);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private MOText() {}
}
