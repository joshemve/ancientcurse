package com.ancientcurse.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class WallBlackStoneTorchBlock extends WallTorchBlock {
    public WallBlackStoneTorchBlock(Settings settings) {
        super(settings, null); // Pass null to prevent default particles
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        Direction direction = state.get(FACING);
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 0.7;
        double z = (double)pos.getZ() + 0.5;
        double g = 0.22;
        double h = 0.27;
        Direction opposite = direction.getOpposite();
        
        // Adjust position based on wall direction
        x += h * (double)opposite.getOffsetX();
        y += g;
        z += h * (double)opposite.getOffsetZ();
        
        // Optimized purple flame effect - reduced particle count for better performance
        
        // Main purple flame particle (always spawn at least one)
        world.addParticle(
            new net.minecraft.particle.DustParticleEffect(
                new net.minecraft.util.math.Vec3d(0.7, 0.0, 1.0).toVector3f(), // Purple
                1.3f
            ),
            x, y, z, 0.0, 0.0, 0.0
        );
        
        // Secondary particle with movement (50% chance)
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
        
        // Reduced smoke particles (10% chance)
        if (random.nextDouble() < 0.1) {
            world.addParticle(ParticleTypes.SMOKE, x, y + 0.9, z, 0.0, 0.0, 0.0);
        }
    }
}