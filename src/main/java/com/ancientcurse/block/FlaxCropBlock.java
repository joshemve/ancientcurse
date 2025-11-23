package com.ancientcurse.block;

import com.ancientcurse.ModBlocks;
import com.ancientcurse.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.List;

/**
 * Flax crop block that produces flax fibers, an important material
 * in Ancient Egyptian civilization.
 */
public class FlaxCropBlock extends CropBlock {
    public static final IntProperty AGE = Properties.AGE_3; // 4 growth stages (0-3)
    
    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[] {
        Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),  // Age 0
        Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),  // Age 1
        Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),  // Age 2
        Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0)  // Age 3
    };
    
    public FlaxCropBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(this.getAgeProperty(), 0));
    }
    
    @Override
    public IntProperty getAgeProperty() {
        return AGE;
    }
    
    @Override
    public int getMaxAge() {
        return 3;
    }
    
    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.FLAX_SEEDS; 
    }
    
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.FLAX_SEEDS);
    }

    // Override getDroppedStacks to control drops directly
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        List<ItemStack> drops = super.getDroppedStacks(state, builder);
        int age = state.get(AGE);

        // Only drop flax fiber if fully grown
        if (age == getMaxAge()) {
            // Add flax fiber (1-3)
            drops.add(new ItemStack(ModItems.FLAX_FIBER, builder.getWorld().random.nextInt(3) + 1));
            // Remove default seed drop
            drops.removeIf(stack -> stack.isOf(ModItems.FLAX_SEEDS));
            // Add back 1 seed guaranteed, plus chance for more
            drops.add(new ItemStack(ModItems.FLAX_SEEDS, 1));
            if (builder.getWorld().random.nextInt(3) == 0) { // 33% chance for extra seed
                 drops.add(new ItemStack(ModItems.FLAX_SEEDS, builder.getWorld().random.nextInt(2) + 1));
            }
        }

        return drops;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return AGE_TO_SHAPE[state.get(this.getAgeProperty())];
    }
    
    // Calculate growth speed based on the surrounding environment
    protected float getGrowthSpeed(World world, BlockPos pos) {
        float speed = 1.0F;
        BlockPos soilPos = pos.down();
        BlockState soilState = world.getBlockState(soilPos);
        
        // Check surrounding blocks for other crops (3x3x1 area)
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-1, 0, -1), pos.add(1, 0, 1))) {
            float blockInfluence = 1.0F;
            
            // If checking the current position, skip it
            if (blockPos.equals(pos)) {
                continue;
            }
            
            BlockState blockState = world.getBlockState(blockPos);
            
            // Calculate influence based on adjacent blocks
            if (blockState.getBlock() instanceof CropBlock) {
                // Adjacent crops slightly reduce growth
                blockInfluence = 0.8F;
            } else if (blockState.isAir()) {
                // Air is good for growth
                blockInfluence = 1.2F;
            }
            
            // If diagonal, reduce influence
            if (blockPos.getX() != pos.getX() && blockPos.getZ() != pos.getZ()) {
                blockInfluence *= 0.5F;
            }
            
            speed *= blockInfluence;
        }
        
        // Special bonus for Nile silt variants
        if (soilState.isOf(ModBlocks.FERTILE_NILE_SILT)) {
            speed *= 1.75F; // Very good for growth
        } else if (soilState.isOf(ModBlocks.TILLED_NILE_SILT)) {
            speed *= 2.0F; // Excellent for growth
        } else if (soilState.getBlock() instanceof FarmlandBlock) {
            // Standard farmland still provides a bonus
            int moisture = soilState.contains(FarmlandBlock.MOISTURE) ? soilState.get(FarmlandBlock.MOISTURE) : 0;
            speed *= 1.0F + (moisture / 7.0F);
        }
        
        return speed;
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBaseLightLevel(pos, 0) >= 9) {
            int currentAge = this.getAge(state);
            
            if (currentAge < this.getMaxAge()) {
                float growthSpeed = getGrowthSpeed(world, pos);
                
                // Faster growth on Nile silts
                BlockState soilState = world.getBlockState(pos.down());
                if (soilState.isOf(ModBlocks.FERTILE_NILE_SILT) || 
                    soilState.isOf(ModBlocks.TILLED_NILE_SILT)) {
                    growthSpeed *= 1.5f;
                }
                
                if (random.nextInt((int)(25.0F / growthSpeed) + 1) == 0) {
                    world.setBlockState(pos, this.withAge(currentAge + 1), Block.NOTIFY_LISTENERS);
                }
            }
        }
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Allow harvesting by right-clicking when fully grown
        if (!world.isClient && state.get(AGE) == getMaxAge()) {
            // Get drops based on loot context
            List<ItemStack> drops = getDroppedStacks(state, new LootContextParameterSet.Builder((ServerWorld)world)
                .add(LootContextParameters.BLOCK_STATE, state)
                .add(LootContextParameters.TOOL, player.getStackInHand(hand))
                .add(LootContextParameters.ORIGIN, hit.getPos()));

            // Spawn drops
            for (ItemStack drop : drops) {
                dropStack(world, pos, drop);
            }

            // Reset to age 0 (replant automatically)
            world.setBlockState(pos, this.withAge(0), Block.NOTIFY_LISTENERS);

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos soilPos = pos.down();
        BlockState soilState = world.getBlockState(soilPos);

        // Can grow on vanilla farmland or any type of Nile silt
        return (super.canPlaceAt(state, world, pos) ||
                soilState.isOf(ModBlocks.FERTILE_NILE_SILT) ||
                soilState.isOf(ModBlocks.TILLED_NILE_SILT));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
} 