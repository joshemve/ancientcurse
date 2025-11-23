package com.ancientcurse;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // Thoth Sound Events
    public static final SoundEvent THOTH_AMBIENT = registerSoundEvent("thoth_ambient");
    public static final SoundEvent THOTH_ATTACK_MAGIC_BALL = registerSoundEvent("thoth_attack_magic_ball");
    public static final SoundEvent THOTH_ATTACK_MELEE = registerSoundEvent("thoth_attack_melee");
    public static final SoundEvent THOTH_ATTACK_SCROLL_BLAST = registerSoundEvent("thoth_attack_scroll_blast");
    public static final SoundEvent THOTH_ATTACK_TIME_BEND = registerSoundEvent("thoth_attack_time_bend");
    public static final SoundEvent THOTH_SPAWN = registerSoundEvent("thoth_spawn");
    public static final SoundEvent THOTH_DEATH = registerSoundEvent("thoth_death");
    public static final SoundEvent THOTH_HURT = registerSoundEvent("thoth_hurt");
    public static final SoundEvent THOTH_SUMMON = registerSoundEvent("thoth_summon");
    public static final SoundEvent THOTH_INTRUDERS = registerSoundEvent("thoth_intruders");
    public static final SoundEvent THOTH_YELL = registerSoundEvent("thoth_yell");
    public static final SoundEvent THOTH_WIND_GUST = registerSoundEvent("thoth_wind_gust");
    public static final SoundEvent THOTH_MAGIC_ATTACK = registerSoundEvent("thoth_magic_attack");
    
    // Thoth laugh variants (randomly chosen for personality)
    public static final SoundEvent THOTH_LAUGH_1 = registerSoundEvent("thoth_laugh_1");
    public static final SoundEvent THOTH_LAUGH_2 = registerSoundEvent("thoth_laugh_2");
    public static final SoundEvent THOTH_LAUGH_3 = registerSoundEvent("thoth_laugh_3");
    public static final SoundEvent THOTH_LAUGH_4 = registerSoundEvent("thoth_laugh_4");
    
    // Additional hit/breath sounds
    public static final SoundEvent THOTH_HIT_2 = registerSoundEvent("thoth_hit_2");
    public static final SoundEvent THOTH_BREATH_1 = registerSoundEvent("thoth_breath_1");


    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(AncientCurse.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        AncientCurse.LOGGER.info("Registering custom sounds for " + AncientCurse.MOD_ID);
        // Sound events are automatically registered when the static fields are initialized
        // This method is just for logging purposes
    }
}
