package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Scarab Sealed Urn - a ceremonial container sealed with a sacred scarab symbol
 * Used in ancient Egyptian burial rituals to store preserved items
 */
public class ScarabSealedUrnBlock extends Block {
    // Shape matches the urn model
    protected static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    
    public ScarabSealedUrnBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Play a pottery breaking sound
        world.playSound(
            null, 
            pos, 
            SoundEvents.BLOCK_DECORATED_POT_SHATTER, 
            SoundCategory.BLOCKS, 
            1.0F, 
            0.8F + world.getRandom().nextFloat() * 0.4F 
        );
        
        // Spawn pottery breaking particles
        if (world.isClient) {
            Random random = world.getRandom();
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
        }
        
        super.onBreak(world, pos, state, player);
    }
}
