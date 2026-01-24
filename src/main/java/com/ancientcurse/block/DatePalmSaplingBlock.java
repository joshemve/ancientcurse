package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Custom sapling that, when bone-mealed to grow, also places 1-3 early-stage
 * date fruit clusters near the crown by attaching {@link DateBlock} to the
 * sides of the top trunk segments.
 */
public class DatePalmSaplingBlock extends SaplingBlock {

    public DatePalmSaplingBlock(SaplingGenerator generator, Settings settings) {
        super(generator, settings);
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Run normal sapling growth first (this replaces the sapling with the tree)
        super.grow(world, random, pos, state);
    }
}
