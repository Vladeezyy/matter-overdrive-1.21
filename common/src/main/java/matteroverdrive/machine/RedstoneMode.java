package matteroverdrive.machine;

import java.util.Locale;

/** 1.7.10 "redstoneMode" config: work on low signal, on high signal, or ignore redstone. */
public enum RedstoneMode {
    LOW, HIGH, DISABLED;

    public boolean allows(boolean powered) {
        return switch (this) {
            case LOW -> !powered;
            case HIGH -> powered;
            case DISABLED -> true;
        };
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "gui.matteroverdrive.redstone_mode." + name().toLowerCase(Locale.ROOT);
    }
}
