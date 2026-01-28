package com.ancientcurse.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Client-side handler for the Famine event.
 * Manages visual state and provides smooth interpolation for rendering.
 */
@Environment(EnvType.CLIENT)
public class FamineClientHandler {
    private static boolean active = false;
    private static float targetIntensity = 0.0f;
    private static float currentIntensity = 0.0f;
    private static int remainingSeconds = -1;

    // Config values synced from server
    private static float fadeInSpeed = 1.0f;
    private static float fadeOutSpeed = 1.0f;
    private static float vignetteAlphaMultiplier = 1.0f;
    private static boolean showTimer = true;
    private static boolean showSpoilageFlash = true;

    // For spoilage feedback
    private static int spoilageFlashTimer = 0;
    private static int lastSpoiledSlot = -1;
    private static int lastSpoiledAmount = 0;

    /**
     * Called when receiving sync packet from server
     */
    public static void update(boolean serverActive, float serverIntensity, int serverRemainingSeconds,
                               float serverFadeInSpeed, float serverFadeOutSpeed,
                               float serverVignetteAlpha, boolean serverShowTimer, boolean serverShowSpoilageFlash) {
        active = serverActive;
        targetIntensity = serverIntensity;
        remainingSeconds = serverRemainingSeconds;

        // Update config values
        fadeInSpeed = serverFadeInSpeed;
        fadeOutSpeed = serverFadeOutSpeed;
        vignetteAlphaMultiplier = serverVignetteAlpha;
        showTimer = serverShowTimer;
        showSpoilageFlash = serverShowSpoilageFlash;
    }

    /**
     * Called when food spoils in the player's inventory
     */
    public static void onFoodSpoiled(int slot, int amount) {
        if (!showSpoilageFlash) return;

        lastSpoiledSlot = slot;
        lastSpoiledAmount = amount;
        spoilageFlashTimer = 40; // 2 seconds of feedback

        // Play spoilage sound
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 0.8f);
        }
    }

    /**
     * Called when disconnecting from server
     */
    public static void reset() {
        active = false;
        targetIntensity = 0.0f;
        currentIntensity = 0.0f;
        remainingSeconds = -1;
        spoilageFlashTimer = 0;

        // Reset config to defaults
        fadeInSpeed = 1.0f;
        fadeOutSpeed = 1.0f;
        vignetteAlphaMultiplier = 1.0f;
        showTimer = true;
        showSpoilageFlash = true;
    }

    /**
     * Called every client tick for smooth interpolation
     */
    public static void tick() {
        // Smooth interpolation toward target using config speed
        float fadeIn = 0.01f * fadeInSpeed;
        float fadeOut = 0.02f * fadeOutSpeed;

        if (currentIntensity < targetIntensity) {
            currentIntensity = Math.min(targetIntensity, currentIntensity + fadeIn);
        } else if (currentIntensity > targetIntensity) {
            currentIntensity = Math.max(targetIntensity, currentIntensity - fadeOut);
        }

        // Decay spoilage flash
        if (spoilageFlashTimer > 0) {
            spoilageFlashTimer--;
        }
    }

    // === Getters ===

    public static boolean isActive() {
        return active;
    }

    public static boolean isFamineActive() {
        return currentIntensity > 0.01f;
    }

    public static float getIntensity() {
        return currentIntensity;
    }

    public static int getRemainingSeconds() {
        return remainingSeconds;
    }

    public static boolean isSpoilageFlashing() {
        return spoilageFlashTimer > 0 && showSpoilageFlash;
    }

    public static float getSpoilageFlashAlpha() {
        if (spoilageFlashTimer <= 0 || !showSpoilageFlash) return 0;
        // Pulsing effect
        float base = spoilageFlashTimer / 40.0f;
        return base * (0.5f + 0.5f * (float)Math.sin(spoilageFlashTimer * 0.5f));
    }

    public static int getLastSpoiledSlot() {
        return lastSpoiledSlot;
    }

    public static int getLastSpoiledAmount() {
        return lastSpoiledAmount;
    }

    /**
     * Get a desaturation factor for rendering (brown/dead look)
     * Returns 0-1 where 1 is fully desaturated
     */
    public static float getDesaturationFactor() {
        return currentIntensity * 0.4f; // Max 40% desaturation
    }

    /**
     * Get vignette alpha multiplier from config
     */
    public static float getVignetteAlphaMultiplier() {
        return vignetteAlphaMultiplier;
    }

    /**
     * Check if timer should be shown
     */
    public static boolean shouldShowTimer() {
        return showTimer;
    }

    /**
     * Get the brownish tint color for famine overlay
     */
    public static int getFamineTintColor() {
        // Sickly brown-yellow
        int r = (int)(180 * currentIntensity);
        int g = (int)(140 * currentIntensity);
        int b = (int)(80 * currentIntensity);
        int a = (int)(60 * currentIntensity * vignetteAlphaMultiplier);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
