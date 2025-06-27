package com.ancientcurse.entity.model;

import com.ancientcurse.AncientCurse;
import com.ancientcurse.entity.BabyLotusEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class BabyLotusModel extends GeoModel<BabyLotusEntity> {
    @Override
    public Identifier getModelResource(BabyLotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "geo/locus.geo.json");
    }

    @Override
    public Identifier getTextureResource(BabyLotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "textures/entity/locus.png");
    }

    @Override
    public Identifier getAnimationResource(BabyLotusEntity animatable) {
        return new Identifier(AncientCurse.MOD_ID, "animations/locus.animation.json");
    }
} 