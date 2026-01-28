package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Renders the Darkness plague (9th Plague of Egypt) visual overlay effects:
 * - Deep darkness overlay that covers the screen
 * - Lightning flash effects (brief moments of light piercing the darkness)
 * - Timer display showing remaining darkness duration
 * - Vignette effect for atmospheric depth
 *
 * "Total darkness covered all Egypt for three days. No one could see anyone else
 * or move about for three days." - Exodus 10:22-23
 */
@Environment(EnvType.CLIENT)
public class DarknessOverlayRenderer {

    private static final Identifier VIGNETTE_TEXTURE = new Identifier("minecraft", "textures/misc/vignette.png");

    /**
     * Register the HUD overlay renderer
     */
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !client.options.hudHidden) {
                render(drawContext, client, tickDelta);
            }
        });

        AncientCurse.LOGGER.info("Darkness plague overlay renderer registered");
    }

    private static void render(DrawContext drawContext, MinecraftClient client, float tickDelta) {
        // Check if darkness has any effect
        if (!DarknessClientHandler.isDarknessActive()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Render main darkness overlay
        renderDarknessOverlay(drawContext, screenWidth, screenHeight);

        // Render vignette for extra depth
        renderDarknessVignette(drawContext, screenWidth, screenHeight);

        // Render lightning flash if active
        if (DarknessClientHandler.isFlashing()) {
            renderLightningFlash(drawContext, screenWidth, screenHeight);
        }

        // Render timer if active and config allows
        if (DarknessClientHandler.isActive() && DarknessClientHandler.shouldShowTimer()) {
            renderDarknessTimer(drawContext, client, screenWidth, screenHeight);
        }
    }

    /**
     * Render the main darkness overlay - a dark layer covering the screen
     */
    private static void renderDarknessOverlay(DrawContext drawContext, int width, int height) {
        float effectiveDarkness = DarknessClientHandler.getEffectiveDarkness();
        if (effectiveDarkness <= 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Calculate alpha based on darkness intensity
        float overlayAlpha = DarknessClientHandler.getOverlayAlpha();

        // Dark blue-black color (less intense for better visibility)
        int r = 5;
        int g = 5;
        int b = 15;
        int a = (int)(overlayAlpha * 180); // Reduced from 230 for better visibility

        int color = (a << 24) | (r << 16) | (g << 8) | b;

        // Draw full screen darkness
        drawContext.fill(0, 0, width, height, color);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Render vignette effect for atmospheric depth during darkness
     */
    private static void renderDarknessVignette(DrawContext drawContext, int width, int height) {
        float intensity = DarknessClientHandler.getIntensity();
        if (intensity <= 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);

        // Dark blue-black vignette (softened)
        float r = 0.03f;
        float g = 0.03f;
        float b = 0.08f;

        // Alpha scales with intensity (reduced for better visibility)
        float alpha = intensity * 0.6f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

        buffer.vertex(matrix, 0, height, -90).texture(0, 1).color(r, g, b, alpha).next();
        buffer.vertex(matrix, width, height, -90).texture(1, 1).color(r, g, b, alpha).next();
        buffer.vertex(matrix, width, 0, -90).texture(1, 0).color(r, g, b, alpha).next();
        buffer.vertex(matrix, 0, 0, -90).texture(0, 0).color(r, g, b, alpha).next();

        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Render lightning flash effect - brief moment of light piercing the darkness
     */
    private static void renderLightningFlash(DrawContext drawContext, int width, int height) {
        float flashIntensity = DarknessClientHandler.getFlashIntensity();
        if (flashIntensity <= 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Bright white/yellow flash
        int r = 255;
        int g = 255;
        int b = 240; // Slightly warm white
        int a = (int)(flashIntensity * 180); // Strong but not blinding

        int color = (a << 24) | (r << 16) | (g << 8) | b;

        // Draw full screen flash
        drawContext.fill(0, 0, width, height, color);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Render darkness timer in corner
     */
    private static void renderDarknessTimer(DrawContext drawContext, MinecraftClient client, int width, int height) {
        int remainingDays = DarknessClientHandler.getRemainingDays();
        int remainingSeconds = DarknessClientHandler.getRemainingSeconds();

        // Don't show timer for infinite darkness
        if (remainingSeconds < 0) {
            return;
        }

        String timerText;
        if (remainingDays > 0) {
            // Show days remaining
            timerText = String.format("Darkness: %d day%s", remainingDays, remainingDays != 1 ? "s" : "");
        } else if (remainingSeconds > 0) {
            // Show hours/minutes/seconds for less than a day
            timerText = "Darkness: " + DarknessClientHandler.getFormattedRemainingTime();
        } else {
            timerText = "Darkness lifting...";
        }

        // Draw in top-left corner with background
        int textWidth = client.textRenderer.getWidth(timerText);
        int x = 5;
        int y = 5;

        // Check if other overlays are showing (offset if needed)
        if (FamineClientHandler.isFamineActive() && FamineClientHandler.shouldShowTimer()) {
            y += 15; // Move below famine timer
        }
        if (SandstormClientHandler.isRenderingSandstorm()) {
            y += 15; // Move below sandstorm info if visible
        }

        // Dark background
        float intensity = DarknessClientHandler.getIntensity();
        int bgAlpha = (int)(180 * intensity);
        int bgColor = (bgAlpha << 24) | 0x050510; // Very dark blue

        drawContext.fill(x - 2, y - 2, x + textWidth + 2, y + 10, bgColor);

        // Text - ghostly pale blue
        int textColor = 0xFF8888AA;
        drawContext.drawText(client.textRenderer, timerText, x, y, textColor, true);
    }

    /**
     * Get the darkness factor for external rendering systems (fog, sky, etc.)
     * Returns a value between 0 (no darkness) and 1 (full darkness)
     */
    public static float getDarknessFactor() {
        return DarknessClientHandler.getEffectiveDarkness();
    }

    /**
     * Check if darkness should affect world rendering
     */
    public static boolean shouldAffectWorldRendering() {
        return DarknessClientHandler.isDarknessActive();
    }

    /**
     * Get the modified sky brightness during darkness
     * Returns 0-1 where 0 is completely dark sky
     */
    public static float getSkyBrightness() {
        if (!DarknessClientHandler.isDarknessActive()) return 1.0f;
        return DarknessClientHandler.getAmbientLightLevel();
    }

    /**
     * Get light level modifier for block/sky light
     * Returns multiplier for light calculations
     */
    public static float getLightLevelModifier() {
        if (!DarknessClientHandler.shouldDimLightSources()) return 1.0f;
        return DarknessClientHandler.getLightSourceMultiplier();
    }
}
