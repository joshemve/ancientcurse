package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.player.ThirstDataManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Client-side renderer for the Thirst bar HUD element.
 * Displays water skin icons similar to the hunger bar, positioned above it.
 *
 * The thirst bar appears above the hunger bar on the right side of the screen.
 * Each icon represents 2 thirst points (like hunger), so 10 icons for full thirst.
 */
@Environment(EnvType.CLIENT)
public class ThirstHudRenderer {

    // Texture containing our thirst icons (3 water skins: empty, half, full)
    // Texture is 54x18 with 18x18 icons arranged horizontally: [empty][half][full]
    private static final Identifier THIRST_ICONS = new Identifier(AncientCurse.MOD_ID, "textures/gui/water_skin_icon.png");

    // Icon dimensions in the texture (18x18 pixels for 2x quality)
    private static final int TEXTURE_ICON_SIZE = 18;

    // Rendered icon size on screen (scaled down to match vanilla HUD proportions)
    private static final int RENDER_ICON_SIZE = 9;

    // Texture dimensions
    private static final int TEXTURE_WIDTH = 54;
    private static final int TEXTURE_HEIGHT = 18;

    // Texture UV coordinates for each icon state (in a 54x18 texture)
    // Each icon is 18x18, arranged horizontally: [empty][half][full]
    private static final int EMPTY_U = 0;
    private static final int HALF_U = 18;
    private static final int FULL_U = 36;

    // Bobbing animation state
    private static final Random random = new Random();
    private static int lastTickCount = 0;
    private static final int[] bobOffsets = new int[10]; // Y offset for each icon

    /**
     * Register the Thirst HUD renderer.
     */
    public static void register() {
        AncientCurse.LOGGER.info("Registering Thirst HUD Renderer");

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;

            if (player != null && client.interactionManager != null && !client.options.hudHidden) {
                // Don't render in creative or spectator mode
                if (!player.isCreative() && !player.isSpectator()) {
                    renderThirstBar(drawContext, player);
                }
            }
        });
    }

    /**
     * Render the Thirst bar on the HUD.
     * Positioned above the hunger bar on the right side.
     *
     * @param drawContext The draw context
     * @param player The player entity
     */
    private static void renderThirstBar(DrawContext drawContext, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Get thirst value from client cache
        int thirst = ThirstDataManager.getClientThirstValue();
        float saturation = ThirstDataManager.getClientThirstSaturation();

        // Calculate position (mirrors hunger bar position, but above it)
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Hunger bar is at y = screenHeight - 39, we go 10 pixels above it
        int baseX = screenWidth / 2 + 91; // Right side, same as hunger bar
        int baseY = screenHeight - 39 - 10; // 10 pixels above hunger bar

        // Update bobbing offsets when thirst is low (similar to hunger bar)
        int tickCount = client.inGameHud.getTicks();
        updateBobbing(thirst, tickCount);

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(0.0, 0.0, 50.0); // Render above other HUD elements

        // Draw 10 water skin icons (drains left to right - leftmost empties first)
        for (int i = 0; i < 10; i++) {
            int x = baseX - (i + 1) * 8; // 8 pixel spacing (slight overlap for compact look)
            int y = baseY;

            // Apply bobbing offset when thirst is low
            if (thirst <= 6) {
                y += bobOffsets[i];
            }

            // Calculate thirst points for this icon position
            // i=0 is rightmost (represents points 1-2), i=9 is leftmost (represents points 19-20)
            // This makes it drain from left to right (leftmost icons empty first)
            int minThirstForFull = (i * 2) + 2; // Need 2, 4, 6... 20 for full
            int minThirstForHalf = (i * 2) + 1; // Need 1, 3, 5... 19 for half

            // Determine icon state
            int iconU;
            if (thirst >= minThirstForFull) {
                iconU = FULL_U;
            } else if (thirst >= minThirstForHalf) {
                iconU = HALF_U;
            } else {
                iconU = EMPTY_U;
            }

            // Draw the water skin icon (18x18 texture scaled to 9x9 on screen)
            drawContext.drawTexture(
                THIRST_ICONS,
                x, y,                           // Screen position
                RENDER_ICON_SIZE, RENDER_ICON_SIZE, // Rendered size on screen
                iconU, 0,                       // UV coordinates in texture
                TEXTURE_ICON_SIZE, TEXTURE_ICON_SIZE, // Size to sample from texture
                TEXTURE_WIDTH, TEXTURE_HEIGHT   // Total texture dimensions
            );
        }

        matrices.pop();
    }

    /**
     * Update bobbing offsets for each icon when thirst is low.
     * Similar to how vanilla hunger bar bobs when food is low.
     */
    private static void updateBobbing(int thirst, int tickCount) {
        // Only update bobbing on new ticks
        if (tickCount != lastTickCount) {
            lastTickCount = tickCount;

            // When thirst is low, randomly bob icons
            if (thirst <= 6) {
                for (int i = 0; i < 10; i++) {
                    // More intense bobbing as thirst gets lower
                    // At thirst 6: occasional bob, at thirst 1-2: frequent bob
                    float bobChance = (7 - thirst) / 10.0f; // 0.1 at 6, 0.6 at 1

                    if (random.nextFloat() < bobChance) {
                        // Bob up or down by 1-2 pixels
                        bobOffsets[i] = random.nextInt(3) - 1; // -1, 0, or 1
                    } else {
                        // Gradually return to normal
                        if (bobOffsets[i] > 0) bobOffsets[i]--;
                        else if (bobOffsets[i] < 0) bobOffsets[i]++;
                    }
                }
            } else {
                // Reset all offsets when thirst is healthy
                for (int i = 0; i < 10; i++) {
                    bobOffsets[i] = 0;
                }
            }
        }
    }
}
