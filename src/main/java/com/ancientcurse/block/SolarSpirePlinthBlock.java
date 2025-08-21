package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import java.util.List;

/**
 * Solar Spire Plinth - The base foundation block for constructing a Solar Spire
 * This is the bottom component that must be placed first
 */
public class SolarSpirePlinthBlock extends BaseAncientCurseBlock {
    
    public SolarSpirePlinthBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        // Don't add the base Ancient Curse tooltip - Minecraft already shows the mod name
    }
    
    // No custom shape - uses default full block (16x16x16)
    
    /**
     * Checks if this plinth is part of a complete Solar Spire structure
     */
    public boolean isCompleteStructure(BlockView world, BlockPos pos) {
        // Check for crucible above
        BlockState crucibleState = world.getBlockState(pos.up());
        if (!(crucibleState.getBlock() instanceof SolarSpireCrucibleBlock)) {
            return false;
        }
        
        // Check for pyramidion above crucible
        BlockState pyramidionState = world.getBlockState(pos.up(2));
        return pyramidionState.getBlock() instanceof SolarSpirePyramidionBlock;
    }
}