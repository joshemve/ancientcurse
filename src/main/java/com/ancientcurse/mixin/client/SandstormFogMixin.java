package com.ancientcurse.mixin.client;

import com.ancientcurse.client.SandstormClientHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to apply dense sandstorm fog that limits visibility.
 * Features slow, organic fog distance changes and sand-colored fog tint.
 */
@Mixin(BackgroundRenderer.class)
public class SandstormFogMixin {

    // Dynamic fog pulsing state - very slow transitions
    @Unique
    private static float fogPulsePhase = 0f;
    @Unique
    private static float fogPulseTarget = 0f;
    @Unique
    private static float currentFogPulse = 0f;
    @Unique
    private static int pulseChangeTimer = 0;
    @Unique
    private static float smoothedFogEnd = 0f;
    @Unique
    private static float smoothedFogStart = 0f;
    @Unique
    private static boolean initialized = false;

    // Sand fog color (bright warm sand - lighter tan)
    @Unique
    private static final float SAND_FOG_R = 0.92f;
    @Unique
    private static final float SAND_FOG_G = 0.82f;
    @Unique
    private static final float SAND_FOG_B = 0.65f;

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void applySandstormFogDistance(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (SandstormClientHandler.isRenderingSandstorm()) {
            float intensity = SandstormClientHandler.getIntensity();

            // Update dynamic fog pulse (very slow)
            updateFogPulse(intensity);

            // Base density from intensity (cubic curve)
            float baseDensity = intensity * intensity * intensity;

            // Apply dynamic pulse modifier - subtle variation
            float pulsedDensity = baseDensity * (1.0f + currentFogPulse * 0.3f);
            pulsedDensity = Math.max(0f, Math.min(1f, pulsedDensity));

            // Fog distance ranges
            // At full density: close (5-8 blocks)
            // At low density: moderate visibility (20-30 blocks)
            float minEnd = 5.0f + (1.0f - pulsedDensity) * 15.0f;
            float maxEnd = viewDistance * 0.5f;

            // Calculate fog end distance
            float targetEnd = maxEnd - (pulsedDensity * (maxEnd - minEnd));
            targetEnd = Math.max(minEnd, targetEnd);

            // Fog start - closer start = more dramatic
            float startRatio = 0.15f + pulsedDensity * 0.2f;
            float targetStart = targetEnd * startRatio;

            // Add subtle gust influence
            float gust = SandstormClientHandler.getGustStrength();
            if (gust > 0.3f) {
                float gustEffect = gust * 0.2f;
                targetEnd *= (1.0f - gustEffect);
            }

            // Mega gusts dramatically reduce visibility
            if (SandstormClientHandler.isMegaGust()) {
                float megaStrength = SandstormClientHandler.getMegaGustStrength();
                // Reduce visibility by up to 60% during mega gusts
                targetEnd *= (1.0f - megaStrength * 0.3f);
                targetStart *= 0.5f; // Fog starts much closer
            }

            // Ensure minimum visibility
            targetEnd = Math.max(4.0f, targetEnd);
            targetStart = Math.max(0f, Math.min(targetStart, targetEnd - 2.0f));

            // Initialize smoothed values on first run
            if (!initialized) {
                smoothedFogEnd = targetEnd;
                smoothedFogStart = targetStart;
                initialized = true;
            }

            // VERY slow interpolation for natural feel (0.005 = very gradual)
            smoothedFogEnd += (targetEnd - smoothedFogEnd) * 0.008f;
            smoothedFogStart += (targetStart - smoothedFogStart) * 0.008f;

            float end = smoothedFogEnd;
            float start = smoothedFogStart;

            if (fogType == BackgroundRenderer.FogType.FOG_SKY) {
                // Sky fog - can't see sky well in storm
                start = 0.0f;
                end = Math.max(2.0f, smoothedFogEnd * 0.25f);
            }

            RenderSystem.setShaderFogStart(start);
            RenderSystem.setShaderFogEnd(end);

            // Apply sand-colored fog tint based on intensity
            // Blend from normal fog color toward sand color
            float blendFactor = Math.min(1.0f, intensity * 1.5f);
            RenderSystem.setShaderFogColor(
                SAND_FOG_R * blendFactor + (1.0f - blendFactor) * 0.7f,
                SAND_FOG_G * blendFactor + (1.0f - blendFactor) * 0.8f,
                SAND_FOG_B * blendFactor + (1.0f - blendFactor) * 0.9f,
                1.0f
            );
        } else {
            // Reset when sandstorm ends
            initialized = false;
        }
    }

    /**
     * Updates the fog pulse system for dynamic visibility changes.
     * Very slow, organic transitions.
     */
    @Unique
    private static void updateFogPulse(float intensity) {
        // Very slow base oscillation
        fogPulsePhase += 0.003f;

        // Periodically pick new random pulse targets - longer intervals
        pulseChangeTimer--;
        if (pulseChangeTimer <= 0) {
            // Subtle target variation: -0.3 to +0.5
            float baseTarget = (float)(Math.random() * 0.8f - 0.3f);
            fogPulseTarget = baseTarget * intensity;
            // Long intervals: 8-20 seconds (160-400 ticks equivalent)
            pulseChangeTimer = 160 + (int)(Math.random() * 240);
        }

        // Gentle sine wave combined with random target
        float sineComponent = (float)Math.sin(fogPulsePhase) * 0.15f * intensity;
        float combinedTarget = sineComponent + fogPulseTarget * 0.5f;

        // Very slow blend toward target
        currentFogPulse += (combinedTarget - currentFogPulse) * 0.01f;

        // Clamp to subtle range
        currentFogPulse = Math.max(-0.3f, Math.min(0.5f, currentFogPulse));
    }
}
