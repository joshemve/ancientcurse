package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;

/**
 * The War Axe of Abydos - Ancient weapon of the first pharaohs
 * 
 * Features:
 * - Executioner's Strike: Right-click for a devastating spin attack
 * - Ancient Fury: Chance to inflict Wither on hit
 * - Pharaoh's Might: Increased damage against undead
 * - Royal Decree: Intimidates nearby enemies when killing a foe
 */
public class WarAxeOfAbydosItem extends AxeItem implements GeoItem {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final java.util.function.Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    private static final int EXECUTIONER_STRIKE_COOLDOWN = 300; // 15 seconds
    private static final float SPIN_ATTACK_DAMAGE = 15.0F;
    private static final float UNDEAD_DAMAGE_MULTIPLIER = 1.75F;
    
    public WarAxeOfAbydosItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
        // Register for GeckoLib rendering
        software.bernie.geckolib.animatable.SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.literal("Weapon of the first dynasty").formatted(Formatting.GOLD, Formatting.ITALIC));
        tooltip.add(Text.literal("Forged in the sacred forges of Abydos").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Abilities:").formatted(Formatting.DARK_RED));
        tooltip.add(Text.literal(" • Executioner's Strike: ").formatted(Formatting.RED)
            .append(Text.literal("Right-click for spin attack").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal(" • Ancient Fury: ").formatted(Formatting.DARK_PURPLE)
            .append(Text.literal("25% chance to inflict Wither").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal(" • Pharaoh's Might: ").formatted(Formatting.YELLOW)
            .append(Text.literal("75% bonus damage vs undead").formatted(Formatting.GRAY)));
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient && player.getItemCooldownManager().getCooldownProgress(this, 0) == 0) {
            // Executioner's Strike - Spin Attack
            performExecutionerStrike(world, player);
            player.getItemCooldownManager().set(this, EXECUTIONER_STRIKE_COOLDOWN);
            
            // Damage the item
            stack.damage(3, player, (p) -> p.sendToolBreakStatus(hand));
            
            return TypedActionResult.success(stack);
        }
        
        return TypedActionResult.pass(stack);
    }
    
    private void performExecutionerStrike(World world, PlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Grant temporary strength and resistance
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1));
        
        // Spin attack visual effect
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = player.getX() + Math.cos(angle) * 2.5;
            double z = player.getZ() + Math.sin(angle) * 2.5;
            
            serverWorld.spawnParticles(
                ParticleTypes.SWEEP_ATTACK,
                x, player.getY() + 1, z,
                1, 0, 0, 0, 0
            );
            
            if (i % 30 == 0) {
                serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    x, player.getY() + 1, z,
                    5, 0.1, 0.1, 0.1, 0.1
                );
            }
        }
        
        // Sound effects
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS,
            1.5F, 0.8F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS,
            1.0F, 1.2F);
        
        // Damage entities in area
        Box damageBox = new Box(
            player.getX() - 3, player.getY() - 1, player.getZ() - 3,
            player.getX() + 3, player.getY() + 2, player.getZ() + 3
        );
        
        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class, damageBox, 
            e -> e != player && e.isAlive() && player.canSee(e)
        );
        
        for (LivingEntity entity : entities) {
            float damage = SPIN_ATTACK_DAMAGE;
            
            // Extra damage to undead
            if (entity.isUndead()) {
                damage *= UNDEAD_DAMAGE_MULTIPLIER;
            }
            
            entity.damage(world.getDamageSources().playerAttack(player), damage);
            
            // Knockback
            Vec3d knockback = entity.getPos().subtract(player.getPos()).normalize().multiply(1.5);
            entity.addVelocity(knockback.x, 0.3, knockback.z);
            
            // Chance to apply wither
            if (world.random.nextFloat() < 0.4f) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
            }
        }
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Ancient Fury - chance to inflict Wither
        if (!target.getWorld().isClient && target.getWorld().random.nextFloat() < 0.25f) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
            
            // Visual effect
            if (target.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.05
                );
            }
        }
        
        // Pharaoh's Might - extra damage to undead
        if (target.isUndead()) {
            float extraDamage = 9.0f * (UNDEAD_DAMAGE_MULTIPLIER - 1.0f); // Base damage * multiplier
            target.damage(attacker.getWorld().getDamageSources().magic(), extraDamage);
            
            // Golden particles for undead damage
            if (target.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.WAX_OFF,
                    target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1
                );
            }
        }
        
        // Royal Decree - check if target is about to die and intimidate nearby enemies
        if (target.getHealth() <= 0 && !attacker.getWorld().isClient && attacker instanceof PlayerEntity player) {
            Box intimidationBox = new Box(
                attacker.getX() - 8, attacker.getY() - 3, attacker.getZ() - 8,
                attacker.getX() + 8, attacker.getY() + 3, attacker.getZ() + 8
            );
            
            List<LivingEntity> nearbyEnemies = attacker.getWorld().getEntitiesByClass(
                LivingEntity.class, intimidationBox,
                e -> e != attacker && e.isAlive() && e.canSee(attacker) && !(e instanceof PlayerEntity)
            );
            
            for (LivingEntity enemy : nearbyEnemies) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0));
            }
            
            // Intimidation effect
            if (attacker.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    attacker.getX(), attacker.getY() + 2, attacker.getZ(),
                    10, 2, 0.5, 2, 0
                );
            }
            
            // War cry sound
            attacker.getWorld().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS,
                0.7F, 1.5F);
        }
        
        return super.postHit(stack, target, attacker);
    }
    
    // Royal Decree - intimidate nearby enemies on kill
    // Note: Since onKill doesn't exist in AxeItem, we'll implement this functionality differently
    // This could be done through a mixin or by checking in postHit if the target is dead
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Always has enchantment glint
    }
    
    // GeckoLib implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<WarAxeOfAbydosItem> animationState) {
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
            private final com.ancientcurse.item.renderer.WarAxeOfAbydosRenderer renderer = new com.ancientcurse.item.renderer.WarAxeOfAbydosRenderer();

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