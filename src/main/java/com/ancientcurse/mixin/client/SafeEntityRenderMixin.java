package com.ancientcurse.mixin.client;

import com.ancientcurse.AncientCurse;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class SafeEntityRenderMixin {

    @Shadow
    public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void safeRender(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity == null) {
            AncientCurse.LOGGER.error("Attempted to render null entity!");
            ci.cancel();
            return;
        }
        
        EntityRenderer<? super E> renderer = this.getRenderer(entity);
        if (renderer == null) {
            AncientCurse.LOGGER.error("Missing renderer for entity: " + entity.getType().getRegistryEntry().getKey().get().getValue());
            AncientCurse.LOGGER.error("Entity class: " + entity.getClass().getName());
            AncientCurse.LOGGER.error("Entity position: " + entity.getPos());
            ci.cancel();
            return;
        }
    }
}