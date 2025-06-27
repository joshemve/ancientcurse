package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import org.joml.Vector3f;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Withered Staff - dropped by the Withered Pharaoh.
 * One-hand charge → launches a Wither skull and makes the purple runes glow for 3 s.
 */
public class WitheredStaffItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    /*——————————————————————————————————————
     * CONFIG
     *——————————————————————————————————————*/
    private static final int COOLDOWN_TICKS = 200;          // 10 s
    private static final int MIN_CHARGE_TIME = 20;          // 1 s minimum charge
    private static final Vector3f WITHER_PARTICLE_COLOR       = new Vector3f(0.1f, 0.1f, 0.1f);
    private static final Vector3f WITHER_GLOW_PARTICLE_COLOR  = new Vector3f(0.4f, 0f,   0.4f);

    private static final String NBT_GLOWING   = "Glowing";

    public WitheredStaffItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(666));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    /*────────────────────────────────────────
     * CHARGE → RELEASE
     *────────────────────────────────────────*/
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Check cooldown
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        
        player.setCurrentHand(hand);   // starts "charging"
        
        // Start glowing when charging begins
        if (!stack.hasNbt()) {
            stack.setNbt(new NbtCompound());
        }
        stack.getNbt().putBoolean(NBT_GLOWING, true);
        
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) {
            // Calculate charge progress
            int ticksUsed = getMaxUseTime(stack) - remainingUseTicks;
            float chargeProgress = Math.min((float)ticksUsed / MIN_CHARGE_TIME, 1.0f);
            
            // Particle effects during charging
            if (remainingUseTicks % 3 == 0) {
                Vec3d look = user.getRotationVector();
                double ox = look.x * 0.5;
                double oy = look.y * 0.5 + 1.0;
                double oz = look.z * 0.5;
                
                // More particles as charge increases
                int particleCount = (int)(5 + chargeProgress * 10);
                
                for (int i = 0; i < particleCount; i++) {
                    double angle = (world.getTime() + i * 4) * 0.31415926;
                    double radius = 0.4 * (1.0 + chargeProgress * 0.5);
                    
                    // Alternate between dark and purple particles
                    Vector3f color = (i & 1) == 0 ? WITHER_PARTICLE_COLOR : WITHER_GLOW_PARTICLE_COLOR;
                    
                    // Add some randomness to particle positions
                    double randomOffset = world.random.nextGaussian() * 0.05;
                    
                    world.addParticle(
                            new DustParticleEffect(color, 0.8f + chargeProgress * 0.4f),
                            user.getX() + ox + Math.cos(angle) * radius + randomOffset,
                            user.getY() + oy + Math.sin(angle) * 0.2,
                            user.getZ() + oz + Math.sin(angle) * radius + randomOffset,
                            0, 0.05, 0
                    );
                }
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        // Stop glowing when released
        if (stack.hasNbt()) {
            stack.getNbt().putBoolean(NBT_GLOWING, false);
        }
        
        if (world.isClient()) {
            // Create a burst of particles when firing
            Vec3d particlePos = user.getPos().add(
                user.getRotationVector().multiply(0.4).add(0, 1.2, 0)
            );
            addWitherParticles(world, particlePos, 15, 0.3f);
            return; // Only process server-side logic for the actual firing
        }

        int useTime = this.getMaxUseTime(stack) - remainingUseTicks;
        
        // Require minimum charge time
        if (useTime < MIN_CHARGE_TIME) {
            return;
        }

        // Calculate charge factor based on time held (capped at 2 seconds for full power)
        float chargeFactor = Math.min(1.0f, (useTime - MIN_CHARGE_TIME) / 20.0f);
        
        if (user instanceof PlayerEntity player) {
            // Apply cooldown
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            
            // Spawn wither skull projectile
            WitherSkullEntity witherSkull = new WitherSkullEntity(
                world, user, user.getRotationVector().x, user.getRotationVector().y, user.getRotationVector().z);
                
            witherSkull.setPos(
                user.getX(), 
                user.getEyeY() - 0.1, 
                user.getZ());
                
            world.spawnEntity(witherSkull);
            
            // Apply area effect damage based on charge
            float damage = 2.0f + (8.0f * chargeFactor);
            float radius = 2.0f + (3.0f * chargeFactor);
            
            for (LivingEntity target : world.getNonSpectatingEntities(LivingEntity.class, 
                    new Box(user.getX() - radius, user.getY() - radius, user.getZ() - radius,
                           user.getX() + radius, user.getY() + radius, user.getZ() + radius))) {
                           
                if (target != user && target.distanceTo(user) <= radius) {
                    target.damage(user.getDamageSources().indirectMagic(user, user), damage);
                }
            }
            
            // Play sound effect
            world.playSound(null, user.getX(), user.getY(), user.getZ(), 
                           SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 
                           0.5f, 0.8f + (world.random.nextFloat() * 0.4f));
        }
    }

    /*────────────────────────────────────────
     * TICK / TOOLTIP
     *────────────────────────────────────────*/
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        // Only update glow effect on client side
        if (world.isClient() && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            
            // Check if the entity is actively using this item - add particles during charging
            if (livingEntity.isUsingItem() && livingEntity.getActiveItem() == stack) {
                // Add particles from the staff center cube while charging
                if (world.getRandom().nextFloat() < 0.3f) {
                    // Calculate position offset for the staff center cube - roughly where the player's hand is
                    Vec3d particlePos = livingEntity.getPos().add(
                        livingEntity.getRotationVector().multiply(0.4).add(0, 1.2, 0)
                    );
                    
                    // Add smaller wither particles during charging
                    addWitherParticles(world, particlePos, 1, 0.1f);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.translatable("item.ancientcurse.withered_staff.tooltip").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("Hold to charge, release to fire").formatted(Formatting.GRAY));
        
        if (world != null && world.isClient) {
            PlayerEntity player = world.getClosestPlayer(0, 0, 0, -1, false);
            if (player != null && player.getItemCooldownManager().isCoolingDown(this)) {
                float progress = player.getItemCooldownManager().getCooldownProgress(this, 0);
                int ticks = (int)(progress * COOLDOWN_TICKS);
                tooltip.add(Text.literal("Recharging: " + (ticks / 20) + "s").formatted(Formatting.RED));
            } else {
                tooltip.add(Text.literal("Ready").formatted(Formatting.GREEN));
            }
        }
    }

    /*────────────────────────────────────────
     * MISC ITEM BEHAVIOUR
     *────────────────────────────────────────*/
    @Override 
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.damage(attacker.getWorld().getDamageSources().wither(), 4f);
        stack.damage(1, attacker, e -> e.sendToolBreakStatus(Hand.MAIN_HAND));
        return true;
    }
    
    @Override 
    public int getMaxUseTime(ItemStack stack) { 
        return 72000; // Infinite hold time
    }
    
    @Override 
    public UseAction getUseAction(ItemStack stack) { 
        return UseAction.SPEAR; 
    }

    /*——————————————————————————————————————
     * PARTICLE EFFECTS
     *——————————————————————————————————————*/
    /**
     * Creates wither particles at the specified position
     */
    private void addWitherParticles(World world, Vec3d pos, int count, float spread) {
        for (int i = 0; i < count; i++) {
            double offsetX = (world.random.nextFloat() - 0.5) * spread;
            double offsetY = (world.random.nextFloat() - 0.5) * spread;
            double offsetZ = (world.random.nextFloat() - 0.5) * spread;
            
            // Add some upward velocity to particles to make them rise from the staff
            double velY = 0.01 + world.random.nextFloat() * 0.03;
            
            world.addParticle(
                new DustParticleEffect(
                    new Vector3f(0.4f, 0.0f, 0.4f), // Dark purple color (wither-like)
                    0.7f + world.random.nextFloat() * 0.3f), // Varied scale
                pos.x + offsetX, 
                pos.y + offsetY, 
                pos.z + offsetZ, 
                0.0, velY, 0.0);
        }
    }

    /*────────────────────────────────────────
     * GECKOLIB
     *────────────────────────────────────────*/
    @Override 
    public Supplier<Object> getRenderProvider() { 
        return renderProvider; 
    }
    
    @Override 
    public AnimatableInstanceCache getAnimatableInstanceCache() { 
        return cache; 
    }

    @Override 
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private final WitheredStaffRenderer renderer = new WitheredStaffRenderer();
            
            @Override 
            public BuiltinModelItemRenderer getCustomRenderer() { 
                return renderer; 
            }
        });
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<WitheredStaffItem> animationState) {
        return PlayState.CONTINUE;
    }

    /*────────────────────────────────────────
     * RENDERER
     *────────────────────────────────────────*/
    @Environment(EnvType.CLIENT)
    public static class WitheredStaffRenderer extends GeoItemRenderer<WitheredStaffItem> {
        
        public WitheredStaffRenderer() { 
            super(new StaffModel());
        }

        @Override
        public void render(ItemStack stack, ModelTransformationMode transformType, MatrixStack poseStack, 
                         VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
            // Instead of complex glow layers, just adjust the light level when glowing
            if (stack.hasNbt() && stack.getNbt().getBoolean(NBT_GLOWING)) {
                // Use full brightness when the staff is glowing
                packedLight = 15728880;  // Full brightness (emissive)
            }
            
            // Render with the calculated light
            super.render(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    /*────────────────────────────────────────
     * MODEL
     *────────────────────────────────────────*/
    public static class StaffModel extends GeoModel<WitheredStaffItem> {
        private static final Identifier MODEL   = new Identifier(AncientCurse.MOD_ID, "geo/withered_staff.geo.json");
        private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/item/withered_staff.png");
        private static final Identifier ANIM    = new Identifier(AncientCurse.MOD_ID, "animations/withered_staff.animation.json");

        @Override 
        public Identifier getModelResource(WitheredStaffItem animatable) { 
            return MODEL; 
        }
        
        @Override 
        public Identifier getTextureResource(WitheredStaffItem animatable) { 
            return TEXTURE; 
        }
        
        @Override 
        public Identifier getAnimationResource(WitheredStaffItem animatable) { 
            return ANIM; 
        }
    }
}