package com.ancientcurse.client;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.item.armor.JackelBindsItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Client-side renderer for the Jackel Binds HUD icon.
 * Displays an icon when the player is wearing Jackel Binds.
 */
@Environment(EnvType.CLIENT)
public class JackelBindsHudRenderer {

    private static final Identifier BINDS_ICON = new Identifier(AncientCurse.MOD_ID, "textures/gui/jackel_binds_icon.png");
    private static final int ICON_SIZE = 18;

    /**
     * Register the Jackel Binds HUD renderer.
     */
    public static void register() {
        AncientCurse.LOGGER.info("Registering Jackel Binds HUD Renderer");

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;

            if (player != null && !client.options.hudHidden && isPlayerBound(player)) {
                renderBindsIcon(drawContext, client);
            }
        });
    }

    /**
     * Check if the player is wearing Jackel Binds
     */
    private static boolean isPlayerBound(PlayerEntity player) {
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        return chestpiece.getItem() instanceof JackelBindsItem;
    }

    /**
     * Render the Jackel Binds icon on the HUD.
     */
    private static void renderBindsIcon(DrawContext drawContext, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position: right side of the screen, vertically centered
        int x = screenWidth - ICON_SIZE - 8; // 8 pixels from right edge
        int y = (screenHeight / 2) - (ICON_SIZE / 2); // Vertically centered

        // Draw the icon texture (18x18 texture)
        drawContext.drawTexture(BINDS_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}
