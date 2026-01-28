package com.ancientcurse.client.sound;

import com.ancientcurse.client.SandstormClientHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class SandstormLoopSoundInstance extends MovingSoundInstance {
    private final float maxVolume;

    public SandstormLoopSoundInstance(SoundEvent sound, float maxVolume) {
        super(sound, SoundCategory.AMBIENT, Random.create());
        this.maxVolume = maxVolume;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.01F; // Start slightly audible
        this.pitch = 1.0F;
        this.relative = true; // Global sound (no position)
    }

    @Override
    public void tick() {
        // If the sound system thinks we are done, we are done
        if (this.isDone()) {
            return;
        }

        // If sandstorm is completely off/inactive
        if (!SandstormClientHandler.isRenderingSandstorm() && SandstormClientHandler.getIntensity() <= 0.01f) {
            this.volume -= 0.05F; // Fade out quickly
            if (this.volume <= 0.0F) {
                this.setDone();
            }
            return;
        }

        float intensity = SandstormClientHandler.getIntensity();
        
        // Target volume scales with intensity
        float targetVolume = intensity * this.maxVolume;
        
        // Smooth fade to target
        if (this.volume < targetVolume) {
            this.volume = Math.min(targetVolume, this.volume + 0.02F);
        } else if (this.volume > targetVolume) {
            this.volume = Math.max(targetVolume, this.volume - 0.02F);
        }
    }
}
