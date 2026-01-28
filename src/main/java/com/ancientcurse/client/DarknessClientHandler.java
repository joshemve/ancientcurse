package com.ancientcurse.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Client-side handler for the Darkness plague event (9th Plague of Egypt).
 * Manages visual state and provides smooth interpolation for rendering.
 *
 * Handles:
 * - Smooth darkness intensity interpolation
 * - Lightning flash effects
 * - Ambient sound management
 * - Sky and fog color modifications
 * - Light source dimming state
 */
@Environment(EnvType.CLIENT)
public class DarknessClientHandler {
    // === Core State ===
    private static boolean active = false;
    private static float targetIntensity = 0.0f;
    private static float currentIntensity = 0.0f;
    private static int remainingSeconds = -1;
    private static int remainingDays = -1;

    // === Flash State ===
    private static float targetFlashIntensity = 0.0f;
    private static float currentFlashIntensity = 0.0f;
    private static int flashTimer = 0;

    // === Config values synced from server ===
    private static float fadeInSpeed = 1.0f;
    private static float fadeOutSpeed = 1.0f;
    private static float maxIntensity = 0.98f;
    private static float minAmbientLight = 0.02f;
    private static float overlayAlphaMultiplier = 1.0f;
    private static boolean showTimer = true;
    private static boolean applyDesaturation = true;
    private static float desaturationStrength = 0.7f;
    private static boolean affectSkyColor = true;
    private static boolean enableLightningFlashes = false;
    private static boolean dimLightSources = true;
    private static float lightSourceEffectiveness = 0.3f;
    private static boolean playAmbientSounds = true;
    private static float ambientSoundVolume = 0.5f;

    // === Internal state ===
    private static boolean wasActive = false;
    private static int ambientSoundCooldown = 0;

    /**
     * Called when receiving sync packet from server
     */
    public static void update(boolean serverActive, float serverIntensity, float serverFlashIntensity,
                               int serverRemainingSeconds, int serverRemainingDays,
                               float serverFadeInSpeed, float serverFadeOutSpeed,
                               float serverMaxIntensity, float serverMinAmbientLight,
                               float serverOverlayAlpha, boolean serverShowTimer,
                               boolean serverApplyDesaturation, float serverDesaturationStrength,
                               boolean serverAffectSky, boolean serverEnableFlashes,
                               boolean serverDimLights, float serverLightEffectiveness,
                               boolean serverPlayAmbient, float serverAmbientVolume) {
        active = serverActive;
        targetIntensity = serverIntensity;
        targetFlashIntensity = serverFlashIntensity;
        remainingSeconds = serverRemainingSeconds;
        remainingDays = serverRemainingDays;

        // Update config values
        fadeInSpeed = serverFadeInSpeed;
        fadeOutSpeed = serverFadeOutSpeed;
        maxIntensity = serverMaxIntensity;
        minAmbientLight = serverMinAmbientLight;
        overlayAlphaMultiplier = serverOverlayAlpha;
        showTimer = serverShowTimer;
        applyDesaturation = serverApplyDesaturation;
        desaturationStrength = serverDesaturationStrength;
        affectSkyColor = serverAffectSky;
        enableLightningFlashes = serverEnableFlashes;
        dimLightSources = serverDimLights;
        lightSourceEffectiveness = serverLightEffectiveness;
        playAmbientSounds = serverPlayAmbient;
        ambientSoundVolume = serverAmbientVolume;

        // Detect state changes for effects
        if (active && !wasActive) {
            onDarknessStart();
        } else if (!active && wasActive && currentIntensity <= 0.01f) {
            onDarknessEnd();
        }
        wasActive = active;
    }

