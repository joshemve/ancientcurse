package com.ancientcurse.block;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * Base class for Egyptian-themed plants that can be placed on desert and Nile-related blocks
 */
public class EgyptianPlantBlock extends PlantBlock {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);
    
    public EgyptianPlantBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        
        // Can be placed on Nile-related blocks - using block ID checks instead of direct references
        String blockId = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BLOCK)
            .getId(blockState.getBlock()).toString();
        
        if (blockId.equals("ancientcurse:fertile_nile_silt") || 
            blockId.equals("ancientcurse:dry_nile_silt") || 
            blockId.equals("ancientcurse:nile_river_sand") || 
            blockId.equals("ancientcurse:arid_nile_turf") || 
            blockId.equals("ancientcurse:light_nile_marsh") || 
            blockId.equals("ancientcurse:spotted_marsh") ||
            blockId.equals("ancientcurse:cursed_earth")) {
            return true;
        }
        
        // Can be placed on certain vanilla blocks that would make sense for desert plants
        return blockState.isOf(Blocks.SAND) || 
               blockState.isOf(Blocks.RED_SAND) || 
               blockState.isOf(Blocks.DIRT) || 
               blockState.isOf(Blocks.GRASS_BLOCK);
    }
}
