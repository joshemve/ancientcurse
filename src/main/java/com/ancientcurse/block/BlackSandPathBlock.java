package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Black Sand Path Block - A path variant for black sand blocks
 */
public class BlackSandPathBlock extends SandPathBlock {
    public BlackSandPathBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if path should convert back to black sand
        BlockState stateAbove = world.getBlockState(pos.up());
        if (stateAbove.isSolid() && stateAbove.isSideSolidFullSquare(world, pos.up(), Direction.DOWN)) {
            // Convert back to black sand
            world.setBlockState(pos, ModBlocks.BLACK_SAND.getDefaultState());
        }
    }
}
