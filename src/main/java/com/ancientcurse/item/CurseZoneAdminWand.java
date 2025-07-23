package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CurseZoneAdminWand extends Item {
    
    public CurseZoneAdminWand(Settings settings) {
        super(settings.maxCount(1));
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!player.hasPermissionLevel(2)) { // Require op level 2
            player.sendMessage(Text.literal("§cYou don't have permission to use this item"), true);
            return TypedActionResult.fail(player.getStackInHand(hand));
        }
        
        ItemStack stack = player.getStackInHand(hand);
        
        if (player.isSneaking()) {
            // Shift + right click in air = clear selection
            if (world.isClient) {
                // Call client-side code through reflection to avoid direct class references
                callClientClearSelection(player);
            }
            clearSelection(stack);
            player.sendMessage(Text.literal("§eSelection cleared"), true);
            return TypedActionResult.success(stack);
        }
        
        // Open menu GUI
        if (world.isClient) {
            NbtCompound nbt = stack.getOrCreateNbt();
            BlockPos pos1 = null;
            BlockPos pos2 = null;
            
            if (nbt.contains("pos1")) {
                pos1 = BlockPos.fromLong(nbt.getLong("pos1"));
            }
            if (nbt.contains("pos2")) {
                pos2 = BlockPos.fromLong(nbt.getLong("pos2"));
            }
            
            // Call client-side code through reflection to avoid direct class references
            final BlockPos finalPos1 = pos1;
            final BlockPos finalPos2 = pos2;
            final ItemStack finalStack = stack;
            
            callClientOpenMenu(finalPos1, finalPos2, finalStack, player);
        }
        
        return TypedActionResult.pass(stack);
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        
        if (player == null || !player.hasPermissionLevel(2)) {
            return ActionResult.FAIL;
        }
        
        ItemStack stack = context.getStack();
        NbtCompound nbt = stack.getOrCreateNbt();
        
        // Handle position selection
        if (!nbt.contains("pos1")) {
            // Set first position
            nbt.putLong("pos1", pos.asLong());
            if (!nbt.contains("pos2")) {
                nbt.remove("pos2"); // Clear pos2 if it exists
            }
            
            if (world.isClient) {
                callClientSetFirstPosition(player, pos);
            }
            
            player.sendMessage(Text.literal("§aFirst position set to " + pos.toShortString()), true);
        } else if (!nbt.contains("pos2")) {
            // Set second position
            nbt.putLong("pos2", pos.asLong());
            
            if (world.isClient) {
                callClientSetSecondPosition(player, pos);
            }
            
            BlockPos pos1 = BlockPos.fromLong(nbt.getLong("pos1"));
            player.sendMessage(Text.literal("§aSecond position set to " + pos.toShortString()), true);
            player.sendMessage(Text.literal("§eArea selected from " + pos1.toShortString() + " to " + pos.toShortString()), true);
            player.sendMessage(Text.literal("§eRight-click in air to configure the zone"), true);
        } else {
            // Both positions already set, clear and start new selection
            clearSelection(stack);
            nbt.putLong("pos1", pos.asLong());
            
            if (world.isClient) {
                callClientClearSelection(player);
                callClientSetFirstPosition(player, pos);
            }
            
            player.sendMessage(Text.literal("§aNew selection started. First position set to " + pos.toShortString()), true);
        }
        
        return ActionResult.SUCCESS;
    }
    
    private void clearSelection(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.remove("pos1");
        nbt.remove("pos2");
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Make it visually distinct as an admin item
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (world.isClient && entity instanceof PlayerEntity player && selected) {
            // Update selection rendering while holding the wand
            NbtCompound nbt = stack.getOrCreateNbt();
            if (nbt.contains("pos1")) {
                BlockPos pos1 = BlockPos.fromLong(nbt.getLong("pos1"));
                callClientSetFirstPosition(player, pos1);
                
                if (nbt.contains("pos2")) {
                    BlockPos pos2 = BlockPos.fromLong(nbt.getLong("pos2"));
                    callClientSetSecondPosition(player, pos2);
                }
            }
        }
    }
    
    /**
     * Safely calls the client-side method to open the menu screen
     */
    @Environment(EnvType.CLIENT)
    private void callClientOpenMenu(BlockPos pos1, BlockPos pos2, ItemStack stack, PlayerEntity player) {
        // This code only runs on the client side
        try {
            // Use reflection to avoid direct class references at class loading time
            Class<?> clientClass = Class.forName("com.ancientcurse.client.CurseZoneWandClient");
            java.lang.reflect.Method openMenuMethod = clientClass.getMethod("openCurseZoneMenu", BlockPos.class, BlockPos.class, Runnable.class);
            
            openMenuMethod.invoke(null, pos1, pos2, (Runnable)() -> {
                clearSelection(stack);
                callClientClearSelection(player);
            });
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to open curse zone menu", e);
        }
    }
    
    /**
     * Safely calls the client-side method to clear selection
     */
    @Environment(EnvType.CLIENT)
    private void callClientClearSelection(PlayerEntity player) {
        // This code only runs on the client side
        try {
            Class<?> clientClass = Class.forName("com.ancientcurse.client.CurseZoneWandClient");
            java.lang.reflect.Method clearMethod = clientClass.getMethod("clearSelection", PlayerEntity.class);
            clearMethod.invoke(null, player);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to clear selection", e);
        }
    }
    
    /**
     * Safely calls the client-side method to set first position
     */
    @Environment(EnvType.CLIENT)
    private void callClientSetFirstPosition(PlayerEntity player, BlockPos pos) {
        // This code only runs on the client side
        try {
            Class<?> clientClass = Class.forName("com.ancientcurse.client.CurseZoneWandClient");
            java.lang.reflect.Method setFirstMethod = clientClass.getMethod("setFirstPosition", PlayerEntity.class, BlockPos.class);
            setFirstMethod.invoke(null, player, pos);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to set first position", e);
        }
    }
    
    /**
     * Safely calls the client-side method to set second position
     */
    @Environment(EnvType.CLIENT)
    private void callClientSetSecondPosition(PlayerEntity player, BlockPos pos) {
        // This code only runs on the client side
        try {
            Class<?> clientClass = Class.forName("com.ancientcurse.client.CurseZoneWandClient");
            java.lang.reflect.Method setSecondMethod = clientClass.getMethod("setSecondPosition", PlayerEntity.class, BlockPos.class);
            setSecondMethod.invoke(null, player, pos);
        } catch (Exception e) {
            AncientCurse.LOGGER.error("Failed to set second position", e);
        }
    }
}