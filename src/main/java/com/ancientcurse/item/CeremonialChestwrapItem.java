package com.ancientcurse.item;

import com.ancientcurse.client.renderer.armor.CeremonialChestwrapRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CeremonialChestwrapItem extends ArmorItem implements GeoItem {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final java.util.function.Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    private static final CeremonialChestwrapMaterial MATERIAL = new CeremonialChestwrapMaterial();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int COOLDOWN_TICKS = 1200; // 60 seconds
    
    public CeremonialChestwrapItem(Settings settings) {
        super(MATERIAL, Type.CHESTPLATE, settings);
    }
    
    private static class CeremonialChestwrapMaterial implements ArmorMaterial {
        @Override
        public int getDurability(Type type) {
            return 200; // Similar to leather
        }
        
        @Override
        public int getProtection(Type type) {
            return 3; // 3 armor points as requested
        }
        
        @Override
        public int getEnchantability() {
            return 15; // Good enchantability like leather
        }
        
        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
        }
        
        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(net.minecraft.item.Items.STRING); // Repair with string
        }
        
        @Override
        public String getName() {
            return "ceremonial_chestwrap";
        }
        
        @Override
        public float getToughness() {
            return 0.0f;
        }
        
        @Override
        public float getKnockbackResistance() {
            return 0.0f;
        }
    }
    
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity player && player.getEquippedStack(EquipmentSlot.CHEST) == stack) {
            // Apply passive effects every 20 ticks (1 second)
            if (world.getTime() % 20 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0, true, false, true));
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Check if player is wearing the chestwrap and crouching
        if (user.getEquippedStack(EquipmentSlot.CHEST) == stack && user.isSneaking()) {
            if (!world.isClient) {
                // Check cooldown
                UUID playerId = user.getUuid();
                long currentTime = world.getTime();
                Long lastUse = cooldowns.get(playerId);
                
                if (lastUse == null || currentTime - lastUse >= COOLDOWN_TICKS) {
                    // Perform Pulse of Harmony
                    performPulseOfHarmony(world, user);
                    cooldowns.put(playerId, currentTime);
                    return TypedActionResult.success(stack, world.isClient());
                } else {
                    // Still on cooldown
                    long remainingTicks = COOLDOWN_TICKS - (currentTime - lastUse);
                    int remainingSeconds = (int)(remainingTicks / 20);
                    user.sendMessage(Text.literal("Pulse of Harmony on cooldown: " + remainingSeconds + " seconds").formatted(Formatting.RED), true);
                    return TypedActionResult.fail(stack);
                }
            }
            return TypedActionResult.success(stack, world.isClient());
        }
        
        return super.use(world, user, hand);
    }
    
    private void performPulseOfHarmony(World world, PlayerEntity caster) {
        double radius = 6.0;
        Vec3d center = caster.getPos();
        
        // Get all entities in radius
        Box area = new Box(center.add(-radius, -radius, -radius), center.add(radius, radius, radius));
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, area, 
            entity -> entity.distanceTo(caster) <= radius && entity != caster);
        
        // Process entities
        for (LivingEntity entity : entities) {
            if (entity instanceof PlayerEntity) {
                // Heal allies
                entity.heal(4.0f); // 2 hearts
            } else if (entity.isAlive() && !entity.isSpectator()) {
                // Apply weakness to enemies
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0)); // 5 seconds
            }
        }
        
        // Visual and audio effects
        if (world instanceof ServerWorld serverWorld) {
            // Spawn gold dust particles in a wave
            for (int i = 0; i < 50; i++) {
                double angle = (Math.PI * 2) * i / 50;
                double distance = radius * (i / 50.0);
                double x = center.x + Math.cos(angle) * distance;
                double z = center.z + Math.sin(angle) * distance;
                double y = center.y + 0.5 + (Math.sin(i * 0.3) * 0.3);
                
                serverWorld.spawnParticles(
                    new DustParticleEffect(Vec3d.unpackRgb(0xFFD700).toVector3f(), 1.0f), // Gold color
                    x, y, z,
                    1, 0, 0, 0, 0
                );
            }
        }
        
        // Play harp chime sound
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 
            SoundCategory.PLAYERS, 1.0f, 1.2f);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 
            SoundCategory.PLAYERS, 0.7f, 1.5f);
        
        // Send feedback to caster
        caster.sendMessage(Text.literal("Pulse of Harmony activated!").formatted(Formatting.GOLD), true);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("Woven in gold for those who protect, not fight.").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.literal("Heals allies in times of need — but burdens the bold.").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Passive: ").formatted(Formatting.DARK_PURPLE)
            .append(Text.literal("Regeneration I, Weakness I").formatted(Formatting.LIGHT_PURPLE)));
        tooltip.add(Text.literal("Active: ").formatted(Formatting.DARK_PURPLE)
            .append(Text.literal("Crouch + Right-click for Pulse of Harmony").formatted(Formatting.LIGHT_PURPLE)));
        tooltip.add(Text.literal("         ").append(Text.literal("60 second cooldown").formatted(Formatting.GRAY)));
        super.appendTooltip(stack, world, tooltip, context);
    }
    
    // GeckoLib implementation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<CeremonialChestwrapItem> animationState) {
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
        consumer.accept(new RenderProvider() {
            private CeremonialChestwrapRenderer renderer;
            
            @Override
            public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
                if (this.renderer == null)
                    this.renderer = new CeremonialChestwrapRenderer();
                
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }
    
    @Override
    public java.util.function.Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }
}