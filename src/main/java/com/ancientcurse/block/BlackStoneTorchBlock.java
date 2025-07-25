package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.TorchBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class BlackStoneTorchBlock extends TorchBlock {
    public BlackStoneTorchBlock(Settings settings) {
        super(settings, null); // Pass null to prevent default particles
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 0.7;
        double z = (double)pos.getZ() + 0.5;
        
        // Optimized purple flame effect - reduced particle count for better performance
        
        // Main purple flame particle (always spawn at least one)
        world.addParticle(
            new net.minecraft.particle.DustParticleEffect(
                new net.minecraft.util.math.Vec3d(0.7, 0.0, 1.0).toVector3f(), // Purple
                1.3f
            ),
            x, y, z, 0.0, 0.0, 0.0
        );
        
        // Secondary particle with movement (50% chance instead of 80%)
        if (random.nextDouble() < 0.5) {
            double offsetX = (random.nextDouble() - 0.5) * 0.08;
            double offsetZ = (random.nextDouble() - 0.5) * 0.08;
            world.addParticle(
                new net.minecraft.particle.DustParticleEffect(
                    new net.minecraft.util.math.Vec3d(0.9, 0.2, 1.0).toVector3f(), // Light purple
                    1.0f
                ),
                x + offsetX, y + 0.1, z + offsetZ, 0.0, 0.01, 0.0
            );
        }
        
        // Reduced smoke particles (10% chance instead of 20%)
        if (random.nextDouble() < 0.1) {
            world.addParticle(ParticleTypes.SMOKE, x, y + 0.9, z, 0.0, 0.0, 0.0);
        }
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockState = this.getDefaultState();
        WorldView worldView = ctx.getWorld();
        BlockPos blockPos = ctx.getBlockPos();
        Direction[] directions = ctx.getPlacementDirections();
        
        for (Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                BlockState wallState = ModBlocks.WALL_BLACK_STONE_TORCH.getDefaultState().with(WallBlackStoneTorchBlock.FACING, direction);
                if (wallState.canPlaceAt(worldView, blockPos)) {
                    return wallState;
                }
            }
        }
        
        return blockState;
    }
}