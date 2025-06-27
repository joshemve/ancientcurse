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
            Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());

            // Check if the item belongs to the Ancient Curse mod
            if (id != null && id.getNamespace().equals(AncientCurse.MOD_ID)) {
                Text modTooltip = Text.translatable("tooltip.ancientcurse.ancient_curse").formatted(Formatting.DARK_PURPLE);

                // Add the tooltip only if it's not already present to prevent duplicates
                if (!lines.contains(modTooltip)) {
                    lines.add(modTooltip);
                }
            }
        });
    }
}
