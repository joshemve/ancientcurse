package com.ancientcurse.client;

import com.ancientcurse.util.WandSelectionManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Renders selection boxes for the Curse Zone Admin Wand
 */
@Environment(EnvType.CLIENT)
public class WandSelectionRenderer {
    
    /**
     * Renders selection boxes for all players holding the wand
     */
    public static void renderSelection(MatrixStack matrices, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        
        // Get immediate vertex consumer provider
        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Render selections for all players in view
        for (PlayerEntity player : client.world.getPlayers()) {
            WandSelectionManager.SelectionData selection = WandSelectionManager.getSelection(player);
            if (selection != null) {
                renderPlayerSelection(matrices, camera, vertexConsumers, selection, player);
            }
        }
        
        // Draw the buffer
        vertexConsumers.draw();
    }
    
    /**
     * Renders the selection for a specific player
     */
    private static void renderPlayerSelection(MatrixStack matrices, Camera camera, 
                                            VertexConsumerProvider vertexConsumers,
                                            WandSelectionManager.SelectionData selection,
                                            PlayerEntity player) {
        Vec3d cameraPos = camera.getPos();
        
        // Render first position
        if (selection.pos1 != null) {
            renderBlockOutline(matrices, vertexConsumers, selection.pos1, cameraPos, 0.0f, 1.0f, 0.0f, 1.0f);
        }
        
        // Render second position
        if (selection.pos2 != null) {
            renderBlockOutline(matrices, vertexConsumers, selection.pos2, cameraPos, 0.0f, 0.0f, 1.0f, 1.0f);
        }
        
        // Render selection box if both positions are set
        if (selection.pos1 != null && selection.pos2 != null) {
            BlockPos min = WandSelectionManager.getMinPos(selection);
            BlockPos max = WandSelectionManager.getMaxPos(selection);
            renderSelectionBox(matrices, vertexConsumers, min, max, cameraPos);
        }
    }
    
    /**
     * Renders an outline around a single block
     */
    private static void renderBlockOutline(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                         BlockPos pos, Vec3d cameraPos,
                                         float r, float g, float b, float a) {
        matrices.push();
        matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
        
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Box box = new Box(0, 0, 0, 1, 1, 1);
        
        WorldRenderer.drawBox(matrices, vertexConsumer, box, r, g, b, a);
        
        matrices.pop();
    }
    
    /**
     * Renders the full selection box between two positions
     */
    private static void renderSelectionBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                         BlockPos min, BlockPos max, Vec3d cameraPos) {
        matrices.push();
        
        double minX = min.getX() - cameraPos.x;
        double minY = min.getY() - cameraPos.y;
        double minZ = min.getZ() - cameraPos.z;
        double maxX = max.getX() + 1 - cameraPos.x;
        double maxY = max.getY() + 1 - cameraPos.y;
        double maxZ = max.getZ() + 1 - cameraPos.z;
        
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        
        // Draw yellow outline for the full selection
        WorldRenderer.drawBox(matrices, vertexConsumer, box, 1.0f, 1.0f, 0.0f, 0.5f);
        
        matrices.pop();
    }
}