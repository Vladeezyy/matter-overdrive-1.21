package matteroverdrive.neoforge;

import matteroverdrive.fluid.MatterPlasmaFluid;
import net.neoforged.neoforge.fluids.FluidType;

/** Matter Plasma with its NeoForge fluid type (light, viscosity, sounds, the name in tanks) */
final class NeoForgeMatterPlasma {
    static final class Source extends MatterPlasmaFluid.Source {
        @Override
        public FluidType getFluidType() {
            return NeoForgePlatform.MATTER_PLASMA_TYPE.get();
        }
    }

    static final class Flowing extends MatterPlasmaFluid.Flowing {
        @Override
        public FluidType getFluidType() {
            return NeoForgePlatform.MATTER_PLASMA_TYPE.get();
        }
    }

    private NeoForgeMatterPlasma() {}
}
