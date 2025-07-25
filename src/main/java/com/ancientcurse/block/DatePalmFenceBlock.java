package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.util.math.Direction;

public class DatePalmFenceBlock extends FenceBlock {
    public DatePalmFenceBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean canConnect(BlockState state, boolean neighborIsFullSquare, Direction dir) {
        Block block = state.getBlock();
        return block == this || block == ModBlocks.DATE_PALM_FENCE_GATE || super.canConnect(state, neighborIsFullSquare, dir);
    }
}