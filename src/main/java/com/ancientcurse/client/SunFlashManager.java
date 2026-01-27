package com.ancientcurse.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side manager for sun flash visual effects.
 * Creates a flashbang-style screen overlay that fades from bright white to golden.
 * Used by Ra's attacks and the Staff of Ra item.
 */
@Environment(EnvType.CLIENT)
public class SunFlashManager {
    private static float intensity = 0;
    private static float duration = 0;
    private static float elapsed = 0;

    // Phase timing (as percentage of total duration)
    private static final float INTENSE_PHASE_END = 0.2f; // First 20% is intense white flash

    /**
     * Trigger a sun flash effect.
     *
     * @param flashIntensity Intensity multiplier (1.0 = full flash, 0.5 = half intensity)
     * @param durationTicks  Duration in ticks (20 ticks = 1 second)
     */
    public static void flash(float flashIntensity, float durationTicks) {
        // Calculate remaining intensity of current flash
        float remainingIntensity = 0;
        if (duration > 0 && elapsed < duration) {
            remainingIntensity = intensity * (1.0f - elapsed / duration);
        }

        // Only override if new flash is stronger than remaining
        if (flashIntensity > remainingIntensity) {
            intensity = flashIntensity;
            duration = durationTicks;
            elapsed = 0;
        }
    }

    /**
     * Called every client tick to update flash state.
     */
    public static void tick() {
        if (elapsed < duration) {
            elapsed++;
        }
    }

    /**
     * Check if a flash is currently active.
     */
    public static boolean isFlashing() {
        return elapsed < duration && intensity > 0;
    }

    /**
     * Get the current flash progress (0.0 = start, 1.0 = end).
     *
     * @param tickDelta Partial tick for smooth interpolation
     * @return Progress value from 0 to 1
     */
    public static float getProgress(float tickDelta) {
        if (elapsed >= duration || intensity <= 0) {
            return 1.0f;
        }
        return (elapsed + tickDelta) / duration;
    }

    /**
     * Get the current overlay alpha value.
     *
     * @param tickDelta Partial tick for smooth interpolation
     * @return Alpha value from 0 to 1
     */
    public static float getAlpha(float tickDelta) {
        if (!isFlashing()) {
            return 0;
        }

        float progress = getProgress(tickDelta);

        // Two-phase alpha curve:
        // Phase 1 (0-20%): Intense flash, high alpha (0.85 -> 0.7)
        // Phase 2 (20-100%): Fading out (0.7 -> 0)
        float alpha;
        if (progress < INTENSE_PHASE_END) {
            // Intense phase - start very bright, slight decay
            float phaseProgress = progress / INTENSE_PHASE_END;
            alpha = 0.85f - (0.15f * phaseProgress);
        } else {
            // Decay phase - fade out smoothly
            float phaseProgress = (progress - INTENSE_PHASE_END) / (1.0f - INTENSE_PHASE_END);
            // Use ease-out curve for smooth fade
            alpha = 0.7f * (1.0f - (phaseProgress * phaseProgress));
        }

        return alpha * intensity;
    }

    /**
     * Get the current overlay color as ARGB int.
     * Transitions from bright white-yellow to golden as flash fades.
     *
     * @param tickDelta Partial tick for smooth interpolation
     * @return ARGB color int
     */
    public static int getColor(float tickDelta) {
        if (!isFlashing()) {
            return 0;
        }

        float progress = getProgress(tickDelta);
        float alpha = getAlpha(tickDelta);

        // Color transition:
        // Phase 1: Bright white-yellow (255, 255, 240)
        // Phase 2: Fading to golden (255, 215, 100)
        int r, g, b;
        if (progress < INTENSE_PHASE_END) {
            // Intense phase - bright white-yellow
            r = 255;
            g = 255;
            b = 240;
        } else {
            // Decay phase - transition to golden
            float phaseProgress = (progress - INTENSE_PHASE_END) / (1.0f - INTENSE_PHASE_END);
            r = 255;
            g = (int) (255 - (40 * phaseProgress)); // 255 -> 215
            b = (int) (240 - (140 * phaseProgress)); // 240 -> 100
        }

        int a = (int) (alpha * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Reset all flash state.
     */
    public static void reset() {
        intensity = 0;
        duration = 0;
        elapsed = 0;
    }
}
