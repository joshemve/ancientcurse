package com.ancientcurse.block;

import net.minecraft.block.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Blood Lotus - A rare crimson lotus that grows in water
 * Used in crafting Pharaoh's Blood
 */
public class BloodLotusBlock extends PlantBlock implements Waterloggable {
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);
    
    public BloodLotusBlock(Settings settings) {
        super(settings
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.LILY_PAD)
            .luminance((state) -> 7) // Slight glow
            .nonOpaque()
        );
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState groundState = world.getBlockState(blockPos);
        
        // Can only be placed on blocks underwater or in shallow water
        if (groundState.isSideSolidFullSquare(world, blockPos, Direction.UP)) {
            BlockState aboveState = world.getBlockState(pos.up());
            return aboveState.isOf(Blocks.WATER) || aboveState.isOf(Blocks.AIR);
        }
        return false;
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                              WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Emit crimson particles occasionally
        if (random.nextInt(10) == 0) {
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D;
            double y = pos.getY() + 0.7D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D;
            
            world.addParticle(ParticleTypes.CRIMSON_SPORE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
        
        // Dripping blood effect
        if (random.nextInt(15) == 0) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D;
            
            world.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return fluidState.isOf(Fluids.WATER) && fluidState.isStill() ? 
            super.getPlacementState(ctx) : null;
    }
    
    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.getDefaultState();
    }
    
    // Make it a suspicious stew ingredient that gives regeneration
    public StatusEffectInstance getStewEffect() {
        return new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1);
    }
}