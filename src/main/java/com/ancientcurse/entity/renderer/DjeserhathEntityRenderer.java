package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.DjeserhathEntity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.model.GeoModel;

/**
 * Renderer for the Djeserhath entity
 */
public class DjeserhathEntityRenderer extends GeoEntityRenderer<DjeserhathEntity> {
    public DjeserhathEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DjeserhathModel());
        this.shadowRadius = 0f; // No shadow for plant entity
    }
    
    /**
     * Inner model class to handle the Djeserhath entity model
     */
    private static class DjeserhathModel extends GeoModel<DjeserhathEntity> {
        @Override
        public Identifier getModelResource(DjeserhathEntity object) {
            return new Identifier(AncientCurse.MOD_ID, "geo/djeserhath.geo.json");
        }

        @Override
        public Identifier getTextureResource(DjeserhathEntity object) {
            return new Identifier(AncientCurse.MOD_ID, "textures/entity/djeserhath.png");
        }

        @Override
        public Identifier getAnimationResource(DjeserhathEntity animatable) {
            return new Identifier(AncientCurse.MOD_ID, "animations/djeserhath.animation.json");
        }
    }

    @Override
    public Identifier getTextureLocation(DjeserhathEntity instance) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/djeserhath.png");
    }
}
