package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.model.BabyLocusModel;
import com.ancientcurse.entity.BabyLocusEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BabyLocusRenderer extends GeoEntityRenderer<BabyLocusEntity> {
    public BabyLocusRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BabyLocusModel());
    }

    @Override
    public Identifier getTextureLocation(BabyLocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }
} 