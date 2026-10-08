package matteroverdrive.init;

import java.util.List;

import com.mojang.serialization.Codec;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.matter.ItemPattern;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MODataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MatterOverdrive.MODID);

    /** Stored energy (FE) of batteries and of machines carried as items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Matter carried by matter dust (1.7.10 kept it in the item damage). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MATTER =
            COMPONENTS.registerComponentType("matter", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Item patterns on a pattern drive. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemPattern>>> PATTERNS =
            COMPONENTS.registerComponentType("patterns", b -> b.persistent(ItemPattern.CODEC.listOf())
                    .networkSynchronized(ItemPattern.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** Energy weapon heat (1.7.10 "heat" tag) and whether it overheated. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> HEAT =
            COMPONENTS.registerComponentType("heat", b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OVERHEATED =
            COMPONENTS.registerComponentType("overheated", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Modules installed in an energy weapon (1.7.10 kept them as an inventory in the weapon's NBT). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> WEAPON_MODULES =
            COMPONENTS.registerComponentType("weapon_modules", b -> b.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    private MODataComponents() {}
}
