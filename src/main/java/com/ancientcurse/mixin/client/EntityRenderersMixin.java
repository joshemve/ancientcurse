package com.ancientcurse.mixin.client;

import com.ancientcurse.AncientCurse;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderersMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?>> renderers;

    @Shadow
    private Map<String, EntityRenderer<? extends net.minecraft.entity.player.PlayerEntity>> modelRenderers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void logRendererRegistration(CallbackInfo ci) {
        AncientCurse.LOGGER.info("EntityRenderDispatcher initialized with " + renderers.size() + " entity renderers");
        
        // Log all Ancient Curse entities that have renderers
        renderers.forEach((entityType, renderer) -> {
            if (entityType.getRegistryEntry().getKey().get().getValue().getNamespace().equals(AncientCurse.MOD_ID)) {
                AncientCurse.LOGGER.info("Registered renderer for: " + entityType.getRegistryEntry().getKey().get().getValue());
            }
        });
    }
}