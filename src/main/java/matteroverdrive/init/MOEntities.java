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

    // 1.7.10 rogue androids: biped size; the melee one is the "rogue_android" of the original
    public static final DeferredHolder<EntityType<?>, EntityType<matteroverdrive.entity.monster.MeleeRogueAndroid>> ROGUE_ANDROID =
            ENTITIES.registerEntityType("rogue_android", matteroverdrive.entity.monster.MeleeRogueAndroid::new, MobCategory.MONSTER,
                    b -> b.sized(0.6f, 1.8f).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<matteroverdrive.entity.monster.RangedRogueAndroid>> RANGED_ROGUE_ANDROID =
            ENTITIES.registerEntityType("ranged_rogue_android", matteroverdrive.entity.monster.RangedRogueAndroid::new, MobCategory.MONSTER,
                    b -> b.sized(0.6f, 1.8f).clientTrackingRange(8));

    private MOEntities() {}
}
