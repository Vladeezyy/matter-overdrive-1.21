package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

/** 1.7.10 AndroidAttributes: glitch time and battery use multipliers, changed by stats and bionic parts. */
public final class MOAttributes {
    public static final Registrar<Attribute> ATTRIBUTES = Registrar.create(Registries.ATTRIBUTE, MatterOverdrive.MODID);

    public static final RegistryEntry<Attribute, Attribute> GLITCH_TIME = ATTRIBUTES.register("android_glitch_time",
            () -> new RangedAttribute("attribute.name." + MatterOverdrive.MODID + ".android_glitch_time", 1, 0, 1).setSyncable(true));
    public static final RegistryEntry<Attribute, Attribute> BATTERY_USE = ATTRIBUTES.register("android_battery_use",
            () -> new RangedAttribute("attribute.name." + MatterOverdrive.MODID + ".android_battery_use", 1, 0, Double.MAX_VALUE).setSyncable(true));

    private MOAttributes() {}
}
