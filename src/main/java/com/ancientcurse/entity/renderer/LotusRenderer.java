package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.model.LotusModel;
import com.ancientcurse.entity.LotusEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LotusRenderer extends GeoEntityRenderer<LotusEntity> {
    public LotusRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new LotusModel());
    }

    @Override
    public Identifier getTextureLocation(LotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }
} 