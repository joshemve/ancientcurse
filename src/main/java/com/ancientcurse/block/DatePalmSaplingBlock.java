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

        // Find generated palm top, then decorate with fronds and fruit clusters
        BlockPos topLog = findTopPalmLog(world, pos);
        if (topLog != null) {
            // Create thick stump at the base
            createBaseStump(world, pos);

            tryPlaceLeafArms(world, random, topLog);
            tryPlaceDateClustersFromTop(world, random, topLog);
        }
    }

    /**
     * Creates a wider stump at the base of the palm tree
     */
    private void createBaseStump(ServerWorld world, BlockPos saplingPos) {
        // Find the actual base log position (might be slightly offset from sapling)
        BlockPos baseLog = findBasePalmLog(world, saplingPos);
        if (baseLog == null) return;

        // Place stump blocks around the base (1-2 blocks high)
        Direction[] horizontals = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : horizontals) {
            BlockPos stumpPos = baseLog.offset(dir);
            // Only place if it's air or replaceable (not solid blocks)
            if (world.getBlockState(stumpPos).isAir() || world.getBlockState(stumpPos).isReplaceable()) {
                world.setBlockState(stumpPos, ModBlocks.DATE_PALM_LOG.getDefaultState(), 3);
            }

            // Sometimes add a second layer for more natural look
            BlockPos stumpAbove = stumpPos.up();
            if (world.getBlockState(stumpAbove).isAir() || world.getBlockState(stumpAbove).isReplaceable()) {
                world.setBlockState(stumpAbove, ModBlocks.DATE_PALM_LOG.getDefaultState(), 3);
            }
        }
    }

    /**
     * Finds the base palm log near the sapling position
     */
    private BlockPos findBasePalmLog(ServerWorld world, BlockPos saplingPos) {
        int searchRadius = 3;
        BlockPos.Mutable m = new BlockPos.Mutable();

        // Search in a small area around and slightly above the sapling position
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    m.set(saplingPos.getX() + dx, saplingPos.getY() + dy, saplingPos.getZ() + dz);
                    if (world.getBlockState(m).isOf(ModBlocks.DATE_PALM_LOG)) {
                        // Check if this is near ground level (has solid block below)
                        BlockPos below = m.down();
                        if (world.getBlockState(below).isSolidBlock(world, below)) {
                            return m.toImmutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findTopPalmLog(ServerWorld world, BlockPos saplingPos) {
        int searchRadius = 5;
        int maxUp = 28; // plenty for tall palms
        BlockPos topLog = null;
        int topY = Integer.MIN_VALUE;

        BlockPos.Mutable m = new BlockPos.Mutable();
        int baseY = saplingPos.getY();
        for (int dy = 0; dy <= maxUp; dy++) {
            int y = baseY + dy;
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    m.set(saplingPos.getX() + dx, y, saplingPos.getZ() + dz);
                    if (world.getBlockState(m).isOf(ModBlocks.DATE_PALM_LOG)) {
                        if (y > topY) {
                            topY = y;
                            topLog = m.toImmutable();
                        }
                    }
                }
            }
        }
        return topLog;
    }

    private void tryPlaceDateClustersFromTop(ServerWorld world, Random random, BlockPos topLog) {
        int topY = topLog.getY();
        // Place 1-3 clusters on trunk positions just below the crown
        int clusters = 1 + random.nextInt(3);
        for (int i = 0; i < clusters; i++) {
            int offsetDown = 1 + random.nextInt(3); // 1-3 blocks below top
            int targetY = topY - offsetDown;

            // Find a log near the top at this Y (within a small horizontal window)
            BlockPos attachLog = findNearbyLogAtY(world, topLog, targetY, 2);
            if (attachLog == null) {
                attachLog = new BlockPos(topLog.getX(), targetY, topLog.getZ());
                if (!world.getBlockState(attachLog).isOf(ModBlocks.DATE_PALM_LOG)) {
                    continue;
                }
            }

            Direction[] dirs = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (int attempt = 0; attempt < 4; attempt++) {
                Direction dir = dirs[random.nextInt(dirs.length)];
                BlockPos fruitPos = attachLog.offset(dir);
                if (world.getBlockState(fruitPos).isAir()) {
                    BlockState fruit = ModBlocks.DATE_BLOCK.getDefaultState()
                        .with(DateBlock.FACING, dir.getOpposite())
                        .with(DateBlock.AGE, random.nextInt(3));
                    world.setBlockState(fruitPos, fruit, 3);
                    break;
                }
            }
        }
    }

    private void tryPlaceLeafArms(ServerWorld world, Random random, BlockPos topLog) {
        // Start 1 block below or at the top for arm roots
        int baseY = topLog.getY();
        BlockPos crownBase = new BlockPos(topLog.getX(), baseY - 1, topLog.getZ());

        // 8-directional arms (cardinals + diagonals) using offset vectors
        int[][] arms = new int[][] {
            { 1,  0}, {-1,  0}, { 0,  1}, { 0, -1}, // E, W, S, N
            { 1,  1}, { 1, -1}, {-1,  1}, {-1, -1}  // SE, NE, SW, NW
        };
        for (int[] arm : arms) {
            boolean cardinal = (arm[0] == 0 || arm[1] == 0);
            int len = (cardinal ? 5 : 4) + random.nextInt(2);
            placeFrond(world, random, crownBase, arm[0], arm[1], len);
        }
    }

    private void placeFrond(ServerWorld world, Random random, BlockPos start, int dx, int dz, int length) {
        // Perpendicular offsets for feathering
        int fx1 = (dz == 0) ? 0 : (dz > 0 ? 1 : -1);
        int fz1 = (dx == 0) ? 0 : (dx > 0 ? -1 : 1);
        int fx2 = -fx1;
        int fz2 = -fz1;

        BlockPos current = start;
        for (int i = 1; i <= length; i++) {
            int droop = (i >= 3 && i % 2 == 1) ? -1 : 0;
            current = current.add(dx, droop, dz);
            placeLeafIfAir(world, current);

            if (i <= 2) {
                placeLeafIfAir(world, current.up(1));
            }
            if (i >= 2 && i <= length - 1) {
                if (random.nextBoolean()) placeLeafIfAir(world, current.add(fx1, 0, fz1));
                if (random.nextBoolean()) placeLeafIfAir(world, current.add(fx2, 0, fz2));
            }
        }
    }

    private void placeLeafIfAir(ServerWorld world, BlockPos pos) {
        BlockState st = world.getBlockState(pos);
        if (st.isAir()) {
            world.setBlockState(pos, ModBlocks.DATE_PALM_LEAVES.getDefaultState(), 3);
        }
    }

    private BlockPos findNearbyLogAtY(ServerWorld world, BlockPos topLog, int y, int horizontalRadius) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                m.set(topLog.getX() + dx, y, topLog.getZ() + dz);
                if (world.getBlockState(m).isOf(ModBlocks.DATE_PALM_LOG)) {
                    return m.toImmutable();
                }
            }
        }
        return null;
    }
}
