package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.joml.Vector3f;
import net.minecraft.world.WorldView;

/**
 * A moss-like plant that provides regeneration to entities that touch it.
 * 
 * Only placeable on the underside of blocks, creating a hanging moss effect.
 * The moss will break if its supporting block is removed.
 */
public class KheruMossBlock extends EgyptianPlantBlock {
    
    // Custom shape for the hanging moss - positioned 3 pixels higher to connect to the ceiling
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 7.0, 2.0, 14.0, 16.0, 14.0);

    public KheruMossBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockAbove = world.getBlockState(pos.up());
        // Allow placement on solid blocks or leaves
        return blockAbove.isSideSolidFullSquare(world, pos.up(), Direction.DOWN) || 
               blockAbove.getBlock().getClass().getSimpleName().toLowerCase().contains("leaves");
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Only allow placement if clicked on the bottom face of a block
        if (ctx.getSide() != Direction.DOWN) {
            return null; // Return null to prevent placement
        }
        return super.getPlacementState(ctx);
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Apply regeneration effect
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0, false, false));
        }
    }
    
    // Dark purple color for the dripping particles
    private static final Vector3f DARK_PURPLE = new Vector3f(0.4f, 0.0f, 0.6f);
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Don't call super to avoid any parent class particles
        
        // Dripping particles - more frequent but optimized
        if (random.nextInt(8) == 0) {
            // Choose a random position under the moss
            double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
            double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
            
            // Create dripping effect
            world.addParticle(
                new DustParticleEffect(DARK_PURPLE, 0.8f),
                x, pos.getY() + 0.1, z,
                0.0, -0.05, 0.0  // Slow downward movement
            );
        }
        
        // Occasional falling particle for variety
        if (random.nextInt(20) == 0) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
            
            // Add a drip particle that falls faster
            world.addParticle(
                ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                x, pos.getY() + 0.2, z,
                0.0, 0.0, 0.0
            );
        }
    }
}
