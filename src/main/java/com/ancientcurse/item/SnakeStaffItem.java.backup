package com.ancientcurse.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Snake Staff - A magical staff with the power to poison enemies
 * Can be used to apply poison to nearby entities or as a melee weapon
 */
public class SnakeStaffItem extends Item {
    
    private static final int COOLDOWN_TICKS = 600; // 30-second cooldown (20 ticks per second * 30 seconds)
    private static final Vector3f POISON_PARTICLE_COLOR = new Vector3f(0.4f, 0.8f, 0.1f);
    
    public SnakeStaffItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(512)); // Can only stack to 1 and has durability
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Start using the staff (charging up)
        player.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }
    
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient && remainingUseTicks % 5 == 0) {
            // Add swirling particles around the staff while charging
            Vec3d lookVec = user.getRotationVector();
            double offsetX = lookVec.x * 0.5;
            double offsetY = lookVec.y * 0.5 + 1.0;
            double offsetZ = lookVec.z * 0.5;
            
            for (int i = 0; i < 3; i++) {
                double angle = (world.getTime() + i * 4) / 20.0 * Math.PI * 2;
                double radius = 0.3;
                double particleX = user.getX() + offsetX + Math.cos(angle) * radius;
                double particleY = user.getY() + offsetY + Math.sin(angle) * 0.1;
                double particleZ = user.getZ() + offsetZ + Math.sin(angle) * radius;
                
                world.addParticle(
                    new DustParticleEffect(POISON_PARTICLE_COLOR, 1.0f),
                    particleX, particleY, particleZ,
                    0, 0.05, 0
                );
            }
        }
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            // Release a poison cloud effect
            if (!world.isClient) {
                // Find nearby entities in a 5-block radius
                List<Entity> nearbyEntities = world.getOtherEntities(player, 
                    new Box(player.getX() - 5, player.getY() - 2, player.getZ() - 5,
                            player.getX() + 5, player.getY() + 3, player.getZ() + 5));
                
                boolean hitAny = false;
                
                // Apply poison to nearby hostile entities
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity living && !entity.isTeammate(player)) {
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 1)); // 10 seconds of Poison II
                        hitAny = true;
                    }
                }
                
                // Play sound and damage the staff
                world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.5F, 1.5F);
                
                if (hitAny) {
                    stack.damage(1, player, e -> e.sendToolBreakStatus(player.getActiveHand()));
                }
                
                // Set cooldown
                player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            }
        }
        
        return stack;
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply a weaker poison effect on melee hit
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0)); // 3 seconds of Poison I
        
        // Damage the staff
        stack.damage(1, attacker, e -> e.sendToolBreakStatus(Hand.MAIN_HAND));
        
        return true;
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 40; // 2 seconds to charge
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR; // Use the spear animation
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.snake_staff.tooltip").formatted(Formatting.GOLD));
        
        // Show cooldown information if applicable
        if (world != null && world.isClient) {
            Optional<PlayerEntity> playerOptional = Optional.ofNullable(world.getClosestPlayer(0, 0, 0, -1, false));
            playerOptional.ifPresent(player -> {
                if (player.getItemCooldownManager().isCoolingDown(this)) {
                    float cooldownProgress = player.getItemCooldownManager().getCooldownProgress(this, 0);
                    int remainingTicks = (int)(cooldownProgress * COOLDOWN_TICKS);
                    int secondsLeft = remainingTicks / 20;
                    tooltip.add(Text.literal("Recharging: " + secondsLeft + "s").formatted(Formatting.RED));
                } else {
                    tooltip.add(Text.literal("Ready to use").formatted(Formatting.GREEN));
                }
            });
        }
    }
}
