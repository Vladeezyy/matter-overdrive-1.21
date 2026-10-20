package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.fluid.MatterPlasmaFluid;
import matteroverdrive.platform.Platform;
import matteroverdrive.platform.Registrar;
import matteroverdrive.platform.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;

/**
 * Matter Plasma: 1.7.10 stored and moved matter as this fluid, 1 mB per unit of matter. It lives in machine tanks and
 * pipes (other mods' fluid pipes can carry it too); matter containers (32 mB) scoop and place it as a block
 * (1.7.10 BlockFluidMatterPlasma: water-like, viscosity 8000, light 15). NeoForge also registers its fluid type.
 */
public final class MOFluids {
    public static final Registrar<Fluid> FLUIDS = Registrar.create(Registries.FLUID, MatterOverdrive.MODID);

    public static final RegistryEntry<Fluid, MatterPlasmaFluid> MATTER_PLASMA = FLUIDS.register("matter_plasma",
            () -> Platform.INSTANCE.matterPlasma(true));
    public static final RegistryEntry<Fluid, MatterPlasmaFluid> MATTER_PLASMA_FLOWING = FLUIDS.register("flowing_matter_plasma",
            () -> Platform.INSTANCE.matterPlasma(false));

    private MOFluids() {}
}
