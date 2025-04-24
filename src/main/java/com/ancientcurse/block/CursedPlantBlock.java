package com.ancientcurse.block;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.World;

/**
 * Base class for cursed plants that apply effects when entities touch them
 */
public class CursedPlantBlock extends PlantBlock {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);
    private final boolean applyWitherEffect;
    private final boolean applyPoisonEffect;
    
    public CursedPlantBlock(Settings settings, boolean applyWitherEffect, boolean applyPoisonEffect) {
        super(settings);
        this.applyWitherEffect = applyWitherEffect;
        this.applyPoisonEffect = applyPoisonEffect;
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            if (applyWitherEffect) {
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
            }
            if (applyPoisonEffect) {
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0));
            }
        }
    }
    
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        
        // Add custom particle effects here if needed
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        
        // Using block ID checks instead of direct references
        String blockId = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BLOCK)
            .getId(blockState.getBlock()).toString();
        
        // Can be placed on cursed earth
        if (blockId.equals("ancientcurse:cursed_earth")) {
            return true;
        }
        
        // Can be placed on certain vanilla blocks that would make sense for cursed plants
        return blockState.isOf(Blocks.SOUL_SAND) || 
               blockState.isOf(Blocks.SOUL_SOIL) || 
               blockState.isOf(Blocks.NETHERRACK) || 
               blockId.equals("ancientcurse:black_sand") || 
               blockId.equals("ancientcurse:black_stone") || 
               blockId.equals("ancientcurse:black_dust");
    }
}
