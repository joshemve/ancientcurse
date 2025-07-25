package com.ancientcurse.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

/**
 * Renders curse zone boundaries in the world
 */
@Environment(EnvType.CLIENT)
public class CurseZoneRenderer {
    
    /**
     * Renders all active curse zones
     */
    public static void renderZones(MatrixStack matrices, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        
        // Only render if player is op
        if (!client.player.hasPermissionLevel(2)) {
            return;
        }
        
        // Check if zone borders should be shown
        if (!CurseZoneClientSettings.shouldShowAllZoneBorders()) {
            return;
        }
        
        // Get immediate vertex consumer provider
        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Render all cached zones
        for (Map.Entry<String, CurseZoneClientCache.ZoneAreaData> entry : CurseZoneClientCache.getAllAreas().entrySet()) {
            CurseZoneClientCache.ZoneAreaData area = entry.getValue();
            renderZoneArea(matrices, camera, vertexConsumers, area);
        }
        
        // Draw the buffer
        vertexConsumers.draw();
    }
    
    /**
     * Renders a single curse zone area
     */
    private static void renderZoneArea(MatrixStack matrices, Camera camera, 
                                     VertexConsumerProvider vertexConsumers,
                                     CurseZoneClientCache.ZoneAreaData area) {
        Vec3d cameraPos = camera.getPos();
        
        matrices.push();
        
        double minX = area.min.getX() - cameraPos.x;
        double minY = area.min.getY() - cameraPos.y;
        double minZ = area.min.getZ() - cameraPos.z;
        double maxX = area.max.getX() + 1 - cameraPos.x;
        double maxY = area.max.getY() + 1 - cameraPos.y;
        double maxZ = area.max.getZ() + 1 - cameraPos.z;
        
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        
        // Choose color based on zone properties
        float r = 1.0f;
        float g = 0.0f;
        float b = 0.0f;
        float a = 0.8f;
        
        if (!area.effectsEnabled) {
            // Gray for disabled zones
            r = 0.5f;
            g = 0.5f;
            b = 0.5f;
            a = 0.5f;
        } else if (area.khamsinLevel > 0) {
            // Purple for khamsin zones
            r = 0.5f;
            g = 0.0f;
            b = 1.0f;
        } else if (area.ankhDrainRate > 0) {
            // Orange for ankh drain zones
            r = 1.0f;
            g = 0.5f;
            b = 0.0f;
        }
        
        WorldRenderer.drawBox(matrices, vertexConsumer, box, r, g, b, a);
        
        matrices.pop();
    }
}