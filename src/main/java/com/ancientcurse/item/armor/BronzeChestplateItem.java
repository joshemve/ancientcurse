package com.ancientcurse.item.armor;

import com.ancientcurse.client.renderer.armor.BronzeChestplateRenderer;
import com.ancientcurse.material.BronzeArmorMaterial;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BronzeChestplateItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    public BronzeChestplateItem() {
        super(new BronzeArmorMaterial(), Type.CHESTPLATE, new FabricItemSettings());
    }
    
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new BronzeChestplateRenderProvider());
    }
    
    // Static nested class to avoid inner class compilation issues
    private static class BronzeChestplateRenderProvider implements RenderProvider {
        private BronzeChestplateRenderer renderer;

        @Override
        public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
            if (this.renderer == null)
                this.renderer = new BronzeChestplateRenderer();

            this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
            return this.renderer;
        }
    }
    
    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations for armor by default
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}