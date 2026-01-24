package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import com.ancientcurse.block.registry.ConstructionBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SoulbloomBushBlock extends PlantBlock {
    public SoulbloomBushBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(ModBlocks.BLACK_SAND) ||
                floor.isOf(ModBlocks.BLACK_STONE) ||
                floor.isOf(ModBlocks.BLACK_COBBLESTONE) ||
                floor.isOf(ModBlocks.BLACKSTONE_BRICK) ||
                floor.isOf(ModBlocks.HARDENED_BLACK_STONE) ||
                floor.isOf(ModBlocks.WIND_SWEPT_BLACKSTONE) ||
                floor.isOf(ConstructionBlocks.BLACK_SAND_PATH);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity && !world.isClient) {
            entity.damage(world.getDamageSources().magic(), 1.0f);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(5) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 0.3 + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;

            // White/Soul particles
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
