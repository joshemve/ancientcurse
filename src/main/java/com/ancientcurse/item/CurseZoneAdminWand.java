package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.gui.CurseZoneEditorScreen;
import com.ancientcurse.gui.CurseZoneManagerScreen;
import com.ancientcurse.gui.CurseZoneMenuScreen;
import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.util.CurseZoneManager;
import com.ancientcurse.util.WandSelectionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
                WandSelectionManager.clearSelection(player);
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
            
            // Pass the wand and player for clearing selection
            MinecraftClient.getInstance().setScreen(new CurseZoneMenuScreen(pos1, pos2, () -> {
                clearSelection(stack);
                WandSelectionManager.clearSelection(player);
            }));
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
                WandSelectionManager.setFirstPosition(player, pos);
            }
            
            player.sendMessage(Text.literal("§aFirst position set to " + pos.toShortString()), true);
        } else if (!nbt.contains("pos2")) {
            // Set second position
            nbt.putLong("pos2", pos.asLong());
            
            if (world.isClient) {
                WandSelectionManager.setSecondPosition(player, pos);
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
                WandSelectionManager.clearSelection(player);
                WandSelectionManager.setFirstPosition(player, pos);
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
                WandSelectionManager.setFirstPosition(player, pos1);
                
                if (nbt.contains("pos2")) {
                    BlockPos pos2 = BlockPos.fromLong(nbt.getLong("pos2"));
                    WandSelectionManager.setSecondPosition(player, pos2);
                }
            }
        }
    }
}