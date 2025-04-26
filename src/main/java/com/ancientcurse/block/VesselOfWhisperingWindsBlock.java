package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Vessel of Whispering Winds - a mystical pottery item that glows and emits ethereal whispers
 */
public class VesselOfWhisperingWindsBlock extends Block {
    // Shape matches the tall vessel model with its distinctive neck and top
    protected static final VoxelShape SHAPE = Block.createCuboidShape(3.875, 0.0, 4.2125, 12.125, 16.25, 11.7125);
    
    public VesselOfWhisperingWindsBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Play a pottery breaking sound with a mystical wind effect
        world.playSound(
            null, 
            pos, 
            SoundEvents.BLOCK_DECORATED_POT_SHATTER, 
            SoundCategory.BLOCKS, 
            1.0F, 
            0.7F + world.getRandom().nextFloat() * 0.3F // Lower pitch for the larger vessel
        );
        
        // Add a wind sound effect
        world.playSound(
            null,
            pos,
            SoundEvents.ENTITY_PHANTOM_FLAP,
            SoundCategory.BLOCKS,
            0.6F,
            1.2F + world.getRandom().nextFloat() * 0.2F
        );
        
        // Spawn pottery breaking particles with mystical wind particles
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
            
            // Add some mystical wind particles
            for (int i = 0; i < 12; i++) {
                world.addParticle(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5 + (random.nextFloat() - 0.5) * 0.5,
                    pos.getY() + 0.7 + (random.nextFloat() - 0.5) * 0.5,
                    pos.getZ() + 0.5 + (random.nextFloat() - 0.5) * 0.5,
                    (random.nextFloat() - 0.5) * 0.2,
                    (random.nextFloat() - 0.5) * 0.2,
                    (random.nextFloat() - 0.5) * 0.2
                );
            }
        }
        
        super.onBreak(world, pos, state, player);
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Add ambient particles for a mystical effect
        if (random.nextInt(10) == 0) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.7D;
            double z = pos.getZ() + 0.5D;
            
            world.addParticle(
                ParticleTypes.END_ROD,
                x + (random.nextDouble() - 0.5D) * 0.5D,
                y + (random.nextDouble() - 0.5D) * 0.2D,
                z + (random.nextDouble() - 0.5D) * 0.5D,
                0, 0.05D, 0
            );
        }
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Players can interact with the vessel (for future functionality)
        ItemStack heldItem = player.getStackInHand(hand);
        
        if (!heldItem.isEmpty()) {
            // For now, just consume the action without doing anything
            // In the future this could trigger special effects or rituals
            return ActionResult.success(world.isClient);
        }
        
        return ActionResult.PASS;
    }
}
