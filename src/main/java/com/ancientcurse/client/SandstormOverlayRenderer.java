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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Renders sandstorm screen effects:
 * - Corner vignette that intensifies with storm strength
 * - Sand particles that stick to the screen and slide off
 */
@Environment(EnvType.CLIENT)
public class SandstormOverlayRenderer {

    private static final Identifier VIGNETTE_TEXTURE = new Identifier("minecraft", "textures/misc/vignette.png");
    private static final Identifier SAND_PARTICLE_TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/gui/sand_speck.png");

    // Screen sand particles
    private static final List<ScreenSandParticle> screenParticles = new ArrayList<>();
    private static final int MAX_SCREEN_PARTICLES = 30;
    private static final Random random = new Random();
    private static int spawnCooldown = 0;

    /**
     * Register the HUD overlay renderer
     */
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (SandstormClientHandler.isRenderingSandstorm()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && !client.options.hudHidden) {
                    render(drawContext, client, tickDelta);
                }
            }
        });

        AncientCurse.LOGGER.info("Sandstorm overlay renderer registered");
    }

    /**
     * Called every client tick to update screen particles
     */
    public static void tick() {
        if (!SandstormClientHandler.isRenderingSandstorm()) {
            // Clear particles when storm ends
            screenParticles.clear();
            return;
        }

        /* Screen particles disabled per user request
        float intensity = SandstormClientHandler.getIntensity();

        // Spawn new particles
        spawnCooldown--;
        if (spawnCooldown <= 0 && screenParticles.size() < MAX_SCREEN_PARTICLES) {
            // Higher intensity = more frequent spawns
            if (random.nextFloat() < intensity * 0.4f) {
                spawnScreenParticle();
            }
            spawnCooldown = 2 + random.nextInt(5);
        }

        // Update existing particles
        Iterator<ScreenSandParticle> iter = screenParticles.iterator();
        while (iter.hasNext()) {
            ScreenSandParticle particle = iter.next();
            particle.tick();
            if (particle.isDead()) {
                iter.remove();
            }
        }
        */
    }

    private static void spawnScreenParticle() {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Get wind direction for spawn position
        float windX = SandstormClientHandler.getWindX();
        float windZ = SandstormClientHandler.getWindZ();

        // Spawn from the upwind edge of screen
        float x, y;
        float velX, velY;

        // Convert 3D wind to 2D screen movement (simplified)
        float screenWindX = windX * 0.5f + windZ * 0.3f; // Blend for diagonal effect

        if (screenWindX > 0) {
            // Wind blowing right - spawn on left
            x = -10;
            velX = 2f + random.nextFloat() * 3f;
        } else {
            // Wind blowing left - spawn on right
            x = screenWidth + 10;
            velX = -(2f + random.nextFloat() * 3f);
        }

        y = random.nextFloat() * screenHeight * 0.7f; // Upper portion of screen
        velY = 0.5f + random.nextFloat() * 1.5f; // Slight downward drift

        // Add gust influence
        float gust = SandstormClientHandler.getGustStrength();
        velX *= (1 + gust * 0.5f);

        float size = 4 + random.nextFloat() * 8;
        int lifetime = 60 + random.nextInt(80);
        float alpha = 0.3f + random.nextFloat() * 0.4f;

        screenParticles.add(new ScreenSandParticle(x, y, velX, velY, size, lifetime, alpha));
    }

    private static void render(DrawContext drawContext, MinecraftClient client, float tickDelta) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float intensity = SandstormClientHandler.getIntensity();

        // Render vignette
        renderVignette(drawContext, screenWidth, screenHeight, intensity);

        // Render screen particles - DISABLED
        // renderScreenParticles(drawContext, tickDelta);
    }

    /**
     * Render sandstorm vignette effect - darkens/tints corners
     */
    private static void renderVignette(DrawContext drawContext, int width, int height, float intensity) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, VIGNETTE_TEXTURE);

        // Sand-tinted vignette color (warm tan/brown)
        float r = 0.6f;
        float g = 0.45f;
        float b = 0.3f;

        // Alpha scales with intensity - more intense = darker corners
        float alpha = intensity * 0.85f;

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
     * Render sand particles stuck to the screen
     */
    private static void renderScreenParticles(DrawContext drawContext, float tickDelta) {
        if (screenParticles.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (ScreenSandParticle particle : screenParticles) {
            particle.render(drawContext, tickDelta);
        }

        RenderSystem.disableBlend();
    }

    /**
     * Represents a sand particle on the screen
     */
    private static class ScreenSandParticle {
        private float x, y;
        private float prevX, prevY;
        private float velX, velY;
        private final float size;
        private int age;
        private final int maxAge;
        private final float maxAlpha;
        private float alpha;
        private final float rotation;
        private final float rotationSpeed;
        private boolean stuck; // Whether particle is "stuck" on screen
        private int stuckTime;

        public ScreenSandParticle(float x, float y, float velX, float velY, float size, int maxAge, float alpha) {
            this.x = x;
            this.y = y;
            this.prevX = x;
            this.prevY = y;
            this.velX = velX;
            this.velY = velY;
            this.size = size;
            this.maxAge = maxAge;
            this.maxAlpha = alpha;
            this.alpha = 0;
            this.rotation = random.nextFloat() * 360f;
            this.rotationSpeed = (random.nextFloat() - 0.5f) * 2f;
            this.stuck = false;
            this.stuckTime = 0;
        }

        public void tick() {
            prevX = x;
            prevY = y;
            age++;

            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // Fade in
            if (age < 10) {
                alpha = (age / 10f) * maxAlpha;
            }

            // Check if should stick
            if (!stuck && x > 0 && x < screenWidth && y > 0 && y < screenHeight) {
                if (random.nextFloat() < 0.02f) { // 2% chance per tick to stick
                    stuck = true;
                    stuckTime = 30 + random.nextInt(60); // Stick for 1.5-4.5 seconds
                }
            }

            if (stuck) {
                // Slowly slide down while stuck
                velX *= 0.95f;
                velY = 0.3f + SandstormClientHandler.getGustStrength() * 0.5f;
                stuckTime--;
                if (stuckTime <= 0) {
                    stuck = false;
                    // Resume with wind
                    velX = SandstormClientHandler.getWindX() * 3f;
                }
            } else {
                // Add some turbulence
                velX += (random.nextFloat() - 0.5f) * 0.1f;
                velY += (random.nextFloat() - 0.5f) * 0.05f;

                // Gusts affect screen particles too
                float gust = SandstormClientHandler.getGustStrength();
                if (gust > 0.1f) {
                    velX += SandstormClientHandler.getWindX() * gust * 0.5f;
                }
            }

            x += velX;
            y += velY;

            // Fade out when near edges or at end of life
            if (age > maxAge - 20) {
                alpha = ((maxAge - age) / 20f) * maxAlpha;
            }
            if (x < -20 || x > screenWidth + 20 || y > screenHeight + 20) {
                alpha = 0;
            }
        }

        public boolean isDead() {
            return age >= maxAge || alpha <= 0;
        }

        public void render(DrawContext drawContext, float tickDelta) {
            if (alpha <= 0) return;

            float renderX = prevX + (x - prevX) * tickDelta;
            float renderY = prevY + (y - prevY) * tickDelta;

            // Sand speck color (tan/brown)
            int color = ((int)(alpha * 255) << 24) | 0xD4A574;

            // Draw as a simple filled rectangle (sand speck)
            int halfSize = (int)(size / 2);
            drawContext.fill(
                (int)(renderX - halfSize),
                (int)(renderY - halfSize),
                (int)(renderX + halfSize),
                (int)(renderY + halfSize),
                color
            );
        }
    }
}
