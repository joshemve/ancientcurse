package com.ancientcurse.block.custom;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PitfallTrapBlock extends Block {
    // Height of 1 pixel
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public PitfallTrapBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    /**
     * This block is NOT opaque - allows light through and doesn't cull adjacent block faces.
     * Critical for seeing the block underneath through transparent parts.
     */
    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }
    
    /**
     * Returns the opacity of this block. 0 means fully transparent for light.
     */
    @Override
    public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }
    
    /**
     * The culling shape determines which faces of adjacent blocks are hidden.
     * Return empty shape so adjacent blocks render all their faces.
     */
    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        // Return empty shape so the block below renders its top face
        return net.minecraft.util.shape.VoxelShapes.empty();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return !world.isAir(pos.down());
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState();
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity) {
            // Trigger when entity bounding box intersects with the trap
            triggerTrapChain(world, pos);
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    /**
     * Triggers a chain reaction of pitfall traps.
     * Uses BFS to prevent stack overflow and optimize performance.
     */
    public void triggerTrapChain(World world, BlockPos startPos) {
        if (world.isClient) return;

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        queue.add(startPos);
        visited.add(startPos);

        // Limit the chain reaction to prevent lag nuke
        int maxBlocks = 256; 
        int count = 0;

        while (!queue.isEmpty() && count < maxBlocks) {
            BlockPos currentPos = queue.poll();
            count++;

            // Break the trap and the floor below
            breakTrapAndFloor(world, currentPos);

            // Check neighbors for connected traps
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighborPos = currentPos.offset(dir);
                if (!visited.contains(neighborPos) && world.getBlockState(neighborPos).getBlock() instanceof PitfallTrapBlock) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                }
            }
        }
    }

    private void breakTrapAndFloor(World world, BlockPos pos) {
        // 1. Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_SCAFFOLDING_BREAK, SoundCategory.BLOCKS, 1.0f, 0.5f);
        world.playSound(null, pos, SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.BLOCKS, 1.0f, 0.5f);

        // 2. Break the trap itself (no drops to prevent clutter, or drops if you prefer)
        world.breakBlock(pos, false); 

        // 3. Break the block below (if it's not bedrock/unbreakable)
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);
        
        if (floorState.getHardness(world, floorPos) >= 0) { // Check if breakable (bedrock is -1.0)
            world.breakBlock(floorPos, false); // false = no drops (destroyed instantly as requested)
            
            // Optional: Spawn some particles to indicate destruction
            if (world instanceof ServerWorld serverWorld) {
                // serverWorld.spawnParticles(...) 
            }
        }
    }
}
