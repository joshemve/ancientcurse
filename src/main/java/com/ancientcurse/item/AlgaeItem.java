package com.ancientcurse.item;

import com.ancientcurse.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Special item class for algae that allows placing directly on water surfaces.
 */
public class AlgaeItem extends BlockItem {

    public AlgaeItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Try normal placement first
        ActionResult result = super.useOnBlock(context);
        
        // If it didn't work, let the use method handle water placement
        return result;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        // Perform raycast to see what the player is looking at
        BlockHitResult hitResult = raycast(world, player, RaycastContext.FluidHandling.SOURCE_ONLY);
        
        // If the raycast hit a block
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos();
            // Check if the hit is on water
            FluidState fluidState = world.getFluidState(blockPos);
            
            if (fluidState.isIn(FluidTags.WATER) && fluidState.isStill()) {
                // Place algae block on top of the water (not waterlogged)
                BlockPos abovePos = blockPos.up();
                
                // Check if the block above is empty
                if (world.getBlockState(abovePos).isAir()) {
                    // Place algae block
                    BlockState algaeState = ModBlocks.ALGAE.getDefaultState();
                    
                    if (!world.isClient) {
                        world.setBlockState(abovePos, algaeState, Block.NOTIFY_ALL);
                        world.emitGameEvent(player, GameEvent.BLOCK_PLACE, abovePos);
                        world.playSound(null, abovePos, SoundEvents.BLOCK_WET_GRASS_PLACE, 
                                SoundCategory.BLOCKS, 1.0f, 0.8f);
                        
                        // Decrease item stack if not in creative mode
                        if (!player.getAbilities().creativeMode) {
                            itemStack.decrement(1);
                        }
                    }
                    
                    return TypedActionResult.success(itemStack, world.isClient());
                }
            }
        }
        
        // Otherwise, use default behavior
        return TypedActionResult.pass(itemStack);
    }
} 