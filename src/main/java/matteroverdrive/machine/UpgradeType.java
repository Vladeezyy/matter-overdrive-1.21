package matteroverdrive.machine;

import java.util.Locale;

/** What a machine upgrade changes. Values are multipliers; names and meanings follow 1.7.10 UpgradeTypes. */
public enum UpgradeType {
    /** Time a job takes (lower is faster). */
    SPEED,
    /** Energy used per job. */
    POWER_USAGE,
    OUTPUT,
    SECOND_OUTPUT,
    /** Chance a job fails. */
    FAIL,
    RANGE,
    POWER_STORAGE,
    POWER_TRANSFER,
    MATTER_STORAGE,
    MATTER_TRANSFER;

    /** Lower multipliers are improvements for these. */
    public boolean lowerIsBetter() {
        return this == SPEED || this == POWER_USAGE || this == FAIL;
    }

    public String translationKey() {
        return "upgrade_type.matteroverdrive." + name().toLowerCase(Locale.ROOT);
    }
}
