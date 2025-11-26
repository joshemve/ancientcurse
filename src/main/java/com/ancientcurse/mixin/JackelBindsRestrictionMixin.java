package com.ancientcurse.mixin;

import com.ancientcurse.entity.ZulmakEntity;
import com.ancientcurse.item.armor.JackelBindsItem;
import com.ancientcurse.system.JackelBindsBreakoutManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to restrict player actions when wearing Jackel Binds or grabbed by Zulmak
 */
@Mixin(PlayerEntity.class)
public abstract class JackelBindsRestrictionMixin extends net.minecraft.entity.LivingEntity {

    // Required constructor for extending LivingEntity
    protected JackelBindsRestrictionMixin(net.minecraft.entity.EntityType<? extends net.minecraft.entity.LivingEntity> entityType, net.minecraft.world.World world) {
        super(entityType, world);
    }

    /**
     * Check if the player is bound (wearing Jackel Binds)
     */
    private boolean ancientcurse$isBound() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        return chestpiece.getItem() instanceof JackelBindsItem;
    }

    /**
     * Check if the player is grabbed by a Zulmak
     */
    private boolean ancientcurse$isGrabbedByZulmak() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        return ZulmakEntity.isPlayerGrabbed(player);
    }

    /**
     * Check if player is restrained (either bound or grabbed)
     */
    private boolean ancientcurse$isRestrained() {
        return ancientcurse$isBound() || ancientcurse$isGrabbedByZulmak();
    }

    /**
     * Block attacking entities when bound or grabbed
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventAttackWhenBound(net.minecraft.entity.Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Check if grabbed by Zulmak (completely block attacks, no message needed)
        if (ancientcurse$isGrabbedByZulmak()) {
            ci.cancel();
            return;
        }

        // Check if wearing Jackel Binds (show message for breakout mechanic)
        if (ancientcurse$isBound()) {
            if (!player.getWorld().isClient) {
                // Always block attacking entities
                player.sendMessage(Text.literal("You cannot attack while bound!").formatted(Formatting.RED), true);
            }
            ci.cancel();
        }
    }

    /**
     * Block block breaking when bound or grabbed
     */
    @Inject(method = "isBlockBreakingRestricted", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventBlockBreakWhenBound(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.world.GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (ancientcurse$isRestrained()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Reset breakout progress on death
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void ancientcurse$resetBreakoutOnDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient) {
            JackelBindsBreakoutManager.getInstance().resetProgress(player.getUuid());
        }
    }

    /**
     * Block dropping items when bound or grabbed
     */
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventDropWhenBound(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<net.minecraft.entity.ItemEntity> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Silently block drops when grabbed by Zulmak
        if (ancientcurse$isGrabbedByZulmak()) {
            cir.setReturnValue(null);
            return;
        }

        // For Jackel Binds, show message and allow dropping the binds themselves
        if (ancientcurse$isBound()) {
            // Allow dropping binds if someone else removes them, but not regular items
            if (!(stack.getItem() instanceof JackelBindsItem)) {
                if (!player.getWorld().isClient) {
                    player.sendMessage(Text.literal("You cannot drop items while bound!").formatted(Formatting.RED), true);
                }
                cir.setReturnValue(null);
            }
        }
    }

    /**
     * Block item use when bound or grabbed (right-click actions)
     */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventInteractWhenBound(net.minecraft.entity.Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Silently block interactions when grabbed by Zulmak
        if (ancientcurse$isGrabbedByZulmak()) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // For Jackel Binds, show message
        if (ancientcurse$isBound()) {
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.literal("You cannot interact while bound!").formatted(Formatting.RED), true);
            }
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
