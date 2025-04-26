package com.ancientcurse.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Base class for all standard items in the Ancient Curse mod.
 * Automatically adds the "Ancient Curse" tooltip to all items.
 */
public class BaseAncientCurseItem extends Item {
    
    public BaseAncientCurseItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.ancientcurse.ancient_curse").formatted(Formatting.DARK_PURPLE));
    }
}
