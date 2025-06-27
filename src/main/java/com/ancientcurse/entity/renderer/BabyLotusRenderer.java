package com.ancientcurse.entity.renderer;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.model.BabyLotusModel;
import com.ancientcurse.entity.BabyLotusEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BabyLotusRenderer extends GeoEntityRenderer<BabyLotusEntity> {
    public BabyLotusRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new BabyLotusModel());
    }

    @Override
    public Identifier getTextureLocation(BabyLotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }
} 