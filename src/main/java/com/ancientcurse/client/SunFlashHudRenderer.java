package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Client-side renderer for the sun flash visual effect.
 * Draws a full-screen colored overlay that fades from bright white to golden.
 */
@Environment(EnvType.CLIENT)
public class SunFlashHudRenderer {

    /**
     * Register the sun flash HUD renderer.
     */
    public static void register() {
        AncientCurse.LOGGER.info("Registering Sun Flash HUD Renderer");

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (SunFlashManager.isFlashing()) {
                renderSunFlash(drawContext, tickDelta);
            }
        });
    }

    /**
     * Render the sun flash overlay on the HUD.
     *
     * @param drawContext The draw context
     * @param tickDelta   Partial tick for smooth interpolation
     */
    private static void renderSunFlash(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int color = SunFlashManager.getColor(tickDelta);

        // Only render if alpha is significant
        if ((color >> 24 & 0xFF) < 2) {
            return;
        }

        // Push matrix to render at very high z-level (above everything)
        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(0.0, 0.0, 1000.0); // Very high z to appear above all UI

        // Draw full-screen colored overlay
        drawContext.fill(0, 0, screenWidth, screenHeight, color);

        matrices.pop();
    }
}
