package com.ancientcurse.client.render;

import com.ancientcurse.client.CurseZoneClientCache;
import com.ancientcurse.client.CurseZoneClientSettings;
import com.ancientcurse.util.CurseZoneArea;
import com.ancientcurse.util.WandSelectionManager;
import com.ancientcurse.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders selection boxes for the admin wand
 */
public class WandSelectionRenderer {
    
    public static void renderSelection(MatrixStack matrices, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;
        
        // Check if player is holding the admin wand
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        boolean holdingWand = mainHand.getItem() == ModItems.CURSE_ZONE_ADMIN_WAND || 
                             offHand.getItem() == ModItems.CURSE_ZONE_ADMIN_WAND;
        
        if (!holdingWand) return;
        
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
        
        // Render existing curse zones if enabled
        if (CurseZoneClientSettings.shouldShowAllZoneBorders()) {
            for (CurseZoneArea area : CurseZoneClientCache.getAllAreas()) {
                float severity = area.getSeverity();
                // Color based on severity: green (low) -> yellow (medium) -> red (high)
                float r = severity;
                float g = 1.0f - severity * 0.5f;
                float b = 0.0f;
                
                renderBox(matrices, buffer, tessellator, area.getBoundingBox(), r, g, b, 0.5f);
            }
        }
        
        // Render current selection
        WandSelectionManager.SelectionData selection = WandSelectionManager.getSelection(player);
        if (selection != null) {
            // Render first position with green outline
            if (selection.pos1 != null) {
                renderBlockOutline(matrices, buffer, tessellator, selection.pos1, 0.0f, 1.0f, 0.0f, 0.8f);
            }
            
            // Render second position with blue outline
            if (selection.pos2 != null) {
                renderBlockOutline(matrices, buffer, tessellator, selection.pos2, 0.0f, 0.0f, 1.0f, 0.8f);
            }
            
            // Render the full selection box if both positions are set
            if (selection.pos1 != null && selection.pos2 != null) {
                BlockPos min = WandSelectionManager.getMinPos(selection);
                BlockPos max = WandSelectionManager.getMaxPos(selection);
                Box box = new Box(min.getX(), min.getY(), min.getZ(), 
                                max.getX() + 1, max.getY() + 1, max.getZ() + 1);
                // Red color with higher alpha for visibility
                renderThickBox(matrices, buffer, tessellator, box, 1.0f, 0.0f, 0.0f, 0.8f);
            }
        }
        
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        
        matrices.pop();
    }
    
    private static void renderBlockOutline(MatrixStack matrices, BufferBuilder buffer, Tessellator tessellator, 
                                          BlockPos pos, float r, float g, float b, float a) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), 
                         pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        renderBox(matrices, buffer, tessellator, box, r, g, b, a);
    }
    
    private static void renderBox(MatrixStack matrices, BufferBuilder buffer, Tessellator tessellator, 
                                 Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Draw box edges
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Bottom face edges
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a).next();
        
        // Top face edges
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a).next();
        
        // Vertical edges
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a).next();
        
        tessellator.draw();
    }
    
    private static void renderThickBox(MatrixStack matrices, BufferBuilder buffer, Tessellator tessellator, 
                                      Box box, float r, float g, float b, float a) {
        // Render the box multiple times with slight offsets to create thickness
        for (float offset = -0.025f; offset <= 0.025f; offset += 0.025f) {
            Box offsetBox = box.expand(offset);
            renderBox(matrices, buffer, tessellator, offsetBox, r, g, b, a);
        }
    }
}