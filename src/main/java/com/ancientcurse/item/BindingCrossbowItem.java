package com.ancientcurse.item;

import com.ancientcurse.entity.BindingBoltEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Binding Crossbow - A special weapon that shoots binding bolts.
 *
 * When charged and fired, it shoots a BindingBoltEntity that can
 * stun Ra by triggering his hibernation state.
 *
 * Usage:
 * - Right-click to start charging
 * - Release to fire
 * - Cooldown between shots
 */
public class BindingCrossbowItem extends Item {

    // Charging time in ticks (1.5 seconds)
    private static final int CHARGE_TIME = 30;

    // Cooldown between shots (3 seconds)
    private static final int COOLDOWN_TICKS = 60;

    // Projectile speed
    private static final float PROJECTILE_SPEED = 2.5f;
    private static final float PROJECTILE_DIVERGENCE = 1.0f;

    public BindingCrossbowItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Start charging
        user.setCurrentHand(hand);

        // Play draw sound
        world.playSound(
            null,
            user.getX(), user.getY(), user.getZ(),
            SoundEvents.ITEM_CROSSBOW_LOADING_START,
            SoundCategory.PLAYERS,
            1.0f, 1.0f
        );

        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // Play loading sounds at intervals
        int ticksUsed = getMaxUseTime(stack) - remainingUseTicks;

        if (ticksUsed == CHARGE_TIME / 2) {
            world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE,
                SoundCategory.PLAYERS,
                1.0f, 1.0f
            );
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) return;

        int ticksCharged = getMaxUseTime(stack) - remainingUseTicks;

        // Only fire if fully charged
        if (ticksCharged >= CHARGE_TIME) {
            if (!world.isClient) {
                // Create and shoot the binding bolt
                BindingBoltEntity bolt = new BindingBoltEntity(world, user);
                bolt.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, PROJECTILE_SPEED, PROJECTILE_DIVERGENCE);

                world.spawnEntity(bolt);

                // Play shoot sound
                world.playSound(
                    null,
                    user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_CROSSBOW_SHOOT,
                    SoundCategory.PLAYERS,
                    1.0f, 1.0f
                );
            }

            // Apply cooldown
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

            // Stats
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        } else {
            // Not fully charged - play fail sound
            world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                SoundCategory.PLAYERS,
                0.5f, 0.5f
            );
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        // Allow holding indefinitely after charge completes
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    /**
     * Get the charge progress (0.0 to 1.0)
     */
    public static float getChargeProgress(int ticksCharged) {
        return Math.min(1.0f, (float) ticksCharged / CHARGE_TIME);
    }
}
