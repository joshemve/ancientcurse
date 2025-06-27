package com.example.egyptianweapons.items;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.render.item.WarAxeRenderer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
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

public class WarAxe extends AxeItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public WarAxe() {
        super(ToolMaterials.NETHERITE, 7, -3.0f, new FabricItemSettings().maxCount(1).maxDamage(2048));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private WarAxeRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new WarAxeRenderer();
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
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.war_axe.idle"));
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
            performSweepingAttack(world, player);
            player.getItemCooldownManager().set(this, 60); // 3-second cooldown
        }
        
        return TypedActionResult.success(stack);
    }
    
    private void performSweepingAttack(World world, PlayerEntity player) {
        // Create a box around the player for the sweeping attack
        double radius = 5.0;
        Box attackBox = player.getBoundingBox().expand(radius, 0.25, radius);
        
        // Get all living entities in range (excluding the player)
        List<Entity> entities = world.getOtherEntities(player, attackBox, 
            entity -> entity instanceof LivingEntity && entity.isAlive());
        
        if (!entities.isEmpty()) {
            // Play attack sound
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 0.8F);
            
            // Apply damage and effects to all entities in range
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity livingEntity) {
                    // Calculate damage based on distance (more damage closer to player)
                    double distance = entity.squaredDistanceTo(player);
                    float damage = (float)(10.0 - Math.sqrt(distance));
                    damage = Math.max(4.0f, damage); // Minimum damage of 4
                    
                    // Apply damage
                    livingEntity.damage(world.getDamageSources().playerAttack(player), damage);
                    
                    // Apply slowness effect
                    livingEntity.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1)
                    );
                    
                    // Apply knockback
                    Vec3d knockbackDir = entity.getPos().subtract(player.getPos()).normalize();
                    entity.addVelocity(knockbackDir.x * 0.8, 0.2, knockbackDir.z * 0.8);
                    entity.velocityModified = true;
                    
                    // Spawn particles
                    if (world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                            ParticleTypes.SWEEP_ATTACK,
                            entity.getX(), entity.getY() + 0.5, entity.getZ(),
                            5, 0.2, 0.2, 0.2, 0.0
                        );
                    }
                }
            }
            
            // Give the player a brief strength boost after a successful attack
            player.addStatusEffect(
                new StatusEffectInstance(StatusEffects.STRENGTH, 60, 1)
            );
        }
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Add bleeding effect on normal hits
        target.addStatusEffect(
            new StatusEffectInstance(StatusEffects.WITHER, 60, 0)
        );
        
        // Spawn blood particles
        if (attacker.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                10, 0.1, 0.1, 0.1, 0.2
            );
        }
        
        return super.postHit(stack, target, attacker);
    }
}
