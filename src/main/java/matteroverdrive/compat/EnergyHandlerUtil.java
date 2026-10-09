package matteroverdrive.compat;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** 1.21.1 stand-in for NeoForge 21.10's EnergyHandlerUtil.move. */
public final class EnergyHandlerUtil {
    private EnergyHandlerUtil() {}

    /** Moves up to {@code amount} FE from one storage to the other; returns what was moved. */
    public static int move(@Nullable IEnergyStorage from, @Nullable IEnergyStorage to, int amount) {
        if (from == null || to == null || amount <= 0) return 0;
        int available = from.extractEnergy(amount, true);
        if (available <= 0) return 0;
        int accepted = to.receiveEnergy(available, true);
        if (accepted <= 0) return 0;
        int extracted = from.extractEnergy(accepted, false);
        return to.receiveEnergy(extracted, false);
    }
}
