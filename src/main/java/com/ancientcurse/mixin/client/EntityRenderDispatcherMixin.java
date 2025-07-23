package com.ancientcurse.mixin.client;

import com.ancientcurse.AncientCurse;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    
    @Shadow
    private Map<EntityType<?>, EntityRenderer<?>> renderers;
    
    @Inject(method = "getRenderer", at = @At("HEAD"))
    private <T extends Entity> void debugMissingRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (entity != null && entity.getType().getRegistryEntry().getKey().get().getValue().getNamespace().equals(AncientCurse.MOD_ID)) {
            if (!this.renderers.containsKey(entity.getType())) {
                AncientCurse.LOGGER.error("Missing renderer for Ancient Curse entity: " + entity.getType().getRegistryEntry().getKey().get().getValue());
                AncientCurse.LOGGER.error("Entity class: " + entity.getClass().getName());
                AncientCurse.LOGGER.error("Entity type: " + entity.getType());
            }
        }
    }
}