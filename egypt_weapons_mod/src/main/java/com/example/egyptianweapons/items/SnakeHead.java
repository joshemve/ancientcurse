package com.example.egyptianweapons.items;

import com.example.egyptianweapons.client.render.item.SnakeHeadRenderer;
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
import java.util.function.Supplier;

/**
 * Snake head item for Egyptian Weapons mod.
 */
public class SnakeHead extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public SnakeHead() {
        super(new FabricItemSettings().maxCount(1).maxDamage(768));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SnakeHeadRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new SnakeHeadRenderer();
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
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.snakehead.idle"));
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
        
        if (!world.isClient()) {
            if (player.isSneaking()) {
                poisonCloud(world, player);
            } else {
                venomStrike(world, player);
            }
        }
        
        return TypedActionResult.success(stack);
    }
    
    private void poisonCloud(World world, PlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }
        
        int radius = 4;
        Vec3d pos = player.getPos();
        
        // Spawn poison particles
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            
            for (int i = 0; i < 100; i++) {
                double dx = pos.x + (world.random.nextDouble() * radius * 2) - radius;
                double dy = pos.y + (world.random.nextDouble() * 2);
                double dz = pos.z + (world.random.nextDouble() * radius * 2) - radius;
                
                serverWorld.spawnParticles(
                    ParticleTypes.SNEEZE, 
                    dx, dy, dz, 
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }
        }
        
        // Apply poison to nearby entities
        Box box = new Box(
            pos.x - radius, pos.y - 1, pos.z - radius,
            pos.x + radius, pos.y + 3, pos.z + radius
        );
        
        List<Entity> entities = world.getOtherEntities(player, box);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.POISON, 200, 1
                ));
                livingEntity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 100, 0
                ));
            }
        }
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 
            1.0F, 0.8F);
        
        player.getItemCooldownManager().set(this, 300);
    }
    
    private void venomStrike(World world, PlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }
        
        double reach = 3.0;
        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d end = start.add(look.multiply(reach));
        
        Box box = player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0);
        
        List<Entity> entities = world.getOtherEntities(player, box);
        boolean hitSomething = false;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.damage(world.getDamageSources().playerAttack(player), 4.0F);
                livingEntity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.POISON, 100, 2
                ));
                livingEntity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS, 60, 0
                ));
                
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 
                    1.0F, 1.5F);
                    
                hitSomething = true;
                break;
            }
        }
        
        if (hitSomething) {
            player.getItemCooldownManager().set(this, 40);
        }
    }
}
