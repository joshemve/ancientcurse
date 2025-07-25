package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.Direction;

public class DriedReedThatchBlock extends HorizontalFacingBlock {
    
    public DriedReedThatchBlock(Settings settings) {
        super(settings);
        // Set default facing direction
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Make the block face the player when placed
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Add the facing property to the block
        builder.add(FACING);
    }
}
