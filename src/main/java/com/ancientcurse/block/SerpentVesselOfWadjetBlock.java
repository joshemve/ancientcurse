package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Serpent Vessel of Wadjet - a ceremonial container decorated with serpent imagery
 * Represents the Egyptian goddess Wadjet, protector of Lower Egypt
 */
public class SerpentVesselOfWadjetBlock extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    
    // Shape matches the vessel model with its distinctive serpent design
    protected static final VoxelShape SHAPE = Block.createCuboidShape(4.375, 0.0, 4.4875, 11.625, 16.0, 11.4875);
    
    public SerpentVesselOfWadjetBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
    
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
    
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Play a pottery breaking sound with a snake-like hiss
        world.playSound(
            null, 
            pos, 
            SoundEvents.BLOCK_DECORATED_POT_SHATTER, 
            SoundCategory.BLOCKS, 
            1.0F, 
            0.8F + world.getRandom().nextFloat() * 0.3F
        );
        
        // Add a serpent-like sound effect
        world.playSound(
            null,
            pos,
            SoundEvents.ENTITY_ENDERMAN_SCREAM, // Using enderman scream as a substitute for snake hiss
            SoundCategory.BLOCKS,
            0.4F, // Lower volume
            1.5F + world.getRandom().nextFloat() * 0.2F // Higher pitch to sound more like a hiss
        );
        
        // Spawn pottery breaking particles with some serpent-themed particles
        if (world.isClient) {
            Random random = world.getRandom();
            // Pottery fragments
            for (int i = 0; i < 15; i++) {
                double xOffset = random.nextGaussian() * 0.15;
                double yOffset = random.nextGaussian() * 0.15;
                double zOffset = random.nextGaussian() * 0.15;
                
                world.addParticle(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5 + xOffset,
                    pos.getY() + 0.5 + yOffset,
                    pos.getZ() + 0.5 + zOffset,
                    xOffset * 0.5,
                    yOffset * 0.5,
                    zOffset * 0.5
                );
            }
            
            // Add some serpent-themed particles (green dust)
            for (int i = 0; i < 10; i++) {
                world.addParticle(
                    ParticleTypes.ENTITY_EFFECT,
                    pos.getX() + 0.5 + (random.nextFloat() - 0.5) * 0.5,
                    pos.getY() + 0.7 + (random.nextFloat() - 0.5) * 0.5,
                    pos.getZ() + 0.5 + (random.nextFloat() - 0.5) * 0.5,
                    0.1, // Green color component
                    0.8, // Green color component
                    0.1  // Green color component
                );
            }
        }
        
        super.onBreak(world, pos, state, player);
    }
}
