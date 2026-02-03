package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.mixin.client.PostEffectProcessorAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public class MirageClientHandler {
    private static boolean active;
    private static float targetIntensity;
    private static float currentIntensity;

    private static PostEffectProcessor mirageProcessor;
    private static int lastWidth;
    private static int lastHeight;
    private static boolean shaderLoadFailed;

    public static void update(boolean serverActive, float serverIntensity) {
        active = serverActive;
        targetIntensity = serverIntensity;
    }

    public static void reset() {
        active = false;
        targetIntensity = 0;
        currentIntensity = 0;
        closeShader();
    }

    public static void tick() {
        // Smooth interpolation for intensity
        if (currentIntensity < targetIntensity) {
            currentIntensity = Math.min(targetIntensity, currentIntensity + 0.005f);
        } else if (currentIntensity > targetIntensity) {
            currentIntensity = Math.max(targetIntensity, currentIntensity - 0.005f);
        }
    }

    public static void applyPostProcessing(float tickDelta) {
        if (currentIntensity < 0.001f) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        // Load shader if needed
        if (mirageProcessor == null && !shaderLoadFailed) {
            loadShader();
        }

        if (mirageProcessor == null) {
            return;
        }

        // Handle window resize
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width != lastWidth || height != lastHeight) {
            mirageProcessor.setupDimensions(width, height);
            lastWidth = width;
            lastHeight = height;
        }

        // Update custom Intensity uniform on all passes
        try {
            for (PostEffectPass pass : ((PostEffectProcessorAccessor) mirageProcessor).getPasses()) {
                var program = pass.getProgram();
                var intensityUniform = program.getUniformByName("Intensity");
                if (intensityUniform != null) {
                    intensityUniform.set(currentIntensity);
                }
            }

            // Apply post-processing
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            mirageProcessor.render(tickDelta);

            // Re-bind the main framebuffer for subsequent rendering
            client.getFramebuffer().beginWrite(false);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Error rendering mirage shader, disabling", e);
            closeShader();
            shaderLoadFailed = true;
        }
    }

    public static void onResized(int width, int height) {
        if (mirageProcessor != null) {
            mirageProcessor.setupDimensions(width, height);
            lastWidth = width;
            lastHeight = height;
        }
    }

    private static void loadShader() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            mirageProcessor = new PostEffectProcessor(
                client.getTextureManager(),
                client.getResourceManager(),
                client.getFramebuffer(),
                new Identifier(AncientCurse.MOD_ID, "shaders/post/mirage.json")
            );
            lastWidth = client.getWindow().getFramebufferWidth();
            lastHeight = client.getWindow().getFramebufferHeight();
            mirageProcessor.setupDimensions(lastWidth, lastHeight);
            AncientCurse.LOGGER.info("Mirage heatwave shader loaded successfully");
        } catch (IOException e) {
            AncientCurse.LOGGER.error("Failed to load mirage shader", e);
            mirageProcessor = null;
            shaderLoadFailed = true;
        }
    }

    private static void closeShader() {
        if (mirageProcessor != null) {
            mirageProcessor.close();
            mirageProcessor = null;
        }
        shaderLoadFailed = false;
        lastWidth = 0;
        lastHeight = 0;
    }

    public static float getIntensity() {
        return currentIntensity;
    }

    public static boolean isRenderingMirage() {
        return currentIntensity > 0.01f;
    }
}
