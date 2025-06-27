package com.example.egyptianweapons.items;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.render.item.SerpentStaffRenderer;
import com.example.egyptianweapons.effect.SnakeHeadProjectileEntity;
import com.example.egyptianweapons.effect.TornadoManager;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SerpentStaff extends Item implements GeoItem {
    private static final DataTicket<ItemStack> ITEMSTACK = new DataTicket<>("itemstack", ItemStack.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public SerpentStaff() {
        super(new FabricItemSettings().maxCount(1).maxDamage(1024));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SerpentStaffRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new SerpentStaffRenderer();
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
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.serpentstaff.idle"));
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
                performTornadoAttack(world, player);
            } else {
                fireSnakeProjectile(world, player);
            }
        }
        
        return TypedActionResult.success(stack);
    }
    
    private void performTornadoAttack(World world, PlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }
        
        double reach = 32.0;
        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d end = start.add(look.multiply(reach));
        
        Box box = player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0);
        Entity target = null;
        double closestDistance = reach;
        
        for (Entity entity : world.getOtherEntities(player, box, entity -> 
            entity instanceof LivingEntity && !(entity instanceof PlayerEntity))) {
            Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> hitPos = entityBox.raycast(start, end);
            
            if (hitPos.isPresent()) {
                double distance = start.distanceTo(hitPos.get());
                if (distance < closestDistance) {
                    target = entity;
                    closestDistance = distance;
                }
            }
        }
        
        if (target instanceof LivingEntity livingTarget) {
            TornadoManager.spawnTornado(world, target.getBlockPos(), livingTarget);
            
            world.playSound(null, target.getX(), target.getY(), target.getZ(), 
                SoundEvents.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 
                1.0F, 0.5F);
                
            ((LivingEntity)player).addStatusEffect(
                new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1)
            );
                
            player.getItemCooldownManager().set(this, 500);
        }
    }
    
    private void fireSnakeProjectile(World world, PlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }
        
        SnakeHeadProjectileEntity projectile = new SnakeHeadProjectileEntity(world, player);
        projectile.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 1.5F, 1.0F);
        world.spawnEntity(projectile);
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 
            1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        
        player.getItemCooldownManager().set(this, 60);
    }
}
