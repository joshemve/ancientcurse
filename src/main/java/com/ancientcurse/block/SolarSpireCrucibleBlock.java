package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import java.util.List;

/**
 * Solar Spire Crucible - The middle ceremonial vessel for constructing a Solar Spire
 * This block must be placed on top of a plinth
 */
public class SolarSpireCrucibleBlock extends BaseAncientCurseBlock {
    
    public SolarSpireCrucibleBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.literal("§6Sacred vessel of the Solar Spire").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7Place on plinth, then pyramidion above").formatted(net.minecraft.util.Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, options);
    }
    
    // No custom shape - uses default full block (16x16x16)
    
    /**
     * Checks if this crucible is properly positioned in a Solar Spire structure
     */
    public boolean isValidPosition(BlockView world, BlockPos pos) {
        // Check for plinth below
        BlockState plinthState = world.getBlockState(pos.down());
        return plinthState.getBlock() instanceof SolarSpirePlinthBlock;
    }
}