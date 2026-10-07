package matteroverdrive.init;

import com.mojang.serialization.Codec;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MODataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MatterOverdrive.MODID);

    /** Stored energy (FE) of batteries and of machines carried as items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private MODataComponents() {}
}
