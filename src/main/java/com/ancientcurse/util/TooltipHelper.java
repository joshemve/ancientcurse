package com.ancientcurse.util;

import com.ancientcurse.AncientCurse;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Helper class for adding tooltips to items and blocks from the Ancient Curse mod.
 */
public class TooltipHelper {
    
    /**
     * Register the tooltip callback to add "Ancient Curse" to all mod items.
     * This should be called during client initialization.
     */
    public static void registerTooltipCallback() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            // Get the item's registry name
            Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
            
            // Check if the item is from our mod
            if (id != null && id.getNamespace().equals(AncientCurse.MOD_ID)) {
                // Check if the item already has a tooltip with "Ancient Curse"
                // or if it has a tooltip entry in the language file
                boolean hasModTooltip = false;
                String itemId = id.getPath();
                
                // First check existing tooltip lines
                for (Text line : lines) {
                    if (line.getString().contains("Ancient Curse")) {
                        hasModTooltip = true;
                        break;
                    }
                }
                
                // Special handling for items with custom tooltips
                boolean isSpecialItem = itemId.equals("scarab_talisman") ||
                    itemId.equals("phial_of_lotus_essence") ||
                    itemId.equals("elixir_of_ras_spark") ||
                    itemId.equals("ancient_pick") ||
                    itemId.equals("snake_staff") ||
                    itemId.equals("scarab_incense_item") ||
                    itemId.equals("eternal_sigil");
                
                if (itemId.startsWith("bronze_")) {
                    hasModTooltip = true; // Mark as having tooltip to skip it
                }
                
                // Add the Ancient Curse tooltip to special items with existing tooltips
                if (isSpecialItem && !hasModTooltip) {
                    // Add the tooltip at the end of the existing tooltips
                    
                    // Use BLUE formatting to match the existing tooltips
                    Text tooltipText = Text.translatable("tooltip.ancientcurse.ancient_curse")
                        .formatted(Formatting.BLUE);
                    
                    // Add the tooltip at the appropriate position
                    lines.add(tooltipText);
                    hasModTooltip = true;
                }
                

                // Only add the tooltip if it doesn't already have one
                if (!hasModTooltip) {
                    // Use BLUE formatting to match the bronze tool tooltips
                    Text tooltipText = Text.translatable("tooltip.ancientcurse.ancient_curse")
                        .formatted(Formatting.BLUE);  // Match the blue color of bronze tooltips
                    lines.add(tooltipText);
                }
            }
        });
    }
}
