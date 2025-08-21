package com.ancientcurse.item;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Custom item for Lotus Flower Pad that allows placement directly on water surface
 */
public class LotusFlowerPadItem extends BlockItem {
    
    public LotusFlowerPadItem(Block block, Settings settings) {
        super(block, settings);
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // First try normal block placement
        ActionResult result = super.useOnBlock(context);
        if (result != ActionResult.PASS) {
            return result;
        }
        
        // If normal placement failed, try water surface placement
        return tryWaterPlacement(context);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // Raycast to find what the player is looking at
        BlockHitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.SOURCE_ONLY);
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos();
            Direction direction = hitResult.getSide();
            BlockPos placePos = blockPos.offset(direction);
            
            // Check if we're clicking on water and can place above it
            if (world.getFluidState(blockPos).isOf(Fluids.WATER) && 
                world.getFluidState(blockPos).getLevel() == 8 &&
                direction == Direction.UP) {
                
                // Check if the position above water is air
                if (world.isAir(placePos) || world.getBlockState(placePos).isReplaceable()) {
                    // Place the lotus pad
                    if (!world.isClient) {
                        BlockState lotusState = ModBlocks.LOTUS_FLOWER_PAD.getDefaultState();
                        if (lotusState.canPlaceAt(world, placePos)) {
                            world.setBlockState(placePos, lotusState);
                            world.playSound(null, placePos, SoundEvents.BLOCK_LILY_PAD_PLACE, 
                                          SoundCategory.BLOCKS, 1.0F, 1.0F);
                            
                            // Consume the item if not in creative mode
                            ItemStack itemStack = user.getStackInHand(hand);
                            if (!user.isCreative()) {
                                itemStack.decrement(1);
                            }
                            
                            // Update statistics
                            user.incrementStat(Stats.USED.getOrCreateStat(this));
                        }
                    }
                    
                    return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
                }
            }
        }
        
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
    
    private ActionResult tryWaterPlacement(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        
        // Check if clicking next to water
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos checkPos = clickedPos.offset(direction);
            
            if (world.getFluidState(checkPos).isOf(Fluids.WATER) && 
                world.getFluidState(checkPos).getLevel() == 8 &&
                world.isAir(checkPos.up())) {
                
                BlockPos placePos = checkPos.up();
                BlockState lotusState = ModBlocks.LOTUS_FLOWER_PAD.getDefaultState();
                
                if (lotusState.canPlaceAt(world, placePos)) {
                    if (!world.isClient) {
                        world.setBlockState(placePos, lotusState);
                        world.playSound(null, placePos, SoundEvents.BLOCK_LILY_PAD_PLACE, 
                                      SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }
                    
                    ItemStack itemStack = context.getStack();
                    if (!context.getPlayer().isCreative()) {
                        itemStack.decrement(1);
                    }
                    
                    return ActionResult.SUCCESS;
                }
            }
        }
        
        return ActionResult.PASS;
    }
}