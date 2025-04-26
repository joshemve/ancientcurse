package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;

import java.util.List;

/**
 * Base class for all standard blocks in the Ancient Curse mod.
 * Automatically adds the "Ancient Curse" tooltip to all block items.
 */
public class BaseAncientCurseBlock extends Block {
    
    public BaseAncientCurseBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        tooltip.add(Text.translatable("tooltip.ancientcurse.ancient_curse").formatted(Formatting.DARK_PURPLE));
    }
}
