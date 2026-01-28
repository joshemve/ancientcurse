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
 * Renders Blood Water plague curse visual overlay effects:
 * - Red pulsing vignette when cursed by blood water
 * - Damage flash when taking damage from the curse
 * - Curse timer showing remaining curse duration
 * - Blood drip effect on screen edges
 */
@Environment(EnvType.CLIENT)
public class BloodWaterOverlayRenderer {

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

        AncientCurse.LOGGER.info("Blood Water overlay renderer registered");
    }

    private static void render(DrawContext drawContext, MinecraftClient client, float tickDelta) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Render curse overlay when player is cursed
        if (BloodWaterClientHandler.isCursed() && BloodWaterClientHandler.shouldShowCurseOverlay()) {
            renderCurseOverlay(drawContext, screenWidth, screenHeight);
            renderCurseVignette(drawContext, screenWidth, screenHeight);
        }

        // Render damage flash
        if (BloodWaterClientHandler.isDamageFlashing()) {
            renderDamageFlash(drawContext, screenWidth, screenHeight);
        }

        // Render curse timer
        if (BloodWaterClientHandler.isCursed() && BloodWaterClientHandler.shouldShowCurseTimer()) {
            renderCurseTimer(drawContext, client, screenWidth, screenHeight);
        }
    }

    /**
     * Render pulsing red overlay when cursed
     */
    private static void renderCurseOverlay(DrawContext drawContext, int width, int height) {
        int color = BloodWaterClientHandler.getCurseOverlayColor();
        if ((color >> 24 & 0xFF) == 0) return; // Skip if fully transparent

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw full screen red tint
        drawContext.fill(0, 0, width, height, color);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Render blood-red vignette effect around edges when cursed
     */
    private static void renderCurseVignette(DrawContext drawContext, int width, int height) {
        if (!BloodWaterClientHandler.isCursed()) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);

        // Blood red vignette
        float r = 0.5f;
        float g = 0.05f;
        float b = 0.05f;

        // Pulsing alpha
        MinecraftClient client = MinecraftClient.getInstance();
        float pulse = 0.6f + 0.4f * (float)Math.sin(
            (client.world != null ? client.world.getTime() : 0) * 0.08f);

        float lingerFactor = Math.min(1.0f, BloodWaterClientHandler.getCurseLingerTicks() / 60.0f);
        float alpha = 0.6f * pulse * lingerFactor * BloodWaterClientHandler.getOverlayAlphaMultiplier();

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
     * Render damage flash effect
     */
    private static void renderDamageFlash(DrawContext drawContext, int width, int height) {
        int color = BloodWaterClientHandler.getDamageFlashColor();
        if ((color >> 24 & 0xFF) == 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw full screen red flash
        drawContext.fill(0, 0, width, height, color);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Render curse timer in corner
     */
    private static void renderCurseTimer(DrawContext drawContext, MinecraftClient client, int width, int height) {
        String curseText = "Blood Curse: " + BloodWaterClientHandler.getFormattedCurseTime();

        int textWidth = client.textRenderer.getWidth(curseText);
        int x = 5;
        int y = 5;

        // Offset if other timers are showing
        if (DarknessClientHandler.isDarknessActive() && DarknessClientHandler.shouldShowTimer()) {
            y += 15;
        }
        if (FamineClientHandler.isFamineActive() && FamineClientHandler.shouldShowTimer()) {
            y += 15;
        }
        if (SandstormClientHandler.isRenderingSandstorm()) {
            y += 15;
        }

        // Background
        float lingerFactor = Math.min(1.0f, BloodWaterClientHandler.getCurseLingerTicks() / 60.0f);
        int bgAlpha = (int)(180 * lingerFactor);
        int bgColor = (bgAlpha << 24) | 0x300505; // Dark red

        drawContext.fill(x - 2, y - 2, x + textWidth + 2, y + 10, bgColor);

        // Text - blood red
        int textColor = 0xFFCC2222;
        drawContext.drawText(client.textRenderer, curseText, x, y, textColor, true);
    }
}
