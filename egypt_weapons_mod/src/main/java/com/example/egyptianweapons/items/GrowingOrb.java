package com.example.egyptianweapons.items;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.render.item.GrowingOrbRenderer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GrowingOrb extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public GrowingOrb() {
        super(new FabricItemSettings().maxCount(1).maxDamage(512));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private GrowingOrbRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new GrowingOrbRenderer();
                return this.renderer;
            }
        });
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.growingorb.idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient() && !player.getItemCooldownManager().isCoolingDown(this)) {
            if (player.isSneaking()) {
                createHealingAura(world, player);
                player.getItemCooldownManager().set(this, 300); // 15-second cooldown for healing
            } else {
                createGrowthAura(world, player);
                player.getItemCooldownManager().set(this, 100); // 5-second cooldown for growth
            }
            
            // Damage the item
            stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
        }
        
        return TypedActionResult.success(stack);
    }
    
    private void createHealingAura(World world, PlayerEntity player) {
        // Create a healing aura around the player
        double radius = 8.0;
        Box auraBox = player.getBoundingBox().expand(radius, radius, radius);
        
        // Get all living entities in range (including the player)
        List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, auraBox);
        
        // Play healing sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 1.0F);
        
        // Apply healing and effects to all entities in range
        for (LivingEntity livingEntity : entities) {
            if (livingEntity.isAlive()) {
                // Apply regeneration effect
                livingEntity.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1)
                );
                
                // Apply resistance effect
                livingEntity.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0)
                );
                
                // Spawn particles
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                        ParticleTypes.HEART,
                        livingEntity.getX(), livingEntity.getY() + livingEntity.getHeight() / 2, livingEntity.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0
                    );
                }
            }
        }
    }
    
    private void createGrowthAura(World world, PlayerEntity player) {
        // Create a growth aura around the player
        double radius = 5.0;
        Box auraBox = player.getBoundingBox().expand(radius, radius, radius);
        
        // Get all living entities in range (excluding the player)
        List<Entity> entities = world.getOtherEntities(player, auraBox, 
            entity -> entity instanceof LivingEntity && entity.isAlive());
        
        // Play growth sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.5F, 1.5F);
        
        // Apply growth effects to the player
        player.addStatusEffect(
            new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1)
        );
        
        player.addStatusEffect(
            new StatusEffectInstance(StatusEffects.SPEED, 200, 1)
        );
        
        player.addStatusEffect(
            new StatusEffectInstance(StatusEffects.JUMP_BOOST, 200, 1)
        );
        
        // Spawn particles around the player
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.DRAGON_BREATH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                50, 2.0, 2.0, 2.0, 0.1
            );
        }
    }
}
