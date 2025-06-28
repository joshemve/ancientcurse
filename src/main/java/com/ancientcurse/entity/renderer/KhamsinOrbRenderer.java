package com.ancientcurse.entity.renderer;

import com.ancientcurse.entity.KhamsinOrbEntity;
import com.ancientcurse.entity.model.KhamsinOrbModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KhamsinOrbRenderer extends GeoEntityRenderer<KhamsinOrbEntity> {
    public KhamsinOrbRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new KhamsinOrbModel());
    }
}