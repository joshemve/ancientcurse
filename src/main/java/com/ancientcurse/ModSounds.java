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

    // Anubis Sound Events - Movement
    public static final SoundEvent ANUBIS_STEP = registerSoundEvent("anubis_step");
    public static final SoundEvent ANUBIS_STEP_HEAVY = registerSoundEvent("anubis_step_heavy");
    public static final SoundEvent ANUBIS_FOOTSTEP_1 = registerSoundEvent("anubis_footstep_1");
    
    // Anubis Voice - Hurt variations (randomly chosen when hit)
    public static final SoundEvent ANUBIS_HURT_1 = registerSoundEvent("anubis_hurt_1");
    public static final SoundEvent ANUBIS_HURT_2 = registerSoundEvent("anubis_hurt_2");
    public static final SoundEvent ANUBIS_HURT_3 = registerSoundEvent("anubis_hurt_3");
    public static final SoundEvent ANUBIS_HURT_4 = registerSoundEvent("anubis_hurt_4");
    
    // Anubis Voice - Breathing (ambient/idle)
    public static final SoundEvent ANUBIS_BREATH_1 = registerSoundEvent("anubis_breath_1");
    public static final SoundEvent ANUBIS_BREATH_2 = registerSoundEvent("anubis_breath_2");
    public static final SoundEvent ANUBIS_RUNNING_BREATHING = registerSoundEvent("anubis_running_breathing");
    
    // Anubis Voice - Howls (sky yell attack)
    public static final SoundEvent ANUBIS_HOWL_1 = registerSoundEvent("anubis_howl_1");
    public static final SoundEvent ANUBIS_HOWL_2 = registerSoundEvent("anubis_howl_2");
    
    // Anubis Voice - Whoosh (attack sounds)
    public static final SoundEvent ANUBIS_WHOOSH_1 = registerSoundEvent("anubis_whoosh_1");
    public static final SoundEvent ANUBIS_WHOOSH_2 = registerSoundEvent("anubis_whoosh_2");
    public static final SoundEvent ANUBIS_ATTACK_1_WHOOSH_LAYER = registerSoundEvent("anubis_attack_1_whoosh_layer");
    
    // Anubis Voice - Evil Laughs (when damaging player)
    public static final SoundEvent ANUBIS_EVIL_LAUGH_1 = registerSoundEvent("anubis_evil_laugh_1");
    public static final SoundEvent ANUBIS_EVIL_LAUGH_2 = registerSoundEvent("anubis_evil_laugh_2");
    
    // Anubis Voice - Judgment
    public static final SoundEvent ANUBIS_JUDGEMENT = registerSoundEvent("anubis_judgement");
    public static final SoundEvent ANUBIS_JUDGEMENT_SAFE = registerSoundEvent("anubis_judgement_safe");
    
    // Anubis Voice - Combat
    public static final SoundEvent ANUBIS_HEART_IS_HEAVY = registerSoundEvent("anubis_heart_is_heavy");
    
    // Anubis - Judgment Effects
    public static final SoundEvent ANUBIS_JUDGEMENT_WHOOSH = registerSoundEvent("anubis_judgement_whoosh");
    public static final SoundEvent ANUBIS_MAGIC_SHIMMER = registerSoundEvent("anubis_magic_shimmer");
    public static final SoundEvent ANUBIS_JUDGEMENT_LOOP = registerSoundEvent("anubis_judgement_loop");

    // Zulmak Sound Events - Ambient/Idle
    public static final SoundEvent ZULMAK_AWAKENED = registerSoundEvent("zulmak_awakened");
    
    // Zulmak - Combat
    public static final SoundEvent ZULMAK_BLOCKING = registerSoundEvent("zulmak_blocking");
    public static final SoundEvent ZULMAK_FIRST_HIT = registerSoundEvent("zulmak_first_hit");
    public static final SoundEvent ZULMAK_HURT = registerSoundEvent("zulmak_hurt");
    public static final SoundEvent ZULMAK_DEATH = registerSoundEvent("zulmak_death");
    public static final SoundEvent ZULMAK_PLAYER_ZAP = registerSoundEvent("zulmak_player_zap");
    public static final SoundEvent ZULMAK_WIND_HIT = registerSoundEvent("zulmak_wind_hit");
    
    // Zulmak - Special Attacks
    public static final SoundEvent ZULMAK_CURSE_ATTACK = registerSoundEvent("zulmak_curse_attack");
    public static final SoundEvent ZULMAK_CURSED_EARTH_ATTACK = registerSoundEvent("zulmak_cursed_earth_attack");
    public static final SoundEvent ZULMAK_LOCUST_SWARM = registerSoundEvent("zulmak_locust_swarm");
    
    // Zulmak - Summon
    public static final SoundEvent ZULMAK_SUMMON = registerSoundEvent("zulmak_summon");
    public static final SoundEvent ZULMAK_SUMMON_SCREAM = registerSoundEvent("zulmak_summon_scream");
    
    // Zulmak - Ambient Loop (spinning blocks vortex)
    public static final SoundEvent ZULMAK_VORTEX_WIND_LOOP = registerSoundEvent("zulmak_vortex_wind_loop");

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
