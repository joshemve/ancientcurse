package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;

/**
 * Papyrus reed block that grows similar to sugar cane but with an Egyptian theme.
 * Can grow up to 3 blocks tall and requires water or fertile Nile silt nearby.
 */
public class PapyrusReedBlock extends Block {
    public static final IntProperty AGE = Properties.AGE_15;
    public static final BooleanProperty TOP = BooleanProperty.of("top");
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    
    public PapyrusReedBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(AGE, 0)
                .with(TOP, true));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
            return;
        }
        
        // Only grow if this is the top block in the stack
        if (state.get(TOP)) {
            int height = 1;
            // Check how tall the current reed stack is
            while (world.getBlockState(pos.down(height)).isOf(this) && height < 3) {
                height++;
            }
            
            // If not at max height and there's space above, grow a new reed
            if (height < 3 && world.isAir(pos.up()) && random.nextInt(10) == 0) {
                // Create a new reed on top and mark this one as not the top
                world.setBlockState(pos.up(), this.getDefaultState());
                world.setBlockState(pos, state.with(TOP, false));
            }
        }
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
                                               WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            // Return AIR state to break the block immediately
            return Blocks.AIR.getDefaultState();
        }
        
        // Update TOP property based on block above
        if (direction == Direction.UP) {
            boolean isTop = !neighborState.isOf(this);
            return state.with(TOP, isTop);
        }
        
        return state;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        
        // Check if there's another reed block above
        boolean isTop = !world.getBlockState(pos.up()).isOf(this);
        
        if (state != null) {
            return state.with(TOP, isTop);
        }
        return null;
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.down());
        
        // Check if block below is a valid soil for papyrus reed
        if (blockState.isOf(Blocks.GRASS_BLOCK) || 
            blockState.isOf(Blocks.DIRT) || 
            blockState.isOf(Blocks.SAND) || 
            blockState.isOf(ModBlocks.FERTILE_NILE_SILT) || 
            blockState.isOf(ModBlocks.DRY_NILE_SILT) || 
            blockState.isOf(ModBlocks.NILE_MUD) || 
            blockState.isOf(ModBlocks.RIVERBED) || 
            blockState.isOf(ModBlocks.RIVERBED_CLAY) ||
            blockState.isOf(ModBlocks.NILE_RIVER_SAND)) {
            
            // Check if there's water nearby
            BlockPos blockPos = pos.down();
            
            // Check all adjacent blocks for water
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockState adjacentState = world.getBlockState(blockPos.offset(direction));
                FluidState fluidState = world.getFluidState(blockPos.offset(direction));
                
                // Valid if adjacent block is water or has water (waterlogged)
                if (fluidState.isIn(FluidTags.WATER) || 
                    adjacentState.isOf(Blocks.FROSTED_ICE) || 
                    adjacentState.isOf(ModBlocks.RIVERBED_MOSS)) {
                    return true;
                }
            }
        }

        // Allow papyrus to stack on top of itself
        return blockState.isOf(this);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, TOP);
    }
} 