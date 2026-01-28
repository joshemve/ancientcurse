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
 * Renders famine visual overlay effects:
 * - Sickly brown/yellow vignette around edges
 * - Subtle desaturation effect (simulated via tint)
 * - Spoilage flash when food rots
 * - Optional timer display
 */
@Environment(EnvType.CLIENT)
public class FamineOverlayRenderer {

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

        AncientCurse.LOGGER.info("Famine overlay renderer registered");
    }

    private static void render(DrawContext drawContext, MinecraftClient client, float tickDelta) {
        // Check if famine has any effect
        if (!FamineClientHandler.isFamineActive()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float intensity = FamineClientHandler.getIntensity();

        // Render famine vignette
        renderFamineVignette(drawContext, screenWidth, screenHeight, intensity);

        // Render spoilage flash
        if (FamineClientHandler.isSpoilageFlashing()) {
            renderSpoilageFlash(drawContext, screenWidth, screenHeight);
        }

        // Render timer if active and config allows
        if (FamineClientHandler.isActive() && FamineClientHandler.shouldShowTimer()) {
            renderFamineTimer(drawContext, client, screenWidth, screenHeight);
        }
    }

    /**
     * Render sickly vignette effect that intensifies with famine
     */
    private static void renderFamineVignette(DrawContext drawContext, int width, int height, float intensity) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);

        // Sickly brownish-yellow tint
        float r = 0.45f;
        float g = 0.35f;
        float b = 0.15f;

        // Alpha scales with intensity and config multiplier
        float alpha = intensity * 0.5f * FamineClientHandler.getVignetteAlphaMultiplier();

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
     * Render flash effect when food spoils
     */
    private static void renderSpoilageFlash(DrawContext drawContext, int width, int height) {
        float alpha = FamineClientHandler.getSpoilageFlashAlpha();
        if (alpha <= 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Greenish rot color flash
        int r = (int)(50 * alpha);
        int g = (int)(80 * alpha);
        int b = (int)(30 * alpha);
        int a = (int)(100 * alpha);
        int color = (a << 24) | (r << 16) | (g << 8) | b;

        // Draw full screen flash
        drawContext.fill(0, 0, width, height, color);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Render famine timer in corner
     */
    private static void renderFamineTimer(DrawContext drawContext, MinecraftClient client, int width, int height) {
        int remainingSeconds = FamineClientHandler.getRemainingSeconds();

        // Don't show timer for infinite famine
        if (remainingSeconds < 0) {
            return;
        }

        String timerText;
        if (remainingSeconds > 0) {
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            if (minutes > 0) {
                timerText = String.format("Famine: %d:%02d", minutes, seconds);
            } else {
                timerText = String.format("Famine: %ds", seconds);
            }
        } else {
            timerText = "Famine ending...";
        }

        // Draw in top-left corner with background
        int textWidth = client.textRenderer.getWidth(timerText);
        int x = 5;
        int y = 5;

        // Background
        float intensity = FamineClientHandler.getIntensity();
        int bgAlpha = (int)(150 * intensity);
        int bgColor = (bgAlpha << 24) | 0x2A1A0A; // Dark brown

        drawContext.fill(x - 2, y - 2, x + textWidth + 2, y + 10, bgColor);

        // Text
        int textColor = 0xFFAA6600; // Orange-brown
        drawContext.drawText(client.textRenderer, timerText, x, y, textColor, true);
    }

    /**
     * Render spoilage indicator on a specific hotbar slot
     * This is called from item rendering mixins if needed
     */
    public static void renderSpoilageIndicator(DrawContext drawContext, int x, int y, float progress) {
        if (progress <= 0) return;

        // Draw a small rot indicator
        int indicatorHeight = (int)(16 * progress);
        int alpha = (int)(150 * progress);
        int color = (alpha << 24) | 0x4A3A2A; // Brown overlay

        // Draw from bottom up
        drawContext.fill(x, y + 16 - indicatorHeight, x + 16, y + 16, color);
    }
}
