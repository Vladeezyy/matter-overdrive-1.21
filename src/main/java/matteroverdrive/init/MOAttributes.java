package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 1.7.10 AndroidAttributes: glitch time and battery use multipliers, changed by stats and bionic parts. */
public final class MOAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MatterOverdrive.MODID);

    public static final DeferredHolder<Attribute, Attribute> GLITCH_TIME = ATTRIBUTES.register("android_glitch_time",
            () -> new RangedAttribute("attribute.name." + MatterOverdrive.MODID + ".android_glitch_time", 1, 0, 1).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> BATTERY_USE = ATTRIBUTES.register("android_battery_use",
            () -> new RangedAttribute("attribute.name." + MatterOverdrive.MODID + ".android_battery_use", 1, 0, Double.MAX_VALUE).setSyncable(true));

    private MOAttributes() {}
}
