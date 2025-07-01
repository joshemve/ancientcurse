package com.ancientcurse;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // Thoth Sound Events
    public static final SoundEvent THOTH_AMBIENT = registerSoundEvent("thoth_ambient");
    public static final SoundEvent THOTH_ATTACK_MAGIC_BALL = registerSoundEvent("thoth_attack_magic_ball");
    public static final SoundEvent THOTH_ATTACK_SCROLL_BLAST = registerSoundEvent("thoth_attack_scroll_blast");
    public static final SoundEvent THOTH_ATTACK_TIME_BEND = registerSoundEvent("thoth_attack_time_bend");
    public static final SoundEvent THOTH_SPAWN = registerSoundEvent("thoth_spawn");
    public static final SoundEvent THOTH_DEATH = registerSoundEvent("thoth_death");
    public static final SoundEvent THOTH_HURT = registerSoundEvent("thoth_hurt");
    public static final SoundEvent THOTH_SUMMON = registerSoundEvent("thoth_summon");


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
