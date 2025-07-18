package com.ancientcurse.client.render;

import com.ancientcurse.item.CurseZoneAdminWand;
import com.ancientcurse.util.CurseZoneData;
import com.ancientcurse.util.CurseZoneManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import org.joml.Matrix4f;

import java.util.Map;

public class CurseZoneRenderer {
    private static final int CHUNK_SIZE = 16;
    private static final float BOX_HEIGHT = 256.0f;
    
    public static void renderZones(MatrixStack matrices, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        
        if (player == null || !isHoldingAdminWand(player)) {
            return;
        }
        
        World world = client.world;
        if (!(world instanceof ServerWorld)) {
            // For client rendering, we'd need to sync data from server
            return;
        }
        
        Vec3d cameraPos = camera.getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        // Get all zones from the manager
        CurseZoneManager manager = CurseZoneManager.get((ServerWorld) world);
        Map<ChunkPos, CurseZoneData> zones = manager.getAllZones();
        
        // Render each zone
        for (Map.Entry<ChunkPos, CurseZoneData> entry : zones.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            CurseZoneData zone = entry.getValue();
            
            if (zone.isEffectsEnabled()) {
                renderChunkBoundary(matrices, buffer, tessellator, chunkPos, zone);
            }
        }
        
        // Render holographic displays
        renderHolographicDisplays(matrices, zones, cameraPos);
        
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        
        matrices.pop();
    }
    
    private static void renderChunkBoundary(MatrixStack matrices, BufferBuilder buffer, 
                                          Tessellator tessellator, ChunkPos chunkPos, CurseZoneData zone) {
        float severity = zone.getSeverity();
        
        // Color based on severity: Green -> Yellow -> Orange -> Red -> Purple
        float r, g, b;
        if (severity < 0.25f) {
            // Green to Yellow
            float t = severity * 4;
            r = t;
            g = 1.0f;
            b = 0.0f;
        } else if (severity < 0.5f) {
            // Yellow to Orange
            float t = (severity - 0.25f) * 4;
            r = 1.0f;
            g = 1.0f - t * 0.5f;
            b = 0.0f;
        } else if (severity < 0.75f) {
            // Orange to Red
            float t = (severity - 0.5f) * 4;
            r = 1.0f;
            g = 0.5f - t * 0.5f;
            b = 0.0f;
        } else {
            // Red to Purple
            float t = (severity - 0.75f) * 4;
            r = 1.0f;
            g = 0.0f;
            b = t * 0.5f;
        }
        
        float alpha = 0.3f;
        
        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        int maxX = minX + CHUNK_SIZE;
        int maxZ = minZ + CHUNK_SIZE;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        // Render vertical walls
        // North wall
        buffer.vertex(matrix, minX, 0, minZ).color(r, g, b, alpha).next();
        buffer.vertex(matrix, maxX, 0, minZ).color(r, g, b, alpha).next();
        buffer.vertex(matrix, maxX, BOX_HEIGHT, minZ).color(r, g, b, alpha * 0.5f).next();
        buffer.vertex(matrix, minX, BOX_HEIGHT, minZ).color(r, g, b, alpha * 0.5f).next();
        
        // South wall
        buffer.vertex(matrix, minX, 0, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(matrix, minX, BOX_HEIGHT, maxZ).color(r, g, b, alpha * 0.5f).next();
        buffer.vertex(matrix, maxX, BOX_HEIGHT, maxZ).color(r, g, b, alpha * 0.5f).next();
        buffer.vertex(matrix, maxX, 0, maxZ).color(r, g, b, alpha).next();
        
        // East wall
        buffer.vertex(matrix, maxX, 0, minZ).color(r, g, b, alpha).next();
        buffer.vertex(matrix, maxX, 0, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(matrix, maxX, BOX_HEIGHT, maxZ).color(r, g, b, alpha * 0.5f).next();
        buffer.vertex(matrix, maxX, BOX_HEIGHT, minZ).color(r, g, b, alpha * 0.5f).next();
        
        // West wall
        buffer.vertex(matrix, minX, 0, minZ).color(r, g, b, alpha).next();
        buffer.vertex(matrix, minX, BOX_HEIGHT, minZ).color(r, g, b, alpha * 0.5f).next();
        buffer.vertex(matrix, minX, BOX_HEIGHT, maxZ).color(r, g, b, alpha * 0.5f).next();
        buffer.vertex(matrix, minX, 0, maxZ).color(r, g, b, alpha).next();
        
        tessellator.draw();
    }
    
    private static void renderHolographicDisplays(MatrixStack matrices, Map<ChunkPos, CurseZoneData> zones, Vec3d cameraPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        for (Map.Entry<ChunkPos, CurseZoneData> entry : zones.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            CurseZoneData zone = entry.getValue();
            
            if (!zone.isEffectsEnabled()) continue;
            
            // Calculate display position (center of chunk, 10 blocks above ground)
            double x = chunkPos.getCenterX();
            double y = 80; // Adjust based on your world
            double z = chunkPos.getCenterZ();
            
            // Only render if close enough
            double distance = Math.sqrt(
                Math.pow(x - cameraPos.x, 2) + 
                Math.pow(z - cameraPos.z, 2)
            );
            if (distance > 64) continue;
            
            matrices.push();
            matrices.translate(x, y, z);
            
            // Face the camera
            matrices.multiply(client.getEntityRenderDispatcher().getRotation());
            matrices.scale(-0.025f, -0.025f, 0.025f);
            
            // Render background
            int backgroundColor = 0x80000000;
            VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
            
            String line1 = zone.getZoneName();
            String line2 = "Khamsin: " + zone.getKhamsinLevel() + "/sec";
            String line3 = "Ankh Drain: " + zone.getAnkhDrainRate() + "/sec";
            
            int maxWidth = Math.max(
                textRenderer.getWidth(line1),
                Math.max(textRenderer.getWidth(line2), textRenderer.getWidth(line3))
            );
            
            // Draw text
            textRenderer.draw(line1, -maxWidth / 2, -20, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, backgroundColor, 15728880);
            textRenderer.draw(line2, -maxWidth / 2, -10, 0xFFFF00, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, backgroundColor, 15728880);
            textRenderer.draw(line3, -maxWidth / 2, 0, 0xFF8800, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, backgroundColor, 15728880);
            
            immediate.draw();
            
            matrices.pop();
        }
    }
    
    private static boolean isHoldingAdminWand(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        return mainHand.getItem() instanceof CurseZoneAdminWand || 
               offHand.getItem() instanceof CurseZoneAdminWand;
    }
}