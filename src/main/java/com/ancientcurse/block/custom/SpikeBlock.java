package com.ancientcurse.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Spike Block - Wooden spikes that damage entities walking over them.
 * 
 * Features:
 * - Damages entities that walk on it
 * - Breaks if the block below is removed (like sand falling)
 * - Transparent rendering to see block below
 * - Craftable with sticks
 */
public class SpikeBlock extends Block {
    // Collision shape - slightly taller than visual to catch entities
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);
    
    // Outline shape for selection - matches the visual appearance
    protected static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    
    // Damage dealt to entities walking on spikes
    private static final float SPIKE_DAMAGE = 2.0f;

    public SpikeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Entities can walk into the spikes slightly
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
        return net.minecraft.util.shape.VoxelShapes.empty();
    }

    /**
     * Spikes can only be placed on solid blocks.
     */
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        // Can place on any solid top surface
        return belowState.isSideSolidFullSquare(world, below, Direction.UP);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (!canPlaceAt(getDefaultState(), ctx.getWorld(), ctx.getBlockPos())) {
            return null; // Can't place here
        }
        return this.getDefaultState();
    }

    /**
     * Handle neighbor updates - break if block below is removed.
     * This handles cases like sand falling, block being mined, etc.
     */
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                                  WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // If the block below changed, check if we can still exist
        if (direction == Direction.DOWN) {
            if (!canPlaceAt(state, world, pos)) {
                // Drop the block as an item and replace with air
                return Blocks.AIR.getDefaultState();
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    /**
     * Damage entities that step on the spikes.
     */
    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Don't damage creative/spectator players
            if (livingEntity instanceof net.minecraft.entity.player.PlayerEntity player) {
                if (player.isCreative() || player.isSpectator()) {
                    return;
                }
            }
            
            // Create spike damage source
            DamageSource damageSource = world.getDamageSources().create(DamageTypes.CACTUS);
            
            // Deal damage
            livingEntity.damage(damageSource, SPIKE_DAMAGE);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    /**
     * Also damage entities that land on spikes (from falling).
     */
    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Don't damage creative/spectator players
            if (livingEntity instanceof net.minecraft.entity.player.PlayerEntity player) {
                if (player.isCreative() || player.isSpectator()) {
                    super.onLandedUpon(world, state, pos, entity, fallDistance);
                    return;
                }
            }
            
            // Create spike damage source
            DamageSource damageSource = world.getDamageSources().create(DamageTypes.CACTUS);
            
            // Deal extra damage based on fall distance (minimum spike damage)
            float damage = Math.max(SPIKE_DAMAGE, fallDistance * 0.5f);
            livingEntity.damage(damageSource, damage);
        }
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }
}
