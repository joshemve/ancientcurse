package com.example.egyptianweapons.items;

import com.example.egyptianweapons.EgyptianWeapons;
import com.example.egyptianweapons.client.render.item.StaffOfRaRenderer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
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

public class StaffOfRa extends Item implements GeoItem {
    private static final DataTicket<ItemStack> ITEMSTACK = new DataTicket<>("itemstack", ItemStack.class);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    private static final int COOLDOWN = 20;
    private static final double RANGE = 10.0;
    private static final int EFFECT_DURATION = 200;
    private static final int EFFECT_AMPLIFIER = 1;

    public StaffOfRa() {
        super(new FabricItemSettings().maxCount(1).maxDamage(100));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private StaffOfRaRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new StaffOfRaRenderer();
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
            state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.staffofra.idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            ItemStack itemStack = player.getStackInHand(hand);
            
            if (!player.getItemCooldownManager().isCoolingDown(this)) {
                applyEffectsToNearbyEntities(world, player);
                player.getItemCooldownManager().set(this, COOLDOWN);
                
                // Damage the item
                itemStack.damage(1, player, (p) -> p.sendToolBreakStatus(hand));
            }
        }
        
        return TypedActionResult.success(player.getStackInHand(hand));
    }

    private void applyEffectsToNearbyEntities(World world, PlayerEntity player) {
        Box box = new Box(
            player.getX() - RANGE, player.getY() - RANGE, player.getZ() - RANGE,
            player.getX() + RANGE, player.getY() + RANGE, player.getZ() + RANGE
        );

        List<Entity> nearbyEntities = world.getOtherEntities(player, box);
        
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Apply positive effects to friendly entities (like players)
                if (entity instanceof PlayerEntity) {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, EFFECT_DURATION, EFFECT_AMPLIFIER));
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_DURATION, EFFECT_AMPLIFIER));
                }
                // Apply negative effects to hostile entities
                else {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, EFFECT_DURATION, EFFECT_AMPLIFIER));
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_DURATION, EFFECT_AMPLIFIER));
                    livingEntity.setOnFireFor(5);
                }
            }
        }
    }
}
