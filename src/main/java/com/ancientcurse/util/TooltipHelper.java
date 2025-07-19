package com.ancientcurse.util;

import com.ancientcurse.AncientCurse;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Helper class for adding tooltips to items and blocks from the Ancient Curse mod.
 */
public class TooltipHelper {
    
    // Items that have their own specific tooltips and don't need the generic "Ancient Curse" tag
    // Empty set - all items should show the Ancient Curse tag for consistency
    private static final Set<String> ITEMS_WITH_SPECIFIC_TOOLTIPS = Set.of(
        // All items now show the Ancient Curse tag
    );
    
    /**
     * Register the tooltip callback to add "Ancient Curse" to mod items that don't have specific tooltips.
     * This should be called during client initialization.
     */
    public static void registerTooltipCallback() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());

            // Check if the item belongs to the Ancient Curse mod
            if (id != null && id.getNamespace().equals(AncientCurse.MOD_ID)) {
                String itemName = id.getPath();
                
                // Only add generic tooltip to items that don't have specific tooltips
                if (!ITEMS_WITH_SPECIFIC_TOOLTIPS.contains(itemName)) {
                    Text modTooltip = Text.translatable("tooltip.ancientcurse.ancient_curse").formatted(Formatting.BLUE);

                    // Add the tooltip only if it's not already present to prevent duplicates
                    if (!lines.contains(modTooltip)) {
                        lines.add(modTooltip);
                    }
                }
            }
        });
    }
}
