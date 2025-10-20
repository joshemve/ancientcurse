package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

/**
 * The Mace of Horus - A powerful weapon blessed by the falcon god
 * 
 * Features:
 * - Sky Strike: Right-click to call down a divine strike from the heavens
 * - Falcon's Sight: Gives night vision when held
 * - Divine Protection: Chance to blind attackers (Eye of Horus protection)
 * - Aerial Supremacy: Increased damage to flying enemies
 */
public class HorusMaceItem extends SwordItem implements GeoItem {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final java.util.function.Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    private static final int SKY_STRIKE_COOLDOWN = 200; // 10 seconds
    private static final float SKY_STRIKE_DAMAGE = 20.0F;
    private static final float AERIAL_DAMAGE_MULTIPLIER = 1.5F;
    
    public HorusMaceItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
        // Register for GeckoLib rendering
        software.bernie.geckolib.animatable.SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.ancientcurse.horus_mace.tooltip1").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("item.ancientcurse.horus_mace.tooltip2").formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("item.ancientcurse.horus_mace.tooltip3").formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("item.ancientcurse.horus_mace.tooltip4").formatted(Formatting.AQUA));
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient && player.getItemCooldownManager().getCooldownProgress(this, 0) == 0) {
            // Sky Strike ability
            performSkyStrike(world, player);
            player.getItemCooldownManager().set(this, SKY_STRIKE_COOLDOWN);
            
            return TypedActionResult.success(stack);
        }
        
        return TypedActionResult.pass(stack);
    }
    
    private void performSkyStrike(World world, PlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) world;
        Vec3d lookDir = player.getRotationVec(1.0F);
        Vec3d targetPos = player.getPos().add(lookDir.multiply(8));
        
        // Find the highest block at target position
        double strikeY = world.getTopY();
        for (int y = world.getTopY(); y > targetPos.y; y--) {
            if (!world.isAir(new net.minecraft.util.math.BlockPos((int)targetPos.x, y, (int)targetPos.z))) {
                strikeY = y + 1;
                break;
            }
        }
        
        // Create divine light beam effect
        for (int i = 0; i < 50; i++) {
            double particleY = targetPos.y + (strikeY - targetPos.y) * i / 50.0;
            serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                targetPos.x, particleY, targetPos.z,
                3,
                0.1, 0.1, 0.1,
                0.02
            );
        }
        
        // Lightning effect
        serverWorld.spawnParticles(
            ParticleTypes.ELECTRIC_SPARK,
            targetPos.x, targetPos.y, targetPos.z,
            50,
            0.5, 1.0, 0.5,
            0.1
        );
        
        // Sound effects
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS,
            0.8F, 1.2F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS,
            0.8F, 1.5F);
        
        // Damage entities in area
        Box damageBox = new Box(
            targetPos.x - 3, targetPos.y - 1, targetPos.z - 3,
            targetPos.x + 3, targetPos.y + 4, targetPos.z + 3
        );
        
        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class, damageBox, 
            e -> e != player && e.isAlive()
        );
        
        for (LivingEntity entity : entities) {
            entity.damage(world.getDamageSources().magic(), SKY_STRIKE_DAMAGE);
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            
            // Knock entities away from center
            Vec3d knockback = entity.getPos().subtract(targetPos).normalize().multiply(1.5);
            entity.addVelocity(knockback.x, 0.5, knockback.z);
        }
        
        // Grant player brief strength
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1));
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Aerial Supremacy - extra damage to flying entities
        if (!target.isOnGround() || target.isFallFlying() || target.hasStatusEffect(StatusEffects.LEVITATION)) {
            float extraDamage = 7.0F * (AERIAL_DAMAGE_MULTIPLIER - 1.0F); // Base damage * multiplier
            target.damage(attacker.getWorld().getDamageSources().magic(), extraDamage);
            
            // Visual effect for aerial damage
            if (target.getWorld() instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) target.getWorld();
                serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                    20,
                    0.3, 0.3, 0.3,
                    0.1
                );
            }
        }
        
        return super.postHit(stack, target, attacker);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity player && selected) {
            // Falcon's Sight - night vision when held (smooth, no flashing)
            // Only reapply when effect is about to expire to prevent flashing
            if (!player.hasStatusEffect(StatusEffects.NIGHT_VISION) ||
                player.getStatusEffect(StatusEffects.NIGHT_VISION).getDuration() < 10) {
                // Long duration (30 seconds) with ambient effect (no particles, subtle icon)
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, true, false, false));
            }
        }
    }
    
    // Divine Protection - handled in player damage event
    public static boolean checkDivineProtection(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        
        if (mainHand.getItem() instanceof HorusMaceItem || offHand.getItem() instanceof HorusMaceItem) {
            // 30% chance to blind attacker
            if (player.getRandom().nextFloat() < 0.3F) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Always has enchantment glint
    }
    
    // GeckoLib implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<HorusMaceItem> animationState) {
        return PlayState.STOP;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    @Override
    public double getTick(Object itemStack) {
        return RenderUtils.getCurrentTick();
    }
    
    @Override
    public void createRenderer(java.util.function.Consumer<Object> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.RenderProvider() {
            private final com.ancientcurse.item.renderer.HorusMaceRenderer renderer = new com.ancientcurse.item.renderer.HorusMaceRenderer();

            @Override
            public net.minecraft.client.render.item.BuiltinModelItemRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
    
    @Override
    public java.util.function.Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }
}