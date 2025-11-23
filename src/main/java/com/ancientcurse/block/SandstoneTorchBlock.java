package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.TorchBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

import java.util.Collections;
import java.util.List;

public class SandstoneTorchBlock extends TorchBlock {
    public SandstoneTorchBlock(Settings settings) {
        super(settings, ParticleTypes.FLAME);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockState = this.getDefaultState();
        WorldView worldView = ctx.getWorld();
        BlockPos blockPos = ctx.getBlockPos();
        Direction clickedFace = ctx.getSide();
        
        // First check if the player clicked on a horizontal face (wall)
        if (clickedFace.getAxis().isHorizontal()) {
            Direction facing = clickedFace.getOpposite();
            BlockState wallState = ModBlocks.WALL_SANDSTONE_TORCH.getDefaultState().with(WallSandstoneTorchBlock.FACING, facing);
            if (wallState.canPlaceAt(worldView, blockPos)) {
                return wallState;
            }
        }
        
        // If clicked face doesn't work or was top/bottom, check other directions
        Direction[] directions = ctx.getPlacementDirections();
        for (Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                BlockState wallState = ModBlocks.WALL_SANDSTONE_TORCH.getDefaultState().with(WallSandstoneTorchBlock.FACING, direction);
                if (wallState.canPlaceAt(worldView, blockPos)) {
                    return wallState;
                }
            }
        }
        
        return blockState;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        // Always drop the sandstone torch item
        return Collections.singletonList(new ItemStack(ModBlocks.SANDSTONE_TORCH));
    }
}