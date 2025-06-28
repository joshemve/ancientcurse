package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.KhamsinOrbEntity;
import com.ancientcurse.entity.model.KhamsinOrbModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KhamsinOrbRenderer extends GeoEntityRenderer<KhamsinOrbEntity> {
    
    // Create additive render layer for glowing effect
    private static final RenderLayer ADDITIVE_LAYER = RenderLayer.getEntityTranslucentEmissive(
        new Identifier("ancientcurse", "textures/entity/khamsin_orb.png")
    );
    
    public KhamsinOrbRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new KhamsinOrbModel());
    }
    
    @Override
    public RenderLayer getRenderType(KhamsinOrbEntity animatable, Identifier texture, 
                                   net.minecraft.client.render.VertexConsumerProvider bufferSource, float partialTick) {
        // Use additive blending for glowing effect
        return ADDITIVE_LAYER;
    }
}