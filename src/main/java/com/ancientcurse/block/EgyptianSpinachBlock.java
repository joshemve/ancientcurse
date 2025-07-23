package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.List;

public class EgyptianSpinachBlock extends PlantBlock {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public EgyptianSpinachBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // Drop 1-3 spinach when right-clicked
            int spinachCount = Random.create().nextBetween(1, 3);
            dropStack(world, pos, new ItemStack(ModItems.SPINACH, spinachCount));
            
            // Replace with air (fully harvested)
            world.removeBlock(pos, false);
            
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.CONSUME;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(Blocks.FARMLAND) || 
               floor.isOf(ModBlocks.TILLED_NILE_SILT) || 
               floor.isOf(ModBlocks.FERTILE_NILE_SILT) || 
               floor.isOf(ModBlocks.DRY_NILE_SILT) || 
               floor.isOf(Blocks.DIRT) || 
               floor.isOf(Blocks.GRASS_BLOCK);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        // Always drop 1-2 spinach when broken
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(ModItems.SPINACH, Random.create().nextBetween(1, 2)));
        return drops;
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR || super.canPathfindThrough(state, world, pos, type);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}