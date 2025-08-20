package com.ancientcurse.client.color;

import com.ancientcurse.block.RockBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 * Client-side color provider for rock blocks.
 * This handles the coloring of rocks based on the block beneath them.
 */
public class RockColorProvider implements BlockColorProvider, ItemColorProvider {
    
    // Cache to improve performance
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final int ITEM_COLOR = 0x8F8F8F;
    
    @Override
    public int getColor(BlockState state, BlockRenderView world, BlockPos pos, int tintIndex) {
        if (world == null || pos == null) {
            return DEFAULT_COLOR;
        }
        
        // Check if this is a rock block (could be any of our rock blocks)
        if (!(state.getBlock() instanceof RockBlock)) {
            return DEFAULT_COLOR;
        }
        
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);
        
        if (belowState.isAir()) {
            return DEFAULT_COLOR;
        }
        
        // First try to get the block's color provider tint
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getBlockColors() != null) {
            try {
                int color = client.getBlockColors().getColor(belowState, world, belowPos, 0);
                if (color != -1) {
                    return color;
                }
            } catch (Exception ignored) {
                // Fall through to map color
            }
        }
        
        // Fallback to map color
        try {
            int mapColor = belowState.getMapColor(world, belowPos).color;
            // Ensure the color has full alpha and is properly formatted
            return (mapColor & 0x00FFFFFF) | 0xFF000000;
        } catch (Exception e) {
            return DEFAULT_COLOR;
        }
    }
    
    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        // Default stone color for items in inventory
        return ITEM_COLOR;
    }
} 