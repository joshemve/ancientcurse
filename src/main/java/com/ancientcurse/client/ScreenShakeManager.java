package com.ancientcurse.client;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Client-side manager for screen shake effects.
 * Uses Perlin-like noise for smooth, organic shake with dramatic impact feel.
 */
public class ScreenShakeManager {
    private static float intensity = 0;
    private static float duration = 0;
    private static float elapsed = 0;
    private static float frequency = 6.0f; // Slower shake for dramatic wobble (was 15)

    // Noise seeds for smooth shake
    private static final Random RANDOM = Random.create();
    private static long noiseSeed = RANDOM.nextLong();

    /**
     * Trigger a screen shake effect.
     *
     * @param shakeIntensity Intensity in degrees (1.0 = subtle, 5.0 = strong)
     * @param durationTicks  Duration in ticks (20 ticks = 1 second)
     */
    public static void shake(float shakeIntensity, float durationTicks) {

        // Calculate remaining intensity of current shake (avoid divide by zero)
        float remainingIntensity = 0;
        if (duration > 0 && elapsed < duration) {
            remainingIntensity = intensity * (1.0f - elapsed / duration);
        }

        // Only override if new shake is stronger than remaining
        if (shakeIntensity > remainingIntensity) {
            intensity = shakeIntensity;
            duration = durationTicks;
            elapsed = 0;
            noiseSeed = RANDOM.nextLong(); // New shake pattern
        }
    }

    /**
     * Called every client tick to update shake state.
     */
    public static void tick() {
        if (elapsed < duration) {
            elapsed++;
        }
    }

    /**
     * Get the current shake offsets for pitch (X) and roll (Z).
     *
     * @param tickDelta Partial tick for smooth interpolation
     * @return float array [pitchOffset, rollOffset] in degrees
     */
    public static float[] getShakeOffset(float tickDelta) {
        if (elapsed >= duration || intensity <= 0) {
            return new float[] { 0, 0 };
        }

        float progress = (elapsed + tickDelta) / duration;

        // Two-phase decay: sharp initial hit, then slower decay
        // First 20% of duration: rapid oscillation with high intensity
        // Remaining 80%: gentle decay
        float decay;
        if (progress < 0.2f) {
            // Initial impact phase - full intensity with slight buildup
            decay = 0.8f + 0.2f * (progress / 0.2f);
        } else {
            // Decay phase - slower falloff
            float decayProgress = (progress - 0.2f) / 0.8f;
            decay = (float) Math.pow(1.0f - decayProgress, 1.5f); // Gentler than exponential
        }

        // Time value for noise sampling
        float time = (elapsed + tickDelta) * 0.05f * frequency;

        // Use sin/cos with offset for pseudo-noise (organic feel)
        // Lower multipliers on high-frequency components for less jitter
        float noiseX = (float) (Math.sin(time * 1.3 + noiseSeed) * 0.7 +
                Math.sin(time * 2.1 + noiseSeed * 0.7) * 0.2 +
                Math.sin(time * 3.7 + noiseSeed * 1.3) * 0.1);

        float noiseZ = (float) (Math.cos(time * 1.5 + noiseSeed * 0.5) * 0.7 +
                Math.cos(time * 2.3 + noiseSeed * 1.1) * 0.2 +
                Math.cos(time * 3.9 + noiseSeed * 0.3) * 0.1);

        // Add initial directional punch (downward then up)
        float punchProgress = Math.min(progress * 5.0f, 1.0f); // First 20% of duration
        float punch = 0;
        if (progress < 0.2f) {
            // Quick down-up motion
            punch = (float) Math.sin(punchProgress * Math.PI) * 0.5f;
        }

        float pitchShake = (noiseX + punch) * intensity * decay;
        float rollShake = noiseZ * intensity * decay * 0.6f; // Less roll than pitch

        return new float[] { pitchShake, rollShake };
    }

    /**
     * Check if a shake is currently active.
     */
    public static boolean isShaking() {
        return elapsed < duration && intensity > 0;
    }

    /**
     * Reset all shake state.
     */
    public static void reset() {
        intensity = 0;
        duration = 0;
        elapsed = 0;
    }
}
