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
 * Pharaoh's Incense Jar - a ceremonial container used for burning sacred incense
 * Emits a soft glow when placed
 */
public class PharaohsIncenseJarBlock extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    
    // Shape matches the jar model
    protected static final VoxelShape SHAPE = Block.createCuboidShape(4.5, 0.0, 4.5, 11.5, 14.0, 11.5);
    
    public PharaohsIncenseJarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
        // Play a pottery breaking sound with a higher pitch for the smaller jar
        world.playSound(
            null, 
            pos, 
            SoundEvents.BLOCK_DECORATED_POT_SHATTER, 
            SoundCategory.BLOCKS, 
            1.0F, 
            0.9F + world.getRandom().nextFloat() * 0.3F // Slightly higher pitch for incense jar
        );
        
        // Spawn pottery breaking particles with some incense smoke
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
            
            // Add some smoke particles for the incense effect
            for (int i = 0; i < 8; i++) {
                world.addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5 + (random.nextFloat() - 0.5) * 0.2,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5 + (random.nextFloat() - 0.5) * 0.2,
                    0,
                    0.05,
                    0
                );
            }
        }
        
        super.onBreak(world, pos, state, player);
    }
}
