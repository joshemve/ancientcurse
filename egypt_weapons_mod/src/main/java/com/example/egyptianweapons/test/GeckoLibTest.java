package com.example.egyptianweapons.test;

import com.example.egyptianweapons.EgyptianWeapons;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.DataTicket;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A test class to demonstrate proper GeckoLib 4.2.1 implementation
 */
public class GeckoLibTest {
    // Example GeoItem implementation
    public static class TestGeoItem extends Item implements GeoItem {
        private static final DataTicket<ItemStack> ITEMSTACK = new DataTicket<>("itemstack", ItemStack.class);
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private final String modelName;

        // Store the model name to create a renderer specific to this item instance
        private final Supplier<Object> rendererSupplier;

        public TestGeoItem(Settings settings, String modelName) {
            super(settings);
            this.modelName = modelName;
            // Create a dedicated renderer supplier for this specific model name
            this.rendererSupplier = () -> new GeoItemRenderer<>(new TestGeoModel(modelName));
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public void createRenderer(Consumer<Object> consumer) {
            consumer.accept(rendererSupplier.get());
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
            controllerRegistrar.add(new AnimationController<>(this, "controller", 0, state -> {
                ItemStack stack = state.getData(ITEMSTACK);
                if (stack != null && stack.hasNbt() && stack.getNbt().getBoolean("NbtTag")) {
                    state.getController().setAnimation(RawAnimation.begin().thenPlay("animation." + modelName + ".action"));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("animation." + modelName + ".idle"));
                }
                return PlayState.CONTINUE;
            }));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return this.cache;
        }

        @Override
        public Supplier<Object> getRenderProvider() {
            return this.rendererSupplier;
        }
    }

    // Example GeoModel implementation
    public static class TestGeoModel extends GeoModel<TestGeoItem> {
        private final String modelName;

        public TestGeoModel(String modelName) {
            this.modelName = modelName;
        }

        @Override
        public Identifier getModelResource(TestGeoItem animatable) {
            return new Identifier(EgyptianWeapons.MOD_ID, "geo/" + modelName + ".geo.json");
        }

        @Override
        public Identifier getTextureResource(TestGeoItem animatable) {
            return new Identifier(EgyptianWeapons.MOD_ID, "textures/item/" + modelName + ".png");
        }

        @Override
        public Identifier getAnimationResource(TestGeoItem animatable) {
            return new Identifier(EgyptianWeapons.MOD_ID, "animations/" + modelName + ".animation.json");
        }
    }
}
