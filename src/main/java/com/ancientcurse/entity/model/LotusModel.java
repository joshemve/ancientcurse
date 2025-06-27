package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.LotusEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class LotusModel extends GeoModel<LotusEntity> {
    @Override
    public Identifier getModelResource(LotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/locus.geo.json");
    }

    @Override
    public Identifier getTextureResource(LotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }

    @Override
    public Identifier getAnimationResource(LotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/locus.animation.json");
    }
} 