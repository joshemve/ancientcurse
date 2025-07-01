package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.LocusEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class LocusModel extends GeoModel<LocusEntity> {
    @Override
    public Identifier getModelResource(LocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/locus.geo.json");
    }

    @Override
    public Identifier getTextureResource(LocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }

    @Override
    public Identifier getAnimationResource(LocusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/locus.animation.json");
    }
} 