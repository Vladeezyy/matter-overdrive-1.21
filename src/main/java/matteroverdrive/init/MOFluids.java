package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Matter Plasma: 1.7.10 stored and moved matter as this fluid, 1 mB per unit of matter. It has no block or
 * bucket; it lives in machine tanks and pipes, and other mods' fluid pipes can carry it too.
 */
public final class MOFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MatterOverdrive.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MatterOverdrive.MODID);

    public static final DeferredHolder<FluidType, FluidType> MATTER_PLASMA_TYPE = FLUID_TYPES.register("matter_plasma",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.matteroverdrive.matter_plasma").lightLevel(10)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> MATTER_PLASMA = FLUIDS.register("matter_plasma",
            () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> MATTER_PLASMA_FLOWING = FLUIDS.register("flowing_matter_plasma",
            () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(MATTER_PLASMA_TYPE, MATTER_PLASMA, MATTER_PLASMA_FLOWING);
    }

    private MOFluids() {}
}
