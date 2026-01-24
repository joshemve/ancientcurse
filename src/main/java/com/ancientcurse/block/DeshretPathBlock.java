package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Deshret Path Block - A path variant for deshret sand blocks
 */
public class DeshretPathBlock extends SandPathBlock {
    public DeshretPathBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if path should convert back to deshret sand
        BlockState stateAbove = world.getBlockState(pos.up());
        if (stateAbove.isSolid() && stateAbove.isSideSolidFullSquare(world, pos.up(), Direction.DOWN)) {
            // Convert back to deshret sand
            world.setBlockState(pos, ModBlocks.DESHRET_SAND.getDefaultState());
        }
    }
}
