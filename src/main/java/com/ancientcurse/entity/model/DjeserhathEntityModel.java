package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.DjeserhathEntity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Model for the Djeserhath entity
 */
public class DjeserhathEntityModel extends GeoModel<DjeserhathEntity> {
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
