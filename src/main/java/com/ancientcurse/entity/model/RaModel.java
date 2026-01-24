package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.RaEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class RaModel extends GeoModel<RaEntity> {
    private static final Identifier MODEL = new Identifier(AncientCurse.MOD_ID, "geo/ra.geo.json");
    private static final Identifier TEXTURE = new Identifier(AncientCurse.MOD_ID, "textures/entity/ra.png");
    private static final Identifier ANIMATION = new Identifier(AncientCurse.MOD_ID, "animations/ra.animation.json");

    @Override
    public Identifier getModelResource(RaEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(RaEntity animatable) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(RaEntity animatable) {
        return ANIMATION;
    }
}
