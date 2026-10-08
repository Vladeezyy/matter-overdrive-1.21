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

    public static final DeferredHolder<SoundEvent, SoundEvent> PHASER_BEAM = sound("phaser_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHASER_SWITCH_MODE = sound("phaser_switch_mode");

    // Androids (phase 6)
    public static final DeferredHolder<SoundEvent, SoundEvent> GLITCH = sound("glitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSFORMATION_MUSIC = sound("transformation_music");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOTIC_STAT_UNLOCK = sound("biotic_stat_unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANDROID_TELEPORT = sound("android_teleport");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIELD_LOOP = sound("shield_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIELD_HIT = sound("shield_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIELD_POWER_UP = sound("shield_power_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIELD_POWER_DOWN = sound("shield_power_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOAK_ON = sound("cloak_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOAK_OFF = sound("cloak_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHT_VISION = sound("night_vision");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_DOWN = sound("power_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOCKWAVE = sound("shockwave");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, name)));
    }

    private MOSounds() {}
}
