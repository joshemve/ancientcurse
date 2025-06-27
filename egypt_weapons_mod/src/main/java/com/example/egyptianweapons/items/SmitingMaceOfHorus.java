package com.example.egyptianweapons.items;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.model.HorusMaceModel;
import com.example.egyptianweapons.client.render.item.HorusMaceRenderer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
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
import software.bernie.geckolib.core.object.DataTicket;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SmitingMaceOfHorus extends SwordItem implements GeoItem {
    private static final String NBT_IS_SLAMMING = "IsSlamming";
    private static final int SLAM_COOLDOWN = 60;
    private static final int STRENGTH_COOLDOWN = 200; // 10 seconds
    private static final DataTicket<ItemStack> ITEMSTACK = new DataTicket<>("itemstack", ItemStack.class);
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public SmitingMaceOfHorus() {
        super(ToolMaterials.NETHERITE, 8, -3.2f, new FabricItemSettings().fireproof());
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private HorusMaceRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new HorusMaceRenderer();
                return this.renderer;
            }
        });
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient()) {
            if (player.isSneaking()) {
                performGroundSlam(world, player, stack);
                return TypedActionResult.success(stack);
            }
        }
        
        return TypedActionResult.pass(stack);
    }

    private void performGroundSlam(World world, PlayerEntity player, ItemStack stack) {
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }

        stack.getOrCreateNbt().putBoolean(NBT_IS_SLAMMING, true);
        
        double radius = 5.0;
        List<Entity> nearbyEntities = world.getOtherEntities(player, 
            player.getBoundingBox().expand(radius, 2.0, radius));
        
        boolean hitAny = false;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                double distance = entity.getPos().distanceTo(player.getPos());
                float damage = (float)(6.0 * (1.0 - distance / radius));
                
                livingEntity.damage(world.getDamageSources().playerAttack(player), damage);
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                
                Vec3d knockbackDir = entity.getPos().subtract(player.getPos()).normalize();
                entity.setVelocity(entity.getVelocity().add(knockbackDir.x * 0.5, 0.3, knockbackDir.z * 0.5));
                entity.velocityModified = true;
                
                hitAny = true;
            }
        }
        
        if (hitAny && world instanceof ServerWorld serverWorld) {
            // Play sound effect
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.0f);
            
            // Spawn particles
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * Math.PI * 2;
                double distance = Math.random() * radius;
                double x = player.getX() + Math.cos(angle) * distance;
                double z = player.getZ() + Math.sin(angle) * distance;
                
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, 
                    x, player.getY() + 0.1, z, 
                    1, 0, 0, 0, 0.1);
            }
        }
        
        // Set cooldown
        player.getItemCooldownManager().set(this, SLAM_COOLDOWN);
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !player.getWorld().isClient()) {
            // 20% chance to apply strength effect
            if (Math.random() < 0.2 && !player.getItemCooldownManager().isCoolingDown(this)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 1));
                
                // Visual and sound effects
                ServerWorld serverWorld = (ServerWorld) player.getWorld();
                serverWorld.spawnParticles(ParticleTypes.FLAME, 
                    player.getX(), player.getY() + 1.0, player.getZ(), 
                    10, 0.5, 0.5, 0.5, 0.1);
                
                serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.5f, 1.0f);
                
                player.getItemCooldownManager().set(this, STRENGTH_COOLDOWN);
            }
        }
        
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.horus_mace.idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
