package com.ancientcurse.item.armor;

import com.ancientcurse.client.renderer.armor.VeilOfAnubisRenderer;
import com.ancientcurse.material.VeilOfAnubisArmorMaterial;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VeilOfAnubisItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int COOLDOWN_TICKS = 1200; // 60 seconds
    
    public VeilOfAnubisItem() {
        super(new VeilOfAnubisArmorMaterial(), Type.HELMET, new FabricItemSettings());
    }
    
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private VeilOfAnubisRenderer armorRenderer;
            
            @Override
            public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
                if (this.armorRenderer == null)
                    this.armorRenderer = new VeilOfAnubisRenderer();
                
                this.armorRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.armorRenderer;
            }
        });
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<VeilOfAnubisItem> animationState) {
        animationState.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Check if player is wearing the veil and crouching
        if (user.getEquippedStack(EquipmentSlot.HEAD) == stack && user.isSneaking()) {
            if (!world.isClient) {
                // Check cooldown
                UUID playerId = user.getUuid();
                long currentTime = world.getTime();
                Long lastUse = cooldowns.get(playerId);
                
                if (lastUse == null || currentTime - lastUse >= COOLDOWN_TICKS) {
                    // Perform Judgment Pulse
                    performJudgmentPulse(world, user);
                    cooldowns.put(playerId, currentTime);
                    return TypedActionResult.success(stack, world.isClient());
                } else {
                    // Still on cooldown
                    long remainingTicks = COOLDOWN_TICKS - (currentTime - lastUse);
                    int remainingSeconds = (int)(remainingTicks / 20);
                    user.sendMessage(Text.literal("Judgment Pulse on cooldown: " + remainingSeconds + " seconds").formatted(Formatting.RED), true);
                    return TypedActionResult.fail(stack);
                }
            }
            return TypedActionResult.success(stack, world.isClient());
        }
        
        return super.use(world, user, hand);
    }
    
    private void performJudgmentPulse(World world, PlayerEntity caster) {
        double radius = 6.0;
        Vec3d center = caster.getPos();
        
        // Give caster Resistance I for 5 seconds
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
        
        // Get all entities in radius
        Box area = new Box(center.add(-radius, -radius, -radius), center.add(radius, radius, radius));
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, area, 
            entity -> entity.distanceTo(caster) <= radius && entity != caster && entity.isAlive() && !entity.isSpectator());
        
        // Process entities - apply effects to all non-player entities (enemies)
        for (LivingEntity entity : entities) {
            if (!(entity instanceof PlayerEntity)) {
                // Apply Blindness I and Slowness I for 4 seconds to enemies
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0));
            }
        }
        
        // Visual and audio effects
        if (world instanceof ServerWorld serverWorld) {
            // Spawn shadow/smoke particles rising around the caster
            for (int i = 0; i < 30; i++) {
                double angle = (Math.PI * 2) * i / 30;
                double distance = 1.5;
                double x = center.x + Math.cos(angle) * distance;
                double z = center.z + Math.sin(angle) * distance;
                
                for (int y = 0; y < 3; y++) {
                    serverWorld.spawnParticles(
                        ParticleTypes.LARGE_SMOKE,
                        x, center.y + (y * 0.5), z,
                        1, 0.1, 0.1, 0.1, 0.02
                    );
                }
            }
            
            // Additional soul particles for mystical effect
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * Math.PI * 2;
                double distance = Math.random() * radius;
                double x = center.x + Math.cos(angle) * distance;
                double z = center.z + Math.sin(angle) * distance;
                
                serverWorld.spawnParticles(
                    ParticleTypes.SOUL,
                    x, center.y + 1, z,
                    1, 0, 0.1, 0, 0.01
                );
            }
        }
        
        // Play deep hum and whisper sounds
        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, 
            SoundCategory.PLAYERS, 1.0f, 0.5f); // Deep hum
        world.playSound(null, caster.getBlockPos(), SoundEvents.PARTICLE_SOUL_ESCAPE, 
            SoundCategory.PLAYERS, 0.7f, 0.8f); // Whisper-like sound
        
        // Send feedback to caster
        caster.sendMessage(Text.literal("Judgment has been passed!").formatted(Formatting.DARK_PURPLE), true);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("Worn by the silent judges of the dead.").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.literal("One gaze and your soul is weighed.").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Active: ").formatted(Formatting.DARK_PURPLE)
            .append(Text.literal("Crouch + Right-click for Judgment Pulse").formatted(Formatting.LIGHT_PURPLE)));
        tooltip.add(Text.literal("         ").append(Text.literal("Blinds & slows enemies, grants Resistance").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("         ").append(Text.literal("60 second cooldown").formatted(Formatting.GRAY)));
        super.appendTooltip(stack, world, tooltip, context);
    }
}