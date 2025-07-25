package com.ancientcurse.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class SandstonePillarBlock extends Block {
    public static final EnumProperty<PillarPart> PART = EnumProperty.of("part", PillarPart.class);
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    
    public SandstonePillarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(PART, PillarPart.SINGLE));
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        return this.getDefaultState().with(PART, getPillarPart(world, pos));
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() == Direction.Axis.Y) {
            return state.with(PART, getPillarPart(world, pos));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    
    private PillarPart getPillarPart(WorldAccess world, BlockPos pos) {
        boolean hasAbove = isPillar(world, pos.up());
        boolean hasBelow = isPillar(world, pos.down());
        
        if (hasAbove && hasBelow) {
            return PillarPart.MIDDLE;
        } else if (hasAbove && !hasBelow) {
            return PillarPart.BOTTOM;
        } else if (!hasAbove && hasBelow) {
            return PillarPart.TOP;
        } else {
            return PillarPart.SINGLE;
        }
    }
    
    private boolean isPillar(WorldAccess world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof SandstonePillarBlock;
    }
    
    public enum PillarPart implements StringIdentifiable {
        TOP("top"),
        MIDDLE("middle"),
        BOTTOM("bottom"),
        SINGLE("single");
        
        private final String name;
        
        PillarPart(String name) {
            this.name = name;
        }
        
        @Override
        public String asString() {
            return this.name;
        }
    }
}