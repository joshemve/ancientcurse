package com.ancientcurse.mixin;

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
 * Mixin to restrict player actions when wearing Jackel Binds
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
     * Block attacking entities when bound, but allow punching air for breakout
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventAttackWhenBound(net.minecraft.entity.Entity target, CallbackInfo ci) {
        if (ancientcurse$isBound()) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (!player.getWorld().isClient) {
                // Always block attacking entities
                player.sendMessage(Text.literal("You cannot attack while bound!").formatted(Formatting.RED), true);
            }
            ci.cancel();
        }
    }

    /**
     * Block block breaking when bound
     */
    @Inject(method = "isBlockBreakingRestricted", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventBlockBreakWhenBound(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.world.GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (ancientcurse$isBound()) {
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
     * Block dropping items when bound
     */
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventDropWhenBound(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<net.minecraft.entity.ItemEntity> cir) {
        if (ancientcurse$isBound()) {
            PlayerEntity player = (PlayerEntity) (Object) this;
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
     * Block item use when bound (right-click actions)
     */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void ancientcurse$preventInteractWhenBound(net.minecraft.entity.Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (ancientcurse$isBound()) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.literal("You cannot interact while bound!").formatted(Formatting.RED), true);
            }
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
