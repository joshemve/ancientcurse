package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.model.LocusModel;
import com.ancientcurse.entity.LocusEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LocusRenderer extends GeoEntityRenderer<LocusEntity> {
    public LocusRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new LocusModel());
    }

    @Override
    public Identifier getTextureLocation(LocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }
} 