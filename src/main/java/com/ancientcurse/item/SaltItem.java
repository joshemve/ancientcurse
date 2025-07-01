package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.ModBlocks;
import com.ancientcurse.system.CursedEarthManager;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Salt Item - Places salt dust blocks and can create protective Salt Circles
 * Part of Phase 3: Balance Features from the Cursed Earth roadmap
 * 
 * Right-click to place salt dust (like redstone dust)
 * Sneak + Right-click with 8 salt = 3x3 protection for 1 hour
 */
public class SaltItem extends BaseAncientCurseItem {
    
    private static final int SALT_CIRCLE_RADIUS = 1; // 3x3 area (radius 1)
    private static final int SALT_CIRCLE_DURATION = 72000; // 1 hour in ticks (3600 seconds)
    private static final int SALT_REQUIRED = 8; // 8 salt items needed
    
    public SaltItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // If sneaking, try to create a salt circle
        if (user.isSneaking()) {
            return createSaltCircle(world, user, hand, itemStack);
        }
        
        // Otherwise, just consume the item (placement is handled in useOnBlock)
        return TypedActionResult.pass(itemStack);
    }
    
    private TypedActionResult<ItemStack> createSaltCircle(World world, PlayerEntity user, Hand hand, ItemStack itemStack) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            BlockPos playerPos = user.getBlockPos();
            
            // Check if player has enough salt
            int saltCount = countSaltInInventory(user);
            if (saltCount < SALT_REQUIRED) {
                user.sendMessage(Text.literal("You need " + SALT_REQUIRED + " salt to create a protective circle!")
                    .formatted(Formatting.RED), false);
                return TypedActionResult.fail(itemStack);
            }
            
            // Check if there's already a salt circle here
            if (CursedEarthManager.getInstance().hasSaltCircle(playerPos)) {
                user.sendMessage(Text.literal("There's already a salt circle protecting this area!")
                    .formatted(Formatting.YELLOW), false);
                return TypedActionResult.fail(itemStack);
            }
            
            // Consume salt from inventory
            if (!user.isCreative()) {
                consumeSaltFromInventory(user, SALT_REQUIRED);
            }
            
            // Create the salt circle
            CursedEarthManager.getInstance().createSaltCircle(serverWorld, playerPos, SALT_CIRCLE_RADIUS, SALT_CIRCLE_DURATION);
            
            // Play sound effect
            world.playSound(null, playerPos, SoundEvents.BLOCK_SAND_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
            
            // Send success message
            user.sendMessage(Text.literal("Salt circle created! This area is protected from cursed earth for 1 hour.")
                .formatted(Formatting.GREEN), false);
            
            AncientCurse.LOGGER.info("Player {} created salt circle at {}", user.getName().getString(), playerPos);
        }
        
        return TypedActionResult.success(itemStack, world.isClient());
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        
        if (player != null && player.isSneaking()) {
            // Create salt circle instead of placing dust
            return createSaltCircle(world, player, context.getHand(), context.getStack()).getResult();
        }
        
        // Try to place salt dust on the clicked surface
        Direction side = context.getSide();
        BlockPos placePos = pos.offset(side);
        
        // Check if we can place salt dust here
        if (world.getBlockState(placePos).isAir() && 
            ModBlocks.SALT_DUST.canPlaceAt(ModBlocks.SALT_DUST.getDefaultState(), world, placePos)) {
            
            // Place the salt dust block
            world.setBlockState(placePos, ModBlocks.SALT_DUST.getDefaultState());
            
            // Play placement sound
            world.playSound(null, placePos, SoundEvents.BLOCK_SAND_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
            
            // Consume item if not in creative
            if (player != null && !player.isCreative()) {
                context.getStack().decrement(1);
            }
            
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.FAIL;
    }
    
    /**
     * Counts salt items in player's inventory
     */
    private int countSaltInInventory(PlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == this) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Consumes salt from player's inventory
     */
    private void consumeSaltFromInventory(PlayerEntity player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == this) {
                int toConsume = Math.min(remaining, stack.getCount());
                stack.decrement(toConsume);
                remaining -= toConsume;
            }
        }
    }
} 