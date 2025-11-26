package com.ancientcurse.item.armor;

import com.ancientcurse.client.renderer.armor.ScarabMaskRenderer;
import com.ancientcurse.item.renderer.ScarabMaskItemRenderer;
import com.ancientcurse.material.ScarabMaskArmorMaterial;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScarabMaskItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public ScarabMaskItem() {
        super(new ScarabMaskArmorMaterial(), Type.HELMET, new FabricItemSettings());
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new ScarabMaskRenderProvider());
    }

    private static class ScarabMaskRenderProvider implements RenderProvider {
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

    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<ScarabMaskItem> animationState) {
        // No animation for now, but structure is here for future use
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("Ancient mask of the sacred scarab").formatted(Formatting.GOLD, Formatting.ITALIC));
        tooltip.add(Text.literal("Symbol of rebirth and protection").formatted(Formatting.GRAY, Formatting.ITALIC));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
