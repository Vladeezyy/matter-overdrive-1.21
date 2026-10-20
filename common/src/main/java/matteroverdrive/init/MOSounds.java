package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import matteroverdrive.platform.RegistryEntry;
import matteroverdrive.platform.Registrar;

/** Sounds from the 1.7.10 sounds.json (stereo files are converted to mono so they attenuate). */
public final class MOSounds {
    public static final Registrar<SoundEvent> SOUNDS = Registrar.create(Registries.SOUND_EVENT, MatterOverdrive.MODID);

    public static final RegistryEntry<SoundEvent, SoundEvent> PHASER_RIFLE_SHOT = sound("phaser_rifle_shot");
    public static final RegistryEntry<SoundEvent, SoundEvent> PLASMA_SHOTGUN_SHOT = sound("plasma_shotgun_shot");
    public static final RegistryEntry<SoundEvent, SoundEvent> SNIPER_RIFLE_FIRE = sound("sniper_rifle_fire");
    public static final RegistryEntry<SoundEvent, SoundEvent> RELOAD = sound("reload");
    public static final RegistryEntry<SoundEvent, SoundEvent> OVERHEAT = sound("overheat");
    public static final RegistryEntry<SoundEvent, SoundEvent> OVERHEAT_ALARM = sound("overheat_alarm");

    public static final RegistryEntry<SoundEvent, SoundEvent> PHASER_BEAM = sound("phaser_beam");
    public static final RegistryEntry<SoundEvent, SoundEvent> PHASER_SWITCH_MODE = sound("phaser_switch_mode");
    public static final RegistryEntry<SoundEvent, SoundEvent> LASER_FIRE = sound("laser_fire");
    public static final RegistryEntry<SoundEvent, SoundEvent> OMNI_TOOL_HUM = sound("omni_tool_hum");

    // Androids (phase 6)
    public static final RegistryEntry<SoundEvent, SoundEvent> GLITCH = sound("glitch");
    public static final RegistryEntry<SoundEvent, SoundEvent> TRANSFORMATION_MUSIC = sound("transformation_music");
    public static final RegistryEntry<SoundEvent, SoundEvent> BIOTIC_STAT_UNLOCK = sound("biotic_stat_unlock");
    public static final RegistryEntry<SoundEvent, SoundEvent> ANDROID_TELEPORT = sound("android_teleport");
    public static final RegistryEntry<SoundEvent, SoundEvent> SHIELD_LOOP = sound("shield_loop");
    public static final RegistryEntry<SoundEvent, SoundEvent> SHIELD_HIT = sound("shield_hit");
    public static final RegistryEntry<SoundEvent, SoundEvent> SHIELD_POWER_UP = sound("shield_power_up");
    public static final RegistryEntry<SoundEvent, SoundEvent> SHIELD_POWER_DOWN = sound("shield_power_down");
    public static final RegistryEntry<SoundEvent, SoundEvent> CLOAK_ON = sound("cloak_on");
    public static final RegistryEntry<SoundEvent, SoundEvent> CLOAK_OFF = sound("cloak_off");
    public static final RegistryEntry<SoundEvent, SoundEvent> NIGHT_VISION = sound("night_vision");
    public static final RegistryEntry<SoundEvent, SoundEvent> POWER_DOWN = sound("power_down");
    public static final RegistryEntry<SoundEvent, SoundEvent> SHOCKWAVE = sound("shockwave");

    // Mobs (phase 7b)
    public static final RegistryEntry<SoundEvent, SoundEvent> ROGUE_ANDROID_SAY = sound("rogue_android_say");
    public static final RegistryEntry<SoundEvent, SoundEvent> ROGUE_ANDROID_DEATH = sound("rogue_android_death");
    public static final RegistryEntry<SoundEvent, SoundEvent> FAILED_ANIMAL_IDLE_PIG = sound("failed_animal_idle_pig");
    public static final RegistryEntry<SoundEvent, SoundEvent> FAILED_ANIMAL_IDLE_COW = sound("failed_animal_idle_cow");
    public static final RegistryEntry<SoundEvent, SoundEvent> FAILED_ANIMAL_IDLE_CHICKEN = sound("failed_animal_idle_chicken");
    public static final RegistryEntry<SoundEvent, SoundEvent> FAILED_ANIMAL_IDLE_SHEEP = sound("failed_animal_idle_sheep");
    public static final RegistryEntry<SoundEvent, SoundEvent> FAILED_ANIMAL_DIE = sound("failed_animal_die");
    public static final RegistryEntry<SoundEvent, SoundEvent> CRATE_OPEN = sound("crate_open");
    public static final RegistryEntry<SoundEvent, SoundEvent> TRANSPORTER = sound("transporter");
    public static final RegistryEntry<SoundEvent, SoundEvent> QUEST_STARTED = sound("quest_started");
    /** 1.7.10 gui.button_soft (MOElementButton's click). */
    public static final RegistryEntry<SoundEvent, SoundEvent> BUTTON_SOFT = sound("button_soft");
    public static final RegistryEntry<SoundEvent, SoundEvent> QUEST_COMPLETE = sound("quest_complete");
    public static final RegistryEntry<SoundEvent, SoundEvent> SCANNER_SCANNING = sound("scanner_scanning");
    public static final RegistryEntry<SoundEvent, SoundEvent> SCANNER_SUCCESS = sound("scanner_success");
    public static final RegistryEntry<SoundEvent, SoundEvent> SCANNER_FAIL = sound("scanner_fail");
    public static final RegistryEntry<SoundEvent, SoundEvent> SCANNER_BEEP = sound("scanner_beep");
    public static final RegistryEntry<SoundEvent, SoundEvent> CRATE_CLOSE = sound("crate_close");

    // Machines: loops while active (1.7.10 MOTileEntityMachine.getSound), replication, the anomaly's wind
    public static final RegistryEntry<SoundEvent, SoundEvent> MACHINE = sound("machine");
    public static final RegistryEntry<SoundEvent, SoundEvent> ANALYZER = sound("analyzer");
    public static final RegistryEntry<SoundEvent, SoundEvent> FORCE_FIELD = sound("force_field");
    public static final RegistryEntry<SoundEvent, SoundEvent> REPLICATE_SUCCESS = sound("replicate_success");
    public static final RegistryEntry<SoundEvent, SoundEvent> WINDY = sound("windy");
    // Plasma bolt hits and the plasma shotgun's charge-up
    public static final RegistryEntry<SoundEvent, SoundEvent> SIZZLE = sound("sizzle");
    public static final RegistryEntry<SoundEvent, SoundEvent> LASER_RICOCHET = sound("laser_ricochet");
    public static final RegistryEntry<SoundEvent, SoundEvent> PLASMA_SHOTGUN_CHARGING = sound("plasma_shotgun_charging");

    private static RegistryEntry<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MatterOverdrive.MODID, name)));
    }

    private MOSounds() {}
}
