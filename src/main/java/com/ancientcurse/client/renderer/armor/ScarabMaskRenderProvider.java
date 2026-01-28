package com.ancientcurse.client.renderer.armor;

import com.ancientcurse.item.renderer.ScarabMaskItemRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.animatable.client.RenderProvider;

/**
 * Client-side render provider for Scarab Mask item.
 * Separated from the item class to avoid class loading issues.
 */
public class ScarabMaskRenderProvider implements RenderProvider {
    private ScarabMaskRenderer armorRenderer;
    private ScarabMaskItemRenderer itemRenderer;

    @Override
    public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
        if (this.armorRenderer == null)
            this.armorRenderer = new ScarabMaskRenderer();

        this.armorRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
        return this.armorRenderer;
    }

    @Override
    public net.minecraft.client.render.item.BuiltinModelItemRenderer getCustomRenderer() {
        if (this.itemRenderer == null)
            this.itemRenderer = new ScarabMaskItemRenderer();
        return this.itemRenderer;
    }
}
