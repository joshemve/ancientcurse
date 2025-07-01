package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A custom animated item using GeckoLib
 */
public class CustomAnimatedItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    private final String name;

    public CustomAnimatedItem(Settings settings, String name) {
        super(settings);
        this.name = name;
        // Register the item for rendering with GeckoLib
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private final CustomItemRenderer renderer = new CustomItemRenderer();

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
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
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        // Always play the sun animation
        tAnimationState.getController().setAnimation(RawAnimation.begin()
                .then("animation.model.sun", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /**
     * Custom renderer for GeckoLib items that respects Blockbench display settings
     */
    private class CustomItemRenderer extends GeoItemRenderer<CustomAnimatedItem> {
        public CustomItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public void render(ItemStack stack, ModelTransformationMode transformationMode, MatrixStack poseStack, 
                           VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
            
            // Store original matrix state
            poseStack.push();
            
            // For left hand, need to mirror the model
            if (transformationMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || 
                transformationMode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
                // Only handle mirroring for left-hand views
                // Blockbench can't export correct left-hand mirroring
                if (transformationMode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
                    // Mirroring is already handled in the Blockbench display settings
                    // No additional code needed
                }
            }
            
            // Call the parent render method - let it use the JSON display settings
            super.render(stack, transformationMode, poseStack, bufferSource, packedLight, packedOverlay);
            
            // Restore matrix state
            poseStack.pop();
        }
    }
    
    /**
     * Custom model class for this item
     */
    private class ItemModel extends GeoModel<CustomAnimatedItem> {
        @Override
        public Identifier getModelResource(CustomAnimatedItem animatable) {
            return new Identifier(AncientCurse.MOD_ID, "geo/" + name + ".geo.json");
        }

        @Override
        public Identifier getTextureResource(CustomAnimatedItem animatable) {
            return new Identifier(AncientCurse.MOD_ID, "textures/item/" + name + ".png");
        }

        @Override
        public Identifier getAnimationResource(CustomAnimatedItem animatable) {
            return new Identifier(AncientCurse.MOD_ID, "animations/" + name + ".animation.json");
        }
    }
} 