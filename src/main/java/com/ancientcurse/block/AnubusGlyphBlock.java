package com.ancientcurse.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AnubusGlyphBlock extends Block {
    // EnumProperty to track the upper or lower part of the block
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public AnubusGlyphBlock(Settings settings) {
        super(settings);
        // Default state sets this as the lower part by default
        this.setDefaultState(this.stateManager.getDefaultState().with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        // Add the 'HALF' property to the block state manager
        builder.add(HALF);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Get the position and world context
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        // If the space above is air, allow placement of the lower block and set the upper block afterward
        return world.getBlockState(pos.up()).canReplace(ctx) ? this.getDefaultState().with(HALF, DoubleBlockHalf.LOWER) : null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        // Set the upper part of the block once the lower part is placed
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // If the block is replaced, remove the corresponding upper or lower block
        if (!state.isOf(newState.getBlock())) {
            BlockPos other = (state.get(HALF) == DoubleBlockHalf.LOWER) ? pos.up() : pos.down();
            if (world.getBlockState(other).isOf(this)) {
                world.removeBlock(other, false);  // Remove the other block part
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
