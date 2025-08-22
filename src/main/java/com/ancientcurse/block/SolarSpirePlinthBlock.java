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
import net.minecraft.world.World;
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
        tooltip.add(Text.literal("§6Foundation of the Solar Spire").formatted(net.minecraft.util.Formatting.GOLD));
        tooltip.add(Text.literal("§7Place first, then crucible above").formatted(net.minecraft.util.Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, options);
    }
    
    // No custom shape - uses default full block (16x16x16)
    
    /**
     * Checks if this plinth is part of a complete Solar Spire structure
     * This version is for lightweight checks (tooltips, client rendering)
     */
    public boolean isCompleteStructure(BlockView world, BlockPos pos) {
        try {
            // Check for crucible above
            BlockState crucibleState = world.getBlockState(pos.up());
            
            // Null safety check for edge cases during chunk loading
            if (crucibleState == null || !(crucibleState.getBlock() instanceof SolarSpireCrucibleBlock)) {
                return false;
            }
            
            // Check for pyramidion above crucible
            BlockState pyramidionState = world.getBlockState(pos.up(2));
            
            // Null safety check
            if (pyramidionState == null) {
                return false;
            }
            
            return pyramidionState.getBlock() instanceof SolarSpirePyramidionBlock;
        } catch (Exception e) {
            // Handle any edge cases gracefully (chunk boundaries, unloaded chunks, etc.)
            return false;
        }
    }
    
    /**
     * Checks if this plinth is part of a complete Solar Spire structure
     * This version is for server logic that needs to modify blocks/entities
     */
    public boolean isCompleteStructure(World world, BlockPos pos) {
        // Delegate to BlockView version since we're just reading
        return isCompleteStructure((BlockView) world, pos);
    }
}