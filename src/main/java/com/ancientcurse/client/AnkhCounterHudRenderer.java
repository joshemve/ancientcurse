package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.player.AnkhDataManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side renderer for the Ankh counter HUD element.
 * Displays a counter centered between the hearts and health bar.
 */
@Environment(EnvType.CLIENT)
public class AnkhCounterHudRenderer {
    
    private static final int TEXT_COLOR = 0xFFD700; // Gold color
    private static final int TEXT_COLOR_LOW = 0xFF0000; // Red color when ankh is low
    private static final int LOW_THRESHOLD = 25; // Value below which the counter turns red
    
    /**
     * Register the Ankh counter HUD renderer.
     */
    public static void register() {
        AncientCurse.LOGGER.info("Registering Ankh Counter HUD Renderer");
        
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;
            
            if (player != null && client.interactionManager != null && !client.options.hudHidden) {
                renderAnkhCounter(drawContext, player);
            }
        });
    }
    
    /**
     * Render the Ankh counter on the HUD.
     * 
     * @param drawContext The draw context
     * @param player The player entity
     */
    private static void renderAnkhCounter(DrawContext drawContext, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        // Get the client-side ankh value for display
        int ankhValue = AnkhDataManager.getClientAnkhValue();
        
        // Format the text to display (smaller size, no bold formatting)
        String displayText = String.format("%d", ankhValue);
        
        // Calculate position (centered between hearts and health bar)
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Position: centered horizontally, positioned lower to avoid item name text
        int x = screenWidth / 2;
        int y = screenHeight - 46; // Positioned lower to avoid item name text
        
        // Determine text color based on ankh value
        int textColor = ankhValue <= LOW_THRESHOLD ? TEXT_COLOR_LOW : TEXT_COLOR;
        
        // Draw the text centered - renders at default HUD layer for proper chat transparency
        drawContext.drawCenteredTextWithShadow(
            textRenderer, 
            Text.literal(displayText), 
            x, 
            y, 
            textColor
        );
    }
}
