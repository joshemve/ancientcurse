package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.KhamsinOrbEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class KhamsinOrbModel extends GeoModel<KhamsinOrbEntity> {
    @Override
    public Identifier getModelResource(KhamsinOrbEntity object) {
        return new Identifier(AncientCurse.MOD_ID, "geo/khamsin_orb.geo.json");
    }

    @Override
    public Identifier getTextureResource(KhamsinOrbEntity object) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/khamsin_orb.png");
    }

    @Override
    public Identifier getAnimationResource(KhamsinOrbEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/khamsin_orb.animation.json");
    }
}