    /**
     * Called when a lightning flash occurs
     */
    public static void onLightningFlash(float strength) {
        flashTimer = 15; // Brief flash duration
        targetFlashIntensity = strength;
        currentFlashIntensity = strength;

        // Play thunder sound
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && playAmbientSounds) {
            client.player.playSound(
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.WEATHER,
                ambientSoundVolume * 0.7f,
                0.8f + client.world.random.nextFloat() * 0.2f
            );
        }
    }

    /**
     * Called when disconnecting from server
     */
    public static void reset() {
        active = false;
        targetIntensity = 0.0f;
        currentIntensity = 0.0f;
        targetFlashIntensity = 0.0f;
        currentFlashIntensity = 0.0f;
        remainingSeconds = -1;
        remainingDays = -1;
        flashTimer = 0;
        wasActive = false;
        ambientSoundCooldown = 0;

        // Reset config to defaults
        fadeInSpeed = 1.0f;
        fadeOutSpeed = 1.0f;
        maxIntensity = 0.98f;
        minAmbientLight = 0.02f;
        overlayAlphaMultiplier = 1.0f;
        showTimer = true;
        applyDesaturation = true;
        desaturationStrength = 0.7f;
        affectSkyColor = true;
        enableLightningFlashes = false;
        dimLightSources = true;
        lightSourceEffectiveness = 0.3f;
        playAmbientSounds = true;
        ambientSoundVolume = 0.5f;
    }

    /**
     * Called every client tick for smooth interpolation
     */
    public static void tick() {
        // Smooth interpolation toward target using config speed
        float fadeIn = 0.005f * fadeInSpeed;
        float fadeOut = 0.01f * fadeOutSpeed;

        if (currentIntensity < targetIntensity) {
            currentIntensity = Math.min(targetIntensity, currentIntensity + fadeIn);
        } else if (currentIntensity > targetIntensity) {
            currentIntensity = Math.max(targetIntensity, currentIntensity - fadeOut);
        }

        // Flash interpolation (faster decay)
        if (flashTimer > 0) {
            flashTimer--;
        }
        if (currentFlashIntensity > targetFlashIntensity || flashTimer <= 0) {
            currentFlashIntensity = Math.max(0, currentFlashIntensity - 0.08f);
            if (flashTimer <= 0) {
                targetFlashIntensity = 0;
            }
        }

        // Ambient sound cooldown
        if (ambientSoundCooldown > 0) {
            ambientSoundCooldown--;
        }

        // Play occasional ambient sounds during darkness
        if (isDarknessActive() && playAmbientSounds && ambientSoundCooldown <= 0) {
            playRandomAmbientSound();
        }
    }

    /**
     * Called when darkness starts
     */
    private static void onDarknessStart() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && playAmbientSounds) {
            // Play ominous sound when darkness begins
            client.player.playSound(
                SoundEvents.AMBIENT_CAVE.value(),
                SoundCategory.AMBIENT,
                ambientSoundVolume,
                0.5f
            );
        }
    }

    /**
     * Called when darkness fully ends
     */
    private static void onDarknessEnd() {
        // Could play a "light returning" sound here
    }

    /**
     * Play random ambient sounds during darkness
     */
    private static void playRandomAmbientSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Only play when intensity is significant
        if (currentIntensity < 0.5f) return;

        // Random chance based on intensity
        if (client.world.random.nextFloat() > 0.001f * currentIntensity) return;

        // Set cooldown to prevent spam (10-30 seconds)
        ambientSoundCooldown = 200 + client.world.random.nextInt(400);

        // Play random eerie sound
        float volume = ambientSoundVolume * 0.3f * currentIntensity;
        float pitch = 0.5f + client.world.random.nextFloat() * 0.3f;

        client.player.playSound(
            SoundEvents.AMBIENT_CAVE.value(),
            SoundCategory.AMBIENT,
            volume,
            pitch
        );
    }

    // === Getters ===

    public static boolean isActive() {
        return active;
    }

    /**
     * Check if darkness is having a visual effect
     */
    public static boolean isDarknessActive() {
        return currentIntensity > 0.01f;
    }

    public static float getIntensity() {
        return currentIntensity;
    }

    /**
     * Get effective darkness (accounting for lightning flashes)
     */
    public static float getEffectiveDarkness() {
        if (currentFlashIntensity > 0) {
            return Math.max(0, currentIntensity - currentFlashIntensity);
        }
        return currentIntensity;
    }

    public static float getFlashIntensity() {
        return currentFlashIntensity;
    }

    public static boolean isFlashing() {
        return currentFlashIntensity > 0.1f;
    }

    public static int getRemainingSeconds() {
        return remainingSeconds;
    }

    public static int getRemainingDays() {
        return remainingDays;
    }

    public static boolean shouldShowTimer() {
        return showTimer;
    }

    // === Visual Calculation Methods ===

    /**
     * Get overlay alpha for rendering
     */
    public static float getOverlayAlpha() {
        float effective = getEffectiveDarkness();
        return effective * overlayAlphaMultiplier;
    }

    /**
     * Get desaturation factor for rendering (0-1)
     */
    public static float getDesaturationFactor() {
        if (!applyDesaturation) return 0;
        return currentIntensity * desaturationStrength;
    }

    /**
     * Check if desaturation should be applied
     */
    public static boolean shouldApplyDesaturation() {
        return applyDesaturation && currentIntensity > 0.01f;
    }

    /**
     * Get the sky darkness factor (0 = normal, 1 = completely dark)
     */
    public static float getSkyDarknessFactor() {
        if (!affectSkyColor) return 0;
        return getEffectiveDarkness();
    }

    /**
     * Get ambient light level (for shader/rendering use)
     */
    public static float getAmbientLightLevel() {
        float effective = getEffectiveDarkness();
        // During flash, more light
        if (isFlashing()) {
            return Math.min(1.0f, minAmbientLight + currentFlashIntensity * (1.0f - minAmbientLight));
        }
        // Interpolate between full light (1.0) and minimum ambient based on intensity
        return 1.0f - (effective * (1.0f - minAmbientLight));
    }

    /**
     * Check if light sources should be dimmed
     */
    public static boolean shouldDimLightSources() {
        return dimLightSources && currentIntensity > 0.3f;
    }

    /**
     * Get light source effectiveness multiplier
     */
    public static float getLightSourceMultiplier() {
        if (!dimLightSources) return 1.0f;
        // Interpolate from 1.0 to lightSourceEffectiveness based on intensity
        return 1.0f - (currentIntensity * (1.0f - lightSourceEffectiveness));
    }

    /**
     * Get the darkness overlay color (ARGB)
     */
    public static int getDarknessOverlayColor() {
        float effective = getEffectiveDarkness();
        int alpha = (int)(effective * 255 * overlayAlphaMultiplier);
        alpha = Math.min(255, Math.max(0, alpha));

        // Very dark blue-black color
        int r = 2;
        int g = 2;
        int b = 8;

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Get the flash overlay color (ARGB) - bright white
     */
    public static int getFlashOverlayColor() {
        if (currentFlashIntensity <= 0) return 0;

        int alpha = (int)(currentFlashIntensity * 200);
        alpha = Math.min(200, Math.max(0, alpha));

        return (alpha << 24) | (255 << 16) | (255 << 8) | 255;
    }

    /**
     * Get fog color during darkness (dark gray/black)
     */
    public static int getFogColor() {
        float effective = getEffectiveDarkness();
        int brightness = (int)((1.0f - effective) * 255);
        brightness = Math.max(5, brightness); // Never completely black for visibility

        return (255 << 24) | (brightness << 16) | (brightness << 8) | brightness;
    }

    /**
     * Get sky color during darkness
     */
    public static int getSkyColor() {
        float effective = getEffectiveDarkness();
        // Lerp from normal sky (0,0,0 as base) to very dark
        int brightness = (int)((1.0f - effective) * 20); // Max brightness 20 even at day

        return (255 << 24) | (brightness << 16) | (brightness << 8) | (brightness + 5);
    }

    /**
     * Format remaining time for display
     */
    public static String getFormattedRemainingTime() {
        if (remainingSeconds < 0) {
            return "Infinite";
        }
        if (remainingDays > 0) {
            int hours = (remainingSeconds % 86400) / 3600;
            return remainingDays + "d " + hours + "h";
        }

        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
