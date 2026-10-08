package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.entity.PlasmaBolt;
import matteroverdrive.entity.animal.FailedChicken;
import matteroverdrive.entity.animal.FailedCow;
import matteroverdrive.entity.animal.FailedPig;
import matteroverdrive.entity.animal.FailedSheep;
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

    // 1.7.10 failed animals: vanilla sizes
    public static final DeferredHolder<EntityType<?>, EntityType<FailedPig>> FAILED_PIG = ENTITIES.registerEntityType("failed_pig",
            FailedPig::new, MobCategory.CREATURE, b -> b.sized(0.9f, 0.9f).passengerAttachments(0.86875f).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<FailedCow>> FAILED_COW = ENTITIES.registerEntityType("failed_cow",
            FailedCow::new, MobCategory.CREATURE, b -> b.sized(0.9f, 1.4f).eyeHeight(1.3f).passengerAttachments(1.36875f).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<FailedChicken>> FAILED_CHICKEN = ENTITIES.registerEntityType("failed_chicken",
            FailedChicken::new, MobCategory.CREATURE, b -> b.sized(0.4f, 0.7f).eyeHeight(0.644f)
                    .passengerAttachments(new net.minecraft.world.phys.Vec3(0.0, 0.7, -0.1)).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<FailedSheep>> FAILED_SHEEP = ENTITIES.registerEntityType("failed_sheep",
            FailedSheep::new, MobCategory.CREATURE, b -> b.sized(0.9f, 1.3f).eyeHeight(1.235f).passengerAttachments(1.2375f).clientTrackingRange(10));

    private MOEntities() {}
}
