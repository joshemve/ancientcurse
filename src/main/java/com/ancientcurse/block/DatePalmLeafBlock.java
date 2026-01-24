package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Custom leaf block for Date Palm trees
 */
public class DatePalmLeafBlock extends LeavesBlock {

    public DatePalmLeafBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(DISTANCE, 7)
                .with(PERSISTENT, false)
                .with(Properties.WATERLOGGED, false));
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(PERSISTENT) && state.get(DISTANCE) == 7) {
            // Check if there's a Date Palm Log nearby within a larger radius (10 blocks)
            // to support large NBT-based tree structures where leaves might be far from the
            // trunk
            if (isLogNearby(world, pos, 10)) {
                // If a log is found, reset distance to 1 to keep it alive
                world.setBlockState(pos, state.with(DISTANCE, 1), 3);
                return;
            }
        }
        // Call the parent method to handle normal leaf decay if no log is nearby
        super.randomTick(state, world, pos, random);
    }

    /**
     * Searches for a Date Palm Log within a specified radius.
     */
    private boolean isLogNearby(ServerWorld world, BlockPos pos, int radius) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutable.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (world.getBlockState(mutable).isOf(com.ancientcurse.ModBlocks.DATE_PALM_LOG)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE, PERSISTENT, Properties.WATERLOGGED);
    }
}
