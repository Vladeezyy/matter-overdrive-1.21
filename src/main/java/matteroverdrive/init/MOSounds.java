package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sounds from the 1.7.10 sounds.json (stereo files are converted to mono so they attenuate). */
public final class MOSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MatterOverdrive.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PHASER_RIFLE_SHOT = sound("phaser_rifle_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_SHOTGUN_SHOT = sound("plasma_shotgun_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNIPER_RIFLE_FIRE = sound("sniper_rifle_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD = sound("reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERHEAT = sound("overheat");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERHEAT_ALARM = sound("overheat_alarm");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, name)));
    }

    private MOSounds() {}
}
