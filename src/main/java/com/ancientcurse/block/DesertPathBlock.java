package com.ancientcurse.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Desert Path Block - A path variant for normal sand and smooth sand blocks
 */
public class DesertPathBlock extends SandPathBlock {
    public DesertPathBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if path should convert back to normal sand
        BlockState stateAbove = world.getBlockState(pos.up());
        if (stateAbove.isSolid() && stateAbove.isSideSolidFullSquare(world, pos.up(), Direction.DOWN)) {
            // Convert back to sand
            world.setBlockState(pos, Blocks.SAND.getDefaultState());
        }
    }
}
