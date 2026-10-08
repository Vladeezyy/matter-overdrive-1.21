package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.PlasmaBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MatterOverdrive.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PlasmaBolt>> PLASMA_BOLT = ENTITIES.registerEntityType("plasma_bolt",
            PlasmaBolt::new, MobCategory.MISC, b -> b.sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(1).noSave());

    private MOEntities() {}
}